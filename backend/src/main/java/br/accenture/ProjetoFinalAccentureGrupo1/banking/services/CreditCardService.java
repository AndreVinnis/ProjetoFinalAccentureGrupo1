package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCardTransaction;
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
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardTransactionRepository;
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
    private final CreditCardNumberGenerator cardNumberGenerator;

    @Transactional
    public CreditCardResponse createVirtualCardForUser(Long userId) {
        if (creditCardRepository.existsByUserId(userId)) {
            throw new CreditCardAlreadyExistsException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + userId));

        LocalDate expirationDate = LocalDate.now().plusYears(5);
        String cardNumber = cardNumberGenerator.generateCardNumber();
        String cvv = cardNumberGenerator.generateCvv();

        CreditCard card = CreditCard.builder()
                .user(user)
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

        CreditCardTransaction transaction = CreditCardTransaction.builder()
                .creditCard(card)
                .amount(request.amount())
                .merchantName(request.merchantName())
                .description(request.description())
                .status(CreditCardTransactionStatus.APPROVED)
                .build();

        creditCardRepository.save(card);
        return toTransactionResponse(transactionRepository.save(transaction));
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
