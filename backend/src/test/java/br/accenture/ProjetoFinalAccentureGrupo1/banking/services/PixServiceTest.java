package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentReceivedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.PaymentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Autor: André Vinícius Barros Macambira
class PixServiceTest {

    @Mock private PaymentRequestRepository paymentRequestRepository;
    @Mock private AccountService accountService;
    @Mock private UserFacade userFacade;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PixService pixService;

    private PaymentRequest pendingRequest;
    private Account merchantAccount;
    private Account payerAccount;
    private UserInfo payerInfo;

    @BeforeEach
    void setUp() {
        merchantAccount = Account.builder()
                .id(1L)
                .accountNumber("00000-0")
                .balance(new BigDecimal("10000000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();

        payerAccount = Account.builder()
                .id(2L)
                .userId(10L)
                .accountNumber("00001-0")
                .balance(new BigDecimal("500.00"))
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        pendingRequest = PaymentRequest.builder()
                .id(100L)
                .code("abc-123")
                .recipient(merchantAccount)
                .amount(new BigDecimal("100.00"))
                .description("Pedido #42")
                .reference("ORDER-42")
                .status(PaymentRequestStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        payerInfo = new UserInfo(10L, "Ana", "ana@email.com", "12345678901",
                LocalDate.of(1990, 1, 1), Set.of(Role.CUSTOMER));
    }

    @Test
    void payByCode_DeveProcessarPagamento_QuandoCobrancaValida() {
        when(paymentRequestRepository.findByCode("abc-123")).thenReturn(Optional.of(pendingRequest));
        when(userFacade.findByEmail("ana@email.com")).thenReturn(payerInfo);
        when(accountService.findByUserId(10L)).thenReturn(payerAccount);
        when(paymentRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        PaymentRequest result = pixService.payByCode("abc-123", "ana@email.com");

        assertEquals(PaymentRequestStatus.PAID, result.getStatus());
        assertEquals(10L, result.getPaidByUserId());
        verify(accountService).debit(payerAccount, new BigDecimal("100.00"), pendingRequest.getReference(), pendingRequest.getDescription(), TransactionType.PAYMENT);
        verify(accountService).credit(merchantAccount, new BigDecimal("100.00"), pendingRequest.getReference(), pendingRequest.getDescription(), TransactionType.PAYMENT);
        verify(eventPublisher).publishEvent(any(PaymentReceivedEvent.class));
    }

    @Test
    void payByCode_DeveLancarException_QuandoCodigoNaoExiste() {
        when(paymentRequestRepository.findByCode("nope")).thenReturn(Optional.empty());

        assertThrows(
                PaymentRequestNotFoundException.class,
                () -> pixService.payByCode("nope", "ana@email.com")
        );
    }

    @Test
    void payByCode_DeveLancarException_QuandoJaPaga() {
        pendingRequest.setStatus(PaymentRequestStatus.PAID);
        when(paymentRequestRepository.findByCode("abc-123")).thenReturn(Optional.of(pendingRequest));

        assertThrows(
                PaymentRequestNotPayableException.class,
                () -> pixService.payByCode("abc-123", "ana@email.com")
        );
    }

    @Test
    void payByCode_DeveExpirarELancarException_QuandoVencida() {
        pendingRequest.setExpiresAt(Instant.now().minusSeconds(60));
        when(paymentRequestRepository.findByCode("abc-123")).thenReturn(Optional.of(pendingRequest));

        assertThrows(
                PaymentRequestNotPayableException.class,
                () -> pixService.payByCode("abc-123", "ana@email.com")
        );

        // confirma que marcou como EXPIRED no banco
        verify(paymentRequestRepository).save(pendingRequest);
        assertEquals(PaymentRequestStatus.EXPIRED, pendingRequest.getStatus());
    }

    @Test
    void getByCode_DeveRetornarCobranca_QuandoExiste() {
        when(paymentRequestRepository.findByCode("abc-123")).thenReturn(Optional.of(pendingRequest));

        PaymentRequest result = pixService.getByCode("abc-123");

        assertEquals("abc-123", result.getCode());
    }
}
