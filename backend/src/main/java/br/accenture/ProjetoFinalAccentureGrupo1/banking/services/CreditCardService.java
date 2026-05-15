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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        CreditCard card = creditCardRepository.findById(cardId).orElseThrow(()
            -> new CardNotFoundException(cardId));
        validateCardCanPurchase(card,amount, cvv);

        Invoice currentInvoice = invoiceService.getOrCreateOpenInvoice(card);

        CardPurchase cardPurchase = CardPurchase.builder()
                .card(card)
                .invoice(currentInvoice)
                .amount(amount)
                .description(description)
                .reference(refence)
                .build();

        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        invoiceService.addCardPurchase(currentInvoice, cardPurchase);
        cardPurchaseRepository.save(cardPurchase);
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
                purchase.getPurchaseDate()
        );
    }
}
