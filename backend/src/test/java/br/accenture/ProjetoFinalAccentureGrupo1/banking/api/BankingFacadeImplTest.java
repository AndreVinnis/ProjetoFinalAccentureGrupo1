package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.PaymentRequestRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankingFacadeImplTest {

    @Mock private AccountService accountService;
    @Mock private CreditCardService creditCardService;
    @Mock private AccountRepository accountRepository;
    @Mock private PaymentRequestRepository paymentRequestRepository;

    @InjectMocks
    private BankingFacadeImpl bankingFacade;

    private Account customer;
    private Account merchant;

    @BeforeEach
    void setUp() {
        customer = Account.builder()
                .id(1L)
                .userId(10L)
                .accountNumber("00001-0")
                .balance(new BigDecimal("250.00"))
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        merchant = Account.builder()
                .id(99L)
                .accountNumber("99999-0")
                .balance(new BigDecimal("10000000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void getBalance_DeveDelegarParaAccountService() {
        when(accountService.getBalance(10L)).thenReturn(new BigDecimal("250.00"));

        BigDecimal balance = bankingFacade.getBalance(10L);

        assertEquals(new BigDecimal("250.00"), balance);
        verify(accountService).getBalance(10L);
    }

    @Test
    void getAccountInfo_DeveRetornarAccountInfoMapeado() {
        when(accountService.findByUserId(10L)).thenReturn(customer);

        AccountInfo info = bankingFacade.getAccountInfo(10L);

        assertEquals("00001-0", info.accountNumber());
        assertEquals(new BigDecimal("250.00"), info.balance());
        assertEquals(AccountStatus.ACTIVE, info.status());
    }

    @Test
    void getAccountInfo_DevePropagarExcecao_QuandoContaNaoExiste() {
        when(accountService.findByUserId(99L)).thenThrow(new RuntimeException("conta não encontrada"));

        assertThrows(RuntimeException.class, () -> bankingFacade.getAccountInfo(99L));
    }

    @Test
    void verifyCard_DeveDelegarParaCreditCardService() {
        CardValidationResponse expected = new CardValidationResponse(5L, "1234");
        when(creditCardService.validateCard("1234567890121234", "123", 12, 2030))
                .thenReturn(expected);

        CardValidationResponse result = bankingFacade.verifyCard("1234567890121234", "123", 12, 2030);

        assertSame(expected, result);
        verify(creditCardService).validateCard("1234567890121234", "123", 12, 2030);
    }

    @Test
    void chargeCard_DeveDelegarParaCreditCardService() {
        bankingFacade.chargeCard(5L, new BigDecimal("100.00"), "123", "Compra X", "ORDER-1", 4);

        verify(creditCardService).chargeCard(5L, new BigDecimal("100.00"), "123", "Compra X", "ORDER-1", 4);
    }

    @Test
    void issueRefund_DeveDelegarParaAccountService() {
        bankingFacade.issueRefund(10L, new BigDecimal("75.00"), "ORDER-42", "Estorno");

        verify(accountService).refund(10L, new BigDecimal("75.00"), "ORDER-42", "Estorno");
    }

    @Test
    void cancelCardPurchase_DeveDelegarParaCreditCardService() {
        when(creditCardService.cancelPurchase("ORDER-42", "Estorno"))
                .thenReturn(new BigDecimal("33.34"));

        BigDecimal refundedAmount = bankingFacade.cancelCardPurchase("ORDER-42", "Estorno");

        assertEquals(new BigDecimal("33.34"), refundedAmount);
        verify(creditCardService).cancelPurchase("ORDER-42", "Estorno");
    }

    @Test
    void applyCashback_DeveDelegarParaAccountService() {
        bankingFacade.applyCashback(10L, new BigDecimal("12.50"), "ORDER-42", "Cashback");

        verify(accountService).cashback(10L, new BigDecimal("12.50"), "ORDER-42", "Cashback");
    }

    @Test
    void createPaymentRequest_DeveCriarComCodigoUuidExpirando30Min() {
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(paymentRequestRepository.save(any(PaymentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        String code = bankingFacade.createPaymentRequest(
                new BigDecimal("199.90"), "Pedido #42", "ORDER-42");
        Instant after = Instant.now();

        assertNotNull(code);
        // O código deve ser um UUID válido.
        UUID.fromString(code);

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestRepository).save(captor.capture());
        PaymentRequest saved = captor.getValue();

        assertEquals(code, saved.getCode());
        assertEquals(new BigDecimal("199.90"), saved.getAmount());
        assertEquals("Pedido #42", saved.getDescription());
        assertEquals("ORDER-42", saved.getReference());
        assertSame(merchant, saved.getRecipient());
        assertEquals(PaymentRequestStatus.PENDING, saved.getStatus());

        // createdAt está entre os instantes capturados antes/depois da chamada
        assertTrue(!saved.getCreatedAt().isBefore(before) && !saved.getCreatedAt().isAfter(after));

        // expiresAt = createdAt + 30 minutos
        Duration delta = Duration.between(saved.getCreatedAt(), saved.getExpiresAt());
        assertEquals(Duration.ofMinutes(30), delta);
    }

    @Test
    void createPaymentRequest_DeveLancarException_QuandoMerchantNaoExiste() {
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bankingFacade.createPaymentRequest(
                        new BigDecimal("10.00"), "x", "REF-1"));

        assertTrue(ex.getMessage().contains("MERCHANT"));
        verify(paymentRequestRepository, never()).save(any());
    }

    @Test
    void cancelPaymentRequest_DeveCancelar_QuandoStatusPending() {
        PaymentRequest request = PaymentRequest.builder()
                .id(1L)
                .code(UUID.randomUUID().toString())
                .recipient(merchant)
                .amount(new BigDecimal("10.00"))
                .description("x")
                .reference("ORDER-42")
                .status(PaymentRequestStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        when(paymentRequestRepository.findByReference("ORDER-42"))
                .thenReturn(Optional.of(request));

        bankingFacade.cancelPaymentRequest("ORDER-42");

        assertEquals(PaymentRequestStatus.CANCELLED, request.getStatus());
        verify(paymentRequestRepository).save(request);
    }

    @Test
    void cancelPaymentRequest_DeveLancarException_QuandoCobrancaNaoEncontrada() {
        when(paymentRequestRepository.findByReference("INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(PaymentRequestNotFoundException.class,
                () -> bankingFacade.cancelPaymentRequest("INVALID"));
        verify(paymentRequestRepository, never()).save(any());
    }

    @Test
    void cancelPaymentRequest_DeveLancarException_QuandoStatusJaPaga() {
        PaymentRequest paid = PaymentRequest.builder()
                .id(2L)
                .code(UUID.randomUUID().toString())
                .recipient(merchant)
                .amount(new BigDecimal("10.00"))
                .description("x")
                .reference("ORDER-99")
                .status(PaymentRequestStatus.PAID)
                .createdAt(Instant.now())
                .expiresAt(Instant.now())
                .build();

        when(paymentRequestRepository.findByReference("ORDER-99"))
                .thenReturn(Optional.of(paid));

        PaymentRequestNotPayableException ex = assertThrows(
                PaymentRequestNotPayableException.class,
                () -> bankingFacade.cancelPaymentRequest("ORDER-99"));

        assertTrue(ex.getMessage().contains("PAID"));
        // Status original preservado e save NÃO chamado
        assertEquals(PaymentRequestStatus.PAID, paid.getStatus());
        verify(paymentRequestRepository, never()).save(any());
    }

    @Test
    void cancelPaymentRequest_DeveLancarException_QuandoStatusExpirada() {
        PaymentRequest expired = PaymentRequest.builder()
                .id(3L)
                .code(UUID.randomUUID().toString())
                .recipient(merchant)
                .amount(new BigDecimal("10.00"))
                .description("x")
                .reference("ORDER-77")
                .status(PaymentRequestStatus.EXPIRED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now())
                .build();

        when(paymentRequestRepository.findByReference("ORDER-77"))
                .thenReturn(Optional.of(expired));

        assertThrows(PaymentRequestNotPayableException.class,
                () -> bankingFacade.cancelPaymentRequest("ORDER-77"));
        verify(paymentRequestRepository, never()).save(any());
    }

    @Test
    void cancelPaymentRequest_DeveLancarException_QuandoStatusJaCancelada() {
        PaymentRequest cancelled = PaymentRequest.builder()
                .id(4L)
                .code(UUID.randomUUID().toString())
                .recipient(merchant)
                .amount(new BigDecimal("10.00"))
                .description("x")
                .reference("ORDER-11")
                .status(PaymentRequestStatus.CANCELLED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now())
                .build();

        when(paymentRequestRepository.findByReference("ORDER-11"))
                .thenReturn(Optional.of(cancelled));

        assertThrows(PaymentRequestNotPayableException.class,
                () -> bankingFacade.cancelPaymentRequest("ORDER-11"));
        verify(paymentRequestRepository, never()).save(any());
    }
}
