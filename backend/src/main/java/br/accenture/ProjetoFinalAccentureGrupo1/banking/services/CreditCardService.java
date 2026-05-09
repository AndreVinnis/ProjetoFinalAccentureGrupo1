package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CardPurchase;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCardTransaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardPurchaseResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardPurchaseRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardTransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditLimitResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditPaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditPaymentResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardTransactionStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.CreditCardAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.CreditCardBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.CreditCardNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientCreditLimitException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CardPurchaseRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardTransactionRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.InvoiceRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private static final BigDecimal INITIAL_CREDIT_LIMIT = new BigDecimal("1000.00");

    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository transactionRepository;
    private final CardPurchaseRepository cardPurchaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreditCardNumberGenerator cardNumberGenerator;
    private final InvoiceService invoiceService;
    private final AccountService accountService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreditCardResponse createVirtualCardForUser(Long userId) {
        if (creditCardRepository.existsByUserId(userId)) {
            throw new CreditCardAlreadyExistsException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + userId));

        return createCard(user, null);
    }

    @Transactional
    public CreditCardResponse createCardForAccount(Account account) {
        if (creditCardRepository.existsByUserId(account.getUserId())) {
            throw new CreditCardAlreadyExistsException(account.getUserId());
        }

        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + account.getUserId()));

        return createCard(user, account);
    }

    @Transactional(readOnly = true)
    public CreditCardResponse findMyCard(String email) {
        CreditCard card = findCardByUserEmail(email);
        return toCardResponse(card);
    }

    @Transactional(readOnly = true)
    public CreditLimitResponse findMyLimit(String email) {
        CreditCard card = findCardByUserEmail(email);
        return toLimitResponse(card);
    }

    @Transactional(noRollbackFor = {CreditCardBlockedException.class, InsufficientCreditLimitException.class})
    public CreditCardTransactionResponse purchase(String email, CreditCardPurchaseRequest request) {
        CreditCard card = findCardByUserEmail(email);
        validateCardCanPurchase(card, request.amount(), request.merchantName(), request.description(), 1);

        CreditCardTransaction savedTx = approvePurchase(
                card,
                request.amount(),
                1,
                request.merchantName(),
                request.description(),
                request.reference()
        );

        return toTransactionResponse(savedTx);
    }

    @Transactional(noRollbackFor = {CreditCardBlockedException.class, InsufficientCreditLimitException.class})
    public CreditPaymentResponse payWithCredit(String email, CreditPaymentRequest request) {
        CreditCard card = findCardByUserEmail(email);
        validateCardCanPurchase(card, request.amount(), request.merchantName(), request.description(), request.installments());

        CreditCardTransaction savedTx = approvePurchase(
                card,
                request.amount(),
                request.installments(),
                request.merchantName(),
                request.description(),
                null
        );

        return toPaymentResponse(savedTx, card);
    }

    @Transactional
    public CreditCardTransactionResponse chargeCard(Long userId, BigDecimal amount, String description, String reference) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + userId));
        return purchase(user.getEmail(), new CreditCardPurchaseRequest(amount, "Loja Accenture", description, reference));
    }

    @Transactional(readOnly = true)
    public List<CardPurchaseResponse> findMyPurchases(String email) {
        CreditCard card = findCardByUserEmail(email);
        return cardPurchaseRepository.findByCardIdOrderByPurchaseDateDesc(card.getId())
                .stream()
                .map(this::toPurchaseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCard findMyCardEntity(String email) {
        return findCardByUserEmail(email);
    }

    @Transactional(readOnly = true)
    public List<CreditCardTransactionResponse> findRecentTransactions(String email) {
        CreditCard card = findCardByUserEmail(email);
        return transactionRepository.findTop10ByCreditCardIdOrderByCreatedAtDesc(card.getId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public CreditCardResponse blockMyCard(String email) {
        CreditCard card = findCardByUserEmail(email);
        card.setStatus(CreditCardStatus.BLOCKED);
        return toCardResponse(creditCardRepository.save(card));
    }

    @Transactional
    public CreditCardResponse unblockMyCard(String email) {
        CreditCard card = findCardByUserEmail(email);
        card.setStatus(CreditCardStatus.ACTIVE);
        return toCardResponse(creditCardRepository.save(card));
    }

    private CreditCardResponse createCard(User user, Account account) {
        LocalDate expirationDate = LocalDate.now().plusYears(5);
        String cardNumber = cardNumberGenerator.generateCardNumber();
        String cvv = cardNumberGenerator.generateCvv();

        CreditCard card = CreditCard.builder()
                .user(user)
                .account(account)
                .holderName(user.getName())
                .numberHash(hash(cardNumber))
                .lastFourDigits(cardNumber.substring(cardNumber.length() - 4))
                .cvvHash(hash(cvv))
                .expirationMonth(expirationDate.getMonthValue())
                .expirationYear(expirationDate.getYear())
                .status(CreditCardStatus.ACTIVE)
                .creditLimit(INITIAL_CREDIT_LIMIT)
                .availableLimit(INITIAL_CREDIT_LIMIT)
                .invoiceBalance(BigDecimal.ZERO)
                .closingDay(25)
                .dueDay(10)
                .build();

        return toCardResponse(creditCardRepository.save(card));
    }

    private CreditCardTransaction approvePurchase(
            CreditCard card,
            BigDecimal amount,
            Integer installments,
            String merchantName,
            String description,
            String reference
    ) {
        BigDecimal installmentAmount = calculateInstallmentAmount(amount, installments);

        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        card.setInvoiceBalance(card.getInvoiceBalance().add(amount));

        Invoice invoice = invoiceService.getOrCreateOpenInvoice(card);
        invoice.setTotalAmount(invoice.getTotalAmount().add(amount));

        CreditCardTransaction transaction = CreditCardTransaction.builder()
                .creditCard(card)
                .amount(amount)
                .merchantName(merchantName)
                .description(description)
                .installments(installments)
                .installmentAmount(installmentAmount)
                .status(CreditCardTransactionStatus.APPROVED)
                .build();

        CardPurchase purchase = CardPurchase.builder()
                .card(card)
                .invoice(invoice)
                .amount(amount)
                .description(description)
                .reference(reference)
                .build();

        creditCardRepository.save(card);
        invoiceRepository.save(invoice);
        cardPurchaseRepository.save(purchase);
        CreditCardTransaction savedTx = transactionRepository.save(transaction);

        accountService.creditMerchant(amount, reference, "Compra no cartao: " + description);
        publishOrderPaid(card, savedTx, amount);

        return savedTx;
    }

    private void validateCardCanPurchase(
            CreditCard card,
            BigDecimal amount,
            String merchantName,
            String description,
            Integer installments
    ) {
        if (card.getStatus() != CreditCardStatus.ACTIVE) {
            saveDeclinedTransaction(card, amount, installments, merchantName, description, "Cartao bloqueado ou cancelado");
            throw new CreditCardBlockedException();
        }

        if (card.getAvailableLimit().compareTo(amount) < 0) {
            saveDeclinedTransaction(card, amount, installments, merchantName, description, "Limite insuficiente");
            throw new InsufficientCreditLimitException();
        }
    }

    private CreditCard findCardByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        return creditCardRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CreditCardNotFoundException(user.getId()));
    }

    private void publishOrderPaid(CreditCard card, CreditCardTransaction savedTx, BigDecimal amount) {
        User payer = card.getUser();
        eventPublisher.publishEvent(new OrderPaidEvent(
                savedTx.getId(),
                payer.getId(),
                payer.getName(),
                payer.getEmail(),
                amount,
                "CREDIT_CARD",
                Instant.now()
        ));
    }

    private CreditCardTransaction saveDeclinedTransaction(
            CreditCard card,
            BigDecimal amount,
            Integer installments,
            String merchantName,
            String description,
            String reason
    ) {
        CreditCardTransaction transaction = CreditCardTransaction.builder()
                .creditCard(card)
                .amount(amount)
                .merchantName(merchantName)
                .description(description)
                .installments(installments)
                .installmentAmount(calculateInstallmentAmount(amount, installments))
                .status(CreditCardTransactionStatus.DECLINED)
                .declineReason(reason)
                .build();
        return transactionRepository.save(transaction);
    }

    private CreditCardResponse toCardResponse(CreditCard card) {
        return new CreditCardResponse(
                card.getId(),
                card.getHolderName(),
                "**** **** **** " + card.getLastFourDigits(),
                card.getExpirationMonth(),
                card.getExpirationYear(),
                card.getStatus(),
                card.getCreditLimit(),
                card.getAvailableLimit(),
                card.getInvoiceBalance()
        );
    }

    private CreditLimitResponse toLimitResponse(CreditCard card) {
        return new CreditLimitResponse(
                card.getCreditLimit(),
                card.getAvailableLimit(),
                card.getCreditLimit().subtract(card.getAvailableLimit()),
                card.getInvoiceBalance()
        );
    }

    private CreditCardTransactionResponse toTransactionResponse(CreditCardTransaction transaction) {
        return new CreditCardTransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getInstallments(),
                transaction.getInstallmentAmount(),
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getDeclineReason(),
                transaction.getCreatedAt()
        );
    }

    private CreditPaymentResponse toPaymentResponse(CreditCardTransaction transaction, CreditCard card) {
        return new CreditPaymentResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getInstallments(),
                transaction.getInstallmentAmount(),
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getDeclineReason(),
                card.getAvailableLimit(),
                card.getInvoiceBalance(),
                transaction.getCreatedAt()
        );
    }

    private CardPurchaseResponse toPurchaseResponse(CardPurchase purchase) {
        return new CardPurchaseResponse(
                purchase.getId(),
                purchase.getInvoice().getId(),
                purchase.getAmount(),
                purchase.getDescription(),
                purchase.getReference(),
                purchase.getPurchaseDate()
        );
    }

    private BigDecimal calculateInstallmentAmount(BigDecimal amount, Integer installments) {
        int installmentCount = installments == null || installments <= 0 ? 1 : installments;
        return amount.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponivel", ex);
        }
    }
}
