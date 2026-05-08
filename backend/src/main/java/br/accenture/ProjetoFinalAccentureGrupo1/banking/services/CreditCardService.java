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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        if (card.getStatus() != CreditCardStatus.ACTIVE) {
            saveDeclinedTransaction(card, request, "Cartao bloqueado ou cancelado");
            throw new CreditCardBlockedException();
        }

        if (card.getAvailableLimit().compareTo(request.amount()) < 0) {
            saveDeclinedTransaction(card, request, "Limite insuficiente");
            throw new InsufficientCreditLimitException();
        }

        card.setAvailableLimit(card.getAvailableLimit().subtract(request.amount()));
        card.setInvoiceBalance(card.getInvoiceBalance().add(request.amount()));
        Invoice invoice = invoiceService.getOrCreateOpenInvoice(card);
        invoice.setTotalAmount(invoice.getTotalAmount().add(request.amount()));

        CreditCardTransaction transaction = CreditCardTransaction.builder()
                .creditCard(card)
                .amount(request.amount())
                .merchantName(request.merchantName())
                .description(request.description())
                .status(CreditCardTransactionStatus.APPROVED)
                .build();

        CardPurchase purchase = CardPurchase.builder()
                .card(card)
                .invoice(invoice)
                .amount(request.amount())
                .description(request.description())
                .reference(request.reference())
                .build();

        creditCardRepository.save(card);
        invoiceRepository.save(invoice);
        cardPurchaseRepository.save(purchase);
        accountService.creditMerchant(request.amount(), request.reference(), "Compra no cartao: " + request.description());
        return toTransactionResponse(transactionRepository.save(transaction));
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

    private CreditCard findCardByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        return creditCardRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CreditCardNotFoundException(user.getId()));
    }

    private void saveDeclinedTransaction(CreditCard card, CreditCardPurchaseRequest request, String reason) {
        CreditCardTransaction transaction = CreditCardTransaction.builder()
                .creditCard(card)
                .amount(request.amount())
                .merchantName(request.merchantName())
                .description(request.description())
                .status(CreditCardTransactionStatus.DECLINED)
                .declineReason(reason)
                .build();
        transactionRepository.save(transaction);
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
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getDeclineReason(),
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
