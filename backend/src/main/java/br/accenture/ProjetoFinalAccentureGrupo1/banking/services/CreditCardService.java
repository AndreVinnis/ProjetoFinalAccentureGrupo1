package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CardPurchase;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardPurchaseResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditLimitResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentReceivedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.*;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CardPurchaseRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.utils.CreditCardNumberGenerator;
import br.accenture.ProjetoFinalAccentureGrupo1.shared.security.AESEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private static final BigDecimal INITIAL_CREDIT_LIMIT = new BigDecimal("5000.00");

    private final UserFacade userFacade;
    private final CreditCardRepository creditCardRepository;
    private final CardPurchaseRepository cardPurchaseRepository;
    private final CreditCardNumberGenerator cardNumberGenerator;
    private final InvoiceService invoiceService;
    private final AccountService accountService;
    private final AESEncryptionService encryptionService;

    @Transactional
    public void createCardForAccount(Account account){
        if(creditCardRepository.existsByAccount(account)){
            throw new CreditCardAlreadyExistsException(account);
        }
        createCard(userFacade.findById(account.getUserId()), account);
    }

    @Transactional(readOnly = true)
    public CreditCardResponse findMyCard(String email, String password) {
        Account account = accountService.findAccountByUserEmail(email);
        CreditCard card = findCard(email);
        if(!encryptionService.decrypt(account.getPassword()).equals(password)){
            System.out.println("Sistema: " + encryptionService.decrypt(account.getPassword()));
            System.out.println("Passado: " + password);
            throw new WrongPasswordException();
        }
        return toCardResponse(card);
    }

    @Transactional(readOnly = true)
    public CreditCard findMyCardByAccount(Account account) {
        return creditCardRepository.findByAccount(account).orElseThrow(
                () -> new CreditCardNotFoundException(0L)
        );
    }

    @Transactional(readOnly = true)
    public CreditLimitResponse findMyLimit(String email) {
        CreditCard card = findCard(email);
        return toLimitResponse(card);
    }

    public CardValidationResponse validateCard(String cardNumber, String cvv, int expirationMonth, int expirationYear){
        CreditCard card = creditCardRepository.findByNumberHash(encryptionService.encrypt(cardNumber)).orElseThrow(
                () -> new InvalidCardException(cardNumber)
        );
        if(!encryptionService.decrypt(card.getCvvHash()).equals(cvv)){
            throw new InvalidCardException(cvv);
        }
        if(card.getExpirationMonth() != expirationMonth){
            throw new InvalidCardException("Mês de vencimento: " + expirationMonth);
        }
        if(card.getExpirationYear() != expirationYear){
            throw new InvalidCardException("Ano de vencimento: " + expirationYear);
        }
        return new CardValidationResponse(card.getId(), cardNumber.substring(cardNumber.length() - 4));
    }

    @Transactional
    public void chargeCard(Long cardId, BigDecimal amount, String cvv, String description, String refence) {
        chargeCard(cardId, amount, cvv, description, refence, 1);
    }

    @Transactional
    public void chargeCard(Long cardId, BigDecimal amount, String cvv, String description, String refence, int installments) {
        CreditCard card = creditCardRepository.findById(cardId).orElseThrow(()
            -> new CardNotFoundException(cardId));
        validateInstallments(installments);
        validateCardCanPurchase(card,amount, cvv);

        Invoice currentInvoice = invoiceService.getOrCreateOpenInvoice(card);
        YearMonth firstReferenceMonth = currentInvoice.getReferenceMonth();
        String installmentGroupId = UUID.randomUUID().toString();
        List<BigDecimal> installmentAmounts = splitInstallments(amount, installments);

        for (int index = 0; index < installments; index++) {
            Invoice invoice = index == 0
                    ? currentInvoice
                    : invoiceService.getOrCreateOpenInvoice(card, firstReferenceMonth.plusMonths(index));
            int installmentNumber = index + 1;
            String installmentDescription = installments == 1
                    ? description
                    : description + " (" + installmentNumber + "/" + installments + ")";

            CardPurchase cardPurchase = CardPurchase.builder()
                    .card(card)
                    .invoice(invoice)
                    .amount(installmentAmounts.get(index))
                    .description(installmentDescription)
                    .reference(refence)
                    .installmentNumber(installmentNumber)
                    .installmentTotal(installments)
                    .installmentGroupId(installmentGroupId)
                    .build();

            invoiceService.addCardPurchase(invoice, cardPurchase);
            cardPurchaseRepository.save(cardPurchase);
        }

        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        creditCardRepository.save(card);
        accountService.creditMerchant(amount, description, refence);
    }

    @Transactional(readOnly = true)
    public List<CardPurchaseResponse> findMyPurchases(String email) {
        CreditCard card = findCard(email);
        return cardPurchaseRepository.findByCardIdOrderByPurchaseDateDesc(card.getId())
                .stream()
                .map(this::toPurchaseResponse)
                .toList();
    }

    @Transactional
    public BigDecimal cancelPurchase(String reference, String refundDescription) {
        List<CardPurchase> purchases = cardPurchaseRepository.findByReferenceOrderByPurchaseDateAsc(reference);
        if (purchases.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CreditCard card = purchases.get(0).getCard();
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal limitToRestore = BigDecimal.ZERO;

        for (CardPurchase purchase : purchases) {
            Invoice invoice = purchase.getInvoice();
            BigDecimal paidAmount = calculatePaidPurchaseAmount(invoice, purchase.getAmount());
            BigDecimal unpaidAmount = purchase.getAmount().subtract(paidAmount);

            refundAmount = refundAmount.add(paidAmount);
            limitToRestore = limitToRestore.add(unpaidAmount);

            invoice.setTotalAmount(nonNegative(invoice.getTotalAmount().subtract(purchase.getAmount())));
            invoice.setPaidAmount(nonNegative(invoice.getPaidAmount().subtract(paidAmount)));
            reconcileInvoiceAfterPurchaseCancellation(invoice);
            cardPurchaseRepository.delete(purchase);
        }

        if (limitToRestore.signum() > 0) {
            card.setAvailableLimit(capAtCreditLimit(card, card.getAvailableLimit().add(limitToRestore)));
            creditCardRepository.save(card);
        }

        if (refundAmount.signum() > 0) {
            accountService.refund(card.getAccount().getUserId(), refundAmount, reference, refundDescription);
        }

        return refundAmount;
    }

    @Transactional
    public void blockCardByAccount(Account account) {
        CreditCard card = creditCardRepository.findByAccount(account).orElseThrow(
                () -> new CardNotFoundException(0L)
        );
        card.setStatus(CreditCardStatus.BLOCKED);
        toCardResponse(creditCardRepository.save(card));
    }

    @Transactional
    public void unblockCardByAccount(Account account) {
        CreditCard card = creditCardRepository.findByAccount(account).orElseThrow(
                () -> new CardNotFoundException(0L)
        );
        card.setStatus(CreditCardStatus.ACTIVE);
        toCardResponse(creditCardRepository.save(card));
    }

    private void createCard(UserInfo userInfo, Account account) {
        LocalDate expirationDate = LocalDate.now().plusYears(5);
        String cardNumber = cardNumberGenerator.generateCardNumber();
        String cvv = cardNumberGenerator.generateCvv();

        CreditCard card = CreditCard.builder()
                .account(account)
                .holderName(userInfo.name())
                .numberHash(encryptionService.encrypt(cardNumber))
                .cvvHash(encryptionService.encrypt(cvv))
                .expirationMonth(expirationDate.getMonthValue())
                .expirationYear(expirationDate.getYear())
                .status(CreditCardStatus.ACTIVE)
                .creditLimit(INITIAL_CREDIT_LIMIT)
                .availableLimit(INITIAL_CREDIT_LIMIT)
                .closingDay(25)
                .dueDay(10)
                .build();

        creditCardRepository.save(card);
    }


    private void validateCardCanPurchase(CreditCard card, BigDecimal amount, String cvv) {
        if (card.getStatus() != CreditCardStatus.ACTIVE) {
            throw new CreditCardBlockedException();
        }

        if (card.getAvailableLimit().compareTo(amount) < 0) {
            throw new InsufficientCreditLimitException();
        }

        if(!encryptionService.encrypt(cvv).equals(card.getCvvHash())){
            throw new WrongCvvException();
        }
    }

    private CreditCard findCard(String email) {
        UserInfo user = userFacade.findByEmail(email);

        Account account = accountService.findByUserId(user.id());

        return creditCardRepository.findByAccount(account).orElseThrow(() -> new CreditCardNotFoundException(user.id()));
    }

    private CreditCardResponse toCardResponse(CreditCard card) {
        return new CreditCardResponse(
                card.getId(),
                card.getHolderName(),
                encryptionService.decrypt(card.getNumberHash()),
                encryptionService.decrypt(card.getCvvHash()),
                card.getExpirationMonth(),
                card.getExpirationYear(),
                card.getStatus(),
                card.getCreditLimit(),
                card.getAvailableLimit()
        );
    }

    private CreditLimitResponse toLimitResponse(CreditCard card) {
        return new CreditLimitResponse(
                card.getCreditLimit(),
                card.getAvailableLimit(),
                card.getCreditLimit().subtract(card.getAvailableLimit())
        );
    }


    private CardPurchaseResponse toPurchaseResponse(CardPurchase purchase) {
        return new CardPurchaseResponse(
                purchase.getId(),
                purchase.getInvoice().getId(),
                purchase.getAmount(),
                purchase.getDescription(),
                purchase.getReference(),
                purchase.getInstallmentNumber() == null ? 1 : purchase.getInstallmentNumber(),
                purchase.getInstallmentTotal() == null ? 1 : purchase.getInstallmentTotal(),
                purchase.getPurchaseDate()
        );
    }

    private void validateInstallments(int installments) {
        if (installments < 1 || installments > 12) {
            throw new IllegalArgumentException("Parcelamento deve estar entre 1 e 12");
        }
    }

    private BigDecimal calculatePaidPurchaseAmount(Invoice invoice, BigDecimal purchaseAmount) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return purchaseAmount;
        }

        if (invoice.getPaidAmount() == null
                || invoice.getTotalAmount() == null
                || invoice.getPaidAmount().signum() <= 0
                || invoice.getTotalAmount().signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal paidRatio = invoice.getPaidAmount()
                .divide(invoice.getTotalAmount(), 8, RoundingMode.HALF_EVEN);
        BigDecimal paidAmount = purchaseAmount.multiply(paidRatio).setScale(2, RoundingMode.HALF_EVEN);

        return paidAmount.min(purchaseAmount);
    }

    private void reconcileInvoiceAfterPurchaseCancellation(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.OPEN) {
            return;
        }

        if (invoice.getTotalAmount().signum() == 0) {
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setStatus(InvoiceStatus.PAID);
            if (invoice.getPaidAt() == null) {
                invoice.setPaidAt(Instant.now());
            }
            return;
        }

        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            if (invoice.getPaidAt() == null) {
                invoice.setPaidAt(Instant.now());
            }
        }
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount.signum() < 0 ? BigDecimal.ZERO : amount;
    }

    private BigDecimal capAtCreditLimit(CreditCard card, BigDecimal amount) {
        return amount.compareTo(card.getCreditLimit()) > 0 ? card.getCreditLimit() : amount;
    }

    private List<BigDecimal> splitInstallments(BigDecimal amount, int installments) {
        BigDecimal baseAmount = amount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(baseAmount.multiply(BigDecimal.valueOf(installments)));

        return java.util.stream.IntStream.rangeClosed(1, installments)
                .mapToObj(index -> index == 1 ? baseAmount.add(remainder) : baseAmount)
                .toList();
    }
}
