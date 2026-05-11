package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoiceOverdueEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.*;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private UserFacade userFacade;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private AccountService accountService;
    @Mock private AccountRepository accountRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Clock clock;

    @InjectMocks
    private InvoiceService invoiceService;

    private Account account;
    private CreditCard card;
    private Invoice openInvoice;
    private Invoice closedInvoice;
    private Invoice overdueInvoice;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .userId(10L)
                .status(AccountStatus.ACTIVE)
                .build();

        card = CreditCard.builder()
                .id(100L)
                .account(account)
                .closingDay(25)
                .dueDay(10)
                .creditLimit(new BigDecimal("1000.00"))
                .availableLimit(new BigDecimal("700.00"))
                .status(CreditCardStatus.ACTIVE)
                .build();

        openInvoice = Invoice.builder()
                .id(50L).card(card)
                .referenceMonth(YearMonth.of(2026, 5))
                .closingDate(LocalDate.of(2026, 5, 25))
                .dueDate(LocalDate.of(2026, 6, 10))
                .totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.OPEN)
                .build();

        closedInvoice = Invoice.builder()
                .id(51L).card(card)
                .referenceMonth(YearMonth.of(2026, 4))
                .closingDate(LocalDate.of(2026, 4, 25))
                .dueDate(LocalDate.of(2026, 5, 10))
                .totalAmount(new BigDecimal("100.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.CLOSED)
                .build();

        overdueInvoice = Invoice.builder()
                .id(52L).card(card)
                .referenceMonth(YearMonth.of(2026, 3))
                .closingDate(LocalDate.of(2026, 3, 25))
                .dueDate(LocalDate.of(2026, 4, 10))
                .totalAmount(new BigDecimal("200.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.OVERDUE)
                .build();

        userInfo = new UserInfo(10L, "Ana", "ana@email.com",
                "12345678901", LocalDate.of(1990, 1, 1), Set.of(Role.CUSTOMER));
    }

    private void mockClockToday(LocalDate today) {
        when(clock.instant()).thenReturn(
                today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void getOrCreateOpenInvoice_DeveRetornarExistente_QuandoJaTemAberta() {
        when(invoiceRepository.findByCardIdAndStatus(100L, InvoiceStatus.OPEN))
                .thenReturn(Optional.of(openInvoice));

        Invoice result = invoiceService.getOrCreateOpenInvoice(card);

        assertSame(openInvoice, result);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getOrCreateOpenInvoice_DeveCriarNova_QuandoNaoExisteAberta() {
        mockClockToday(LocalDate.of(2026, 5, 10));
        when(invoiceRepository.findByCardIdAndStatus(100L, InvoiceStatus.OPEN))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findByCardIdAndReferenceMonth(eq(100L), any()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.getOrCreateOpenInvoice(card);

        assertNotNull(result);
        assertEquals(InvoiceStatus.OPEN, result.getStatus());
        assertEquals(YearMonth.of(2026, 5), result.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 5, 25), result.getClosingDate());
        assertEquals(LocalDate.of(2026, 6, 10), result.getDueDate());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
    }

    @Test
    void closeInvoice_DeveFechar_QuandoOpen() {
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(openInvoice));
        when(invoiceRepository.save(openInvoice)).thenReturn(openInvoice);

        Invoice result = invoiceService.closeInvoice(50L);

        assertEquals(InvoiceStatus.CLOSED, result.getStatus());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void closeInvoice_DeveLancarException_QuandoNaoEstaAberta() {
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));

        assertThrows(
                InvoiceNotCloseableException.class,
                () -> invoiceService.closeInvoice(51L)
        );
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void closeInvoice_DeveLancarException_QuandoNaoEncontrada() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                InvoiceNotFoundException.class,
                () -> invoiceService.closeInvoice(999L)
        );
    }

    @Test
    void closeDueInvoices_DeveFecharTodasOpenComDataPassada() {
        mockClockToday(LocalDate.of(2026, 5, 26));
        when(invoiceRepository.findByStatusAndClosingDateLessThanEqual(
                InvoiceStatus.OPEN, LocalDate.of(2026, 5, 26)))
                .thenReturn(List.of(openInvoice));

        invoiceService.closeDueInvoices();

        assertEquals(InvoiceStatus.CLOSED, openInvoice.getStatus());
        assertNotNull(openInvoice.getClosedAt());
        verify(invoiceRepository).saveAll(List.of(openInvoice));
    }

    @Test
    void payInvoice_DeveQuitarECreditarLimite_QuandoPagamentoIntegral() {
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));
        when(userFacade.findById(10L)).thenReturn(userInfo);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        invoiceService.payInvoice(51L, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("100.00"), closedInvoice.getPaidAmount());
        assertEquals(InvoiceStatus.PAID, closedInvoice.getStatus());
        assertNotNull(closedInvoice.getPaidAt());
        // limite restaurado
        assertEquals(new BigDecimal("800.00"), card.getAvailableLimit());
        verify(eventPublisher).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void payInvoice_DevePagarParcialmente_SemMudarStatus() {
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        invoiceService.payInvoice(51L, new BigDecimal("40.00"));

        assertEquals(new BigDecimal("40.00"), closedInvoice.getPaidAmount());
        // ainda CLOSED — não quitou
        assertEquals(InvoiceStatus.CLOSED, closedInvoice.getStatus());
        assertEquals(new BigDecimal("740.00"), card.getAvailableLimit());
        verify(eventPublisher, never()).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void payInvoice_DeveDesbloquearContaECartao_QuandoQuitaOverdue() {
        account.setStatus(AccountStatus.RESTRICTED);
        card.setStatus(CreditCardStatus.BLOCKED);
        when(invoiceRepository.findById(52L)).thenReturn(Optional.of(overdueInvoice));
        when(userFacade.findById(10L)).thenReturn(userInfo);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        invoiceService.payInvoice(52L, new BigDecimal("200.00"));

        assertEquals(InvoiceStatus.PAID, overdueInvoice.getStatus());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(CreditCardStatus.ACTIVE, card.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void payInvoice_DeveLancarException_QuandoExcedeSaldoDevedor() {
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));

        assertThrows(
                IllegalArgumentException.class,
                () -> invoiceService.payInvoice(51L, new BigDecimal("999.00"))
        );
        verify(accountService, never()).debitForInvoicePayment(any(), any(), any(), any());
    }

    @Test
    void payInvoice_DeveLancarException_QuandoStatusInvalido() {
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(openInvoice));

        assertThrows(
                InvoiceNotPayableException.class,
                () -> invoiceService.payInvoice(50L, new BigDecimal("100.00"))
        );
    }

    @Test
    void chargeDueInvoices_DeveCobrarEMarcarPaid_QuandoSaldoSuficiente() {
        mockClockToday(LocalDate.of(2026, 5, 11));
        when(invoiceRepository.findByStatusAndDueDateLessThanEqual(
                InvoiceStatus.CLOSED, LocalDate.of(2026, 5, 11)))
                .thenReturn(List.of(closedInvoice));
        // mocks pra payInvoice (chamado internamente)
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));
        when(userFacade.findById(10L)).thenReturn(userInfo);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        invoiceService.chargeDueInvoices();

        assertEquals(InvoiceStatus.PAID, closedInvoice.getStatus());
        verify(eventPublisher).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void chargeDueInvoices_DeveMarcarOverdue_QuandoSaldoInsuficiente() {
        mockClockToday(LocalDate.of(2026, 5, 11));
        when(invoiceRepository.findByStatusAndDueDateLessThanEqual(
                InvoiceStatus.CLOSED, LocalDate.of(2026, 5, 11)))
                .thenReturn(List.of(closedInvoice));
        when(invoiceRepository.findById(51L)).thenReturn(Optional.of(closedInvoice));
        when(userFacade.findById(10L)).thenReturn(userInfo);
        // simula saldo insuficiente
        doThrow(new InsufficientBalanceException(BigDecimal.ZERO, new BigDecimal("100.00")))
                .when(accountService).debitForInvoicePayment(any(), any(), any(), any());

        invoiceService.chargeDueInvoices();

        assertEquals(InvoiceStatus.OVERDUE, closedInvoice.getStatus());
        assertEquals(AccountStatus.RESTRICTED, account.getStatus());
        assertEquals(CreditCardStatus.BLOCKED, card.getStatus());
        verify(eventPublisher).publishEvent(any(InvoiceOverdueEvent.class));
    }
}