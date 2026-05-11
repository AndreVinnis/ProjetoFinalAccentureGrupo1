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
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.PaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
// Autor: André Vinícius Barros Macambira
public class PixService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final AccountService accountService;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PaymentRequest getByCode(String code) {
        return paymentRequestRepository.findByCode(code)
                .orElseThrow(() -> new PaymentRequestNotFoundException(code));
    }

    @Transactional
    public PaymentRequest payByCode(String code, String payerEmail) {
        PaymentRequest request = paymentRequestRepository.findByCode(code)
                .orElseThrow(() -> new PaymentRequestNotFoundException(code));

        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new PaymentRequestNotPayableException(code, request.getStatus().name());
        }

        Instant now = Instant.now();
        if (now.isAfter(request.getExpiresAt())) {
            request.setStatus(PaymentRequestStatus.EXPIRED);
            paymentRequestRepository.save(request);
            eventPublisher.publishEvent(new PaymentExpiredEvent(request.getReference(), now));
            throw new PaymentRequestNotPayableException(code, "EXPIRED");
        }

        UserInfo payer = userFacade.findByEmail(payerEmail);
        Account payerAccount = accountService.findByUserId(payer.id());

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
