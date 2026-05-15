package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentExpiredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentReceivedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.WrongPasswordException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.PaymentRequestRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.shared.security.AESEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
// Autor: André Vinícius Barros Macambira
public class PixService {

    private final static String defaultMessage = "Transação PIX";

    private final PaymentRequestRepository paymentRequestRepository;
    private final AccountService accountService;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final AESEncryptionService encryptionService;

    @Transactional(readOnly = true)
    public PaymentRequest getByCode(String code) {
        return paymentRequestRepository.findByCode(code)
                .orElseThrow(() -> new PaymentRequestNotFoundException(code));
    }

    @Transactional
    public void passPix(String payerEmail, String password, String recipientEmail, BigDecimal amount, String description){
        Account payer = accountService.findAccountByUserEmail(payerEmail);
        Account recipient = accountService.findAccountByUserEmail(recipientEmail);

        if(!payer.getPassword().equals(encryptionService.encrypt(password))){
            throw new WrongPasswordException();
        }
        accountService.debit(payer, amount, defaultMessage, description, TransactionType.PAYMENT);
        accountService.credit(recipient, amount, defaultMessage, description, TransactionType.PAYMENT);
    }

    @Transactional
    public PaymentRequest payByCode(String code, String payerEmail, String password) {
        PaymentRequest request = paymentRequestRepository.findByCode(code)
                .orElseThrow(() -> new PaymentRequestNotFoundException(code));

        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new PaymentRequestNotPayableException(code, request.getStatus().name());
        }

        UserInfo payer = userFacade.findByEmail(payerEmail);
        Account payerAccount = accountService.findAccountByUserEmail(payerEmail);

        if(!payerAccount.getPassword().equals(encryptionService.encrypt(password))){
            throw new WrongPasswordException();
        }

        Instant now = Instant.now();
        if (now.isAfter(request.getExpiresAt())) {
            request.setStatus(PaymentRequestStatus.EXPIRED);
            paymentRequestRepository.save(request);
            eventPublisher.publishEvent(new PaymentExpiredEvent(request.getReference(), now));
            throw new PaymentRequestNotPayableException(code, "EXPIRED");
        }

        accountService.debit(payerAccount, request.getAmount(), request.getReference(), request.getDescription(), TransactionType.PAYMENT);
        accountService.credit(request.getRecipient(), request.getAmount(), request.getReference(), request.getDescription(), TransactionType.PAYMENT);

        request.setStatus(PaymentRequestStatus.PAID);
        request.setPaidAt(now);
        request.setPaidByUserId(payer.id());
        paymentRequestRepository.save(request);

        eventPublisher.publishEvent(new PaymentReceivedEvent(
                request.getReference(),
                payer.id(),
                request.getAmount(),
                now
        ));

        return request;
    }
}
