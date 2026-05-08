package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvalidInvoicePaymentException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-05-08T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @InjectMocks
    private InvoiceService invoiceService;

    private User user;
    private CreditCard card;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository,
                creditCardRepository,
                userRepository,
                accountService,
                eventPublisher,
                clock
        );

        user = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .build();

        card = CreditCard.builder()
                .id(10L)
                .user(user)
                .holderName("Ana Silva")
                .status(CreditCardStatus.ACTIVE)
                .creditLimit(new BigDecimal("1000.00"))
                .availableLimit(new BigDecimal("750.00"))
                .invoiceBalance(new BigDecimal("250.00"))
                .closingDay(25)
                .dueDay(10)
                .build();

        invoice = Invoice.builder()
                .id(20L)
                .card(card)
                .referenceMonth(YearMonth.of(2026, 5))
                .closingDate(LocalDate.of(2026, 5, 25))
                .dueDate(LocalDate.of(2026, 6, 10))
                .totalAmount(new BigDecimal("250.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.CLOSED)
                .build();
    }

    @Test
    void payInvoice_DeveQuitarFaturaERestaurarLimite() {
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(invoiceRepository.findById(20L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.payInvoice(20L, "ana@email.com", new BigDecimal("250.00"));

        assertEquals(InvoiceStatus.PAID, response.status());
        assertEquals(new BigDecimal("250.00"), response.paidAmount());
        assertEquals(Instant.parse("2026-05-08T12:00:00Z"), response.paidAt());
        assertEquals(new BigDecimal("1000.00"), card.getAvailableLimit());
        assertEquals(new BigDecimal("0.00"), card.getInvoiceBalance());
        verify(accountService).debitInvoicePayment(1L, new BigDecimal("250.00"), "INVOICE-20", "Pagamento de fatura");
        verify(creditCardRepository).save(card);

        ArgumentCaptor<InvoicePaidEvent> eventCaptor = ArgumentCaptor.forClass(InvoicePaidEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(20L, eventCaptor.getValue().invoiceId());
        assertEquals(1L, eventCaptor.getValue().userId());
        assertEquals(new BigDecimal("250.00"), eventCaptor.getValue().amountPaid());
    }

    @Test
    void payInvoice_DevePermitirPagamentoParcialSemPublicarEvento() {
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(invoiceRepository.findById(20L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.payInvoice(20L, "ana@email.com", new BigDecimal("100.00"));

        assertEquals(InvoiceStatus.CLOSED, response.status());
        assertEquals(new BigDecimal("100.00"), response.paidAmount());
        assertEquals(new BigDecimal("850.00"), card.getAvailableLimit());
        assertEquals(new BigDecimal("150.00"), card.getInvoiceBalance());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void payInvoice_DeveRecusarFaturaAberta() {
        invoice.setStatus(InvoiceStatus.OPEN);

        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(invoiceRepository.findById(20L)).thenReturn(Optional.of(invoice));

        assertThrows(
                InvalidInvoicePaymentException.class,
                () -> invoiceService.payInvoice(20L, "ana@email.com", new BigDecimal("100.00"))
        );

        verify(accountService, never()).debitInvoicePayment(any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
