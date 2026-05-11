package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.PaymentRequestRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
// André Vinícius Barros Macambira
public class BankingFacadeImpl implements BankingFacade {

    private static final long PIX_EXPIRATION_MINUTES = 30;

    private final AccountService accountService;
    private final CreditCardService creditCardService;
    private final AccountRepository accountRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    @Override
    public BigDecimal getBalance(Long userId) {
        return accountService.getBalance(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfo getAccountInfo(Long userId) {
        Account account = accountService.findByUserId(userId);
        return new AccountInfo(
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus()
        );
    }

    @Override
    public CardValidationResponse verifyCard(String cardNumber, String cvv, int expirationMonth, int expirationYear) {
        return creditCardService.validateCard(cardNumber, cvv, expirationMonth, expirationYear);
    }

    @Override
    public void chargeCard(Long cardId, BigDecimal amount, String cvv, String description, String reference) {
        creditCardService.chargeCard(cardId, amount, cvv, description, reference);
    }

    @Override
    public void issueRefund(Long toUserId, BigDecimal amount, String reference, String description) {
        accountService.refund(toUserId, amount, reference, description);
    }

    @Override
    @Transactional
    public String createPaymentRequest(BigDecimal amount, String description, String reference) {
        Account merchant = accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseThrow(() -> new IllegalStateException(
                        "Conta MERCHANT não encontrada. CompanyAccountInitializer não rodou!"
                ));

        Instant now = Instant.now();
        PaymentRequest request = PaymentRequest.builder()
                .code(UUID.randomUUID().toString())
                .recipient(merchant)
                .amount(amount)
                .description(description)
                .reference(reference)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plus(PIX_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .build();

        paymentRequestRepository.save(request);
        return request.getCode();
    }

    @Override
    @Transactional
    public void cancelPaymentRequest(String code) {
        PaymentRequest request = paymentRequestRepository.findByCode(code)
                .orElseThrow(() -> new PaymentRequestNotFoundException(code));

        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new PaymentRequestNotPayableException(code, request.getStatus().name());
        }

        request.setStatus(PaymentRequestStatus.CANCELLED);
        paymentRequestRepository.save(request);
    }
}
