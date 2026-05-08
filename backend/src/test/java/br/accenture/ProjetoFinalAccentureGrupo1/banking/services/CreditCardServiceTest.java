package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.service.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCardTransaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardPurchaseRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardTransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditPaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditPaymentResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardTransactionStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.CreditCardBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientCreditLimitException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardTransactionRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private CreditCardTransactionRepository transactionRepository;

    @Mock
    private CreditCardNumberGenerator cardNumberGenerator;

    @Mock
    private AccountService accountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreditCardService creditCardService;

    private User user;
    private CreditCard card;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        card = CreditCard.builder()
                .id(10L)
                .user(user)
                .holderName("Ana Silva")
                .numberHash("number-hash")
                .lastFourDigits("1111")
                .cvvHash("cvv-hash")
                .expirationMonth(5)
                .expirationYear(2031)
                .status(CreditCardStatus.ACTIVE)
                .creditLimit(new BigDecimal("1000.00"))
                .availableLimit(new BigDecimal("1000.00"))
                .invoiceBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void createVirtualCardForUser_DeveCriarCartaoComLimiteInicial() {
        when(creditCardRepository.existsByUserId(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardNumberGenerator.generateCardNumber()).thenReturn("5432101234567895");
        when(cardNumberGenerator.generateCvv()).thenReturn("123");
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> {
            CreditCard saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CreditCardResponse response = creditCardService.createVirtualCardForUser(1L);

        assertEquals("Ana Silva", response.holderName());
        assertEquals("**** **** **** 7895", response.maskedNumber());
        assertEquals(CreditCardStatus.ACTIVE, response.status());
        assertEquals(new BigDecimal("1000.00"), response.creditLimit());
        assertEquals(new BigDecimal("1000.00"), response.availableLimit());
    }

    @Test
    void purchase_DeveAprovarCompra_QuandoHaLimiteDisponivel() {
        CreditCardPurchaseRequest request = new CreditCardPurchaseRequest(
                new BigDecimal("250.00"),
                "Loja Accenture",
                "Compra no ecommerce"
        );

        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(creditCardRepository.findByUserId(1L)).thenReturn(Optional.of(card));
        when(transactionRepository.save(any(CreditCardTransaction.class))).thenAnswer(invocation -> {
            CreditCardTransaction saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        CreditCardTransactionResponse response = creditCardService.purchase("ana@email.com", request);

        assertEquals(CreditCardTransactionStatus.APPROVED, response.status());
        assertEquals(new BigDecimal("750.00"), card.getAvailableLimit());
        assertEquals(new BigDecimal("250.00"), card.getInvoiceBalance());
        verify(creditCardRepository).save(card);
        verify(accountService).creditMerchantForCreditCardSale(new BigDecimal("250.00"));
        verify(eventPublisher).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    void payWithCredit_DeveAprovarPagamentoParcelado_QuandoCvvELimiteSaoValidos() {
        CreditPaymentRequest request = new CreditPaymentRequest(
                new BigDecimal("300.00"),
                3,
                "Loja Accenture",
                "Pedido #123"
        );

        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(creditCardRepository.findByUserId(1L)).thenReturn(Optional.of(card));
        when(transactionRepository.save(any(CreditCardTransaction.class))).thenAnswer(invocation -> {
            CreditCardTransaction saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        CreditPaymentResponse response = creditCardService.payWithCredit("ana@email.com", request);

        assertEquals(30L, response.transactionId());
        assertEquals(CreditCardTransactionStatus.APPROVED, response.status());
        assertEquals(3, response.installments());
        assertEquals(new BigDecimal("100.00"), response.installmentAmount());
        assertEquals(new BigDecimal("700.00"), response.remainingLimit());
        assertEquals(new BigDecimal("300.00"), response.invoiceBalance());
        assertEquals(new BigDecimal("700.00"), card.getAvailableLimit());
        assertEquals(new BigDecimal("300.00"), card.getInvoiceBalance());
        verify(creditCardRepository).save(card);
        verify(accountService).creditMerchantForCreditCardSale(new BigDecimal("300.00"));
        verify(eventPublisher).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    void purchase_DeveRecusarCompra_QuandoLimiteEInsuficiente() {
        CreditCardPurchaseRequest request = new CreditCardPurchaseRequest(
                new BigDecimal("1200.00"),
                "Loja Accenture",
                "Compra acima do limite"
        );

        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(creditCardRepository.findByUserId(1L)).thenReturn(Optional.of(card));

        assertThrows(InsufficientCreditLimitException.class, () -> creditCardService.purchase("ana@email.com", request));

        ArgumentCaptor<CreditCardTransaction> captor = ArgumentCaptor.forClass(CreditCardTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(CreditCardTransactionStatus.DECLINED, captor.getValue().getStatus());
        assertEquals("Limite insuficiente", captor.getValue().getDeclineReason());
        assertEquals(new BigDecimal("1000.00"), card.getAvailableLimit());
        verify(accountService, never()).creditMerchantForCreditCardSale(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void purchase_DeveRecusarCompra_QuandoCartaoEstaBloqueado() {
        card.setStatus(CreditCardStatus.BLOCKED);
        CreditCardPurchaseRequest request = new CreditCardPurchaseRequest(
                new BigDecimal("100.00"),
                "Loja Accenture",
                "Compra com cartao bloqueado"
        );

        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(creditCardRepository.findByUserId(1L)).thenReturn(Optional.of(card));

        assertThrows(CreditCardBlockedException.class, () -> creditCardService.purchase("ana@email.com", request));

        ArgumentCaptor<CreditCardTransaction> captor = ArgumentCaptor.forClass(CreditCardTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(CreditCardTransactionStatus.DECLINED, captor.getValue().getStatus());
        assertEquals("Cartao bloqueado ou cancelado", captor.getValue().getDeclineReason());
        verify(accountService, never()).creditMerchantForCreditCardSale(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
