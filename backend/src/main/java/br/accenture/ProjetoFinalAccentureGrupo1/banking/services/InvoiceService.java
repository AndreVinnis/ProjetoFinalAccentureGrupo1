package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CardPurchase;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoiceOverdueEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.*;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final UserFacade userFacade;
    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public Invoice getOrCreateOpenInvoice(CreditCard card) {
        return invoiceRepository.findByCardIdAndStatus(card.getId(), InvoiceStatus.OPEN)
                .orElseGet(() -> invoiceRepository.save(newOpenInvoiceForNextAvailableMonth(card)));
    }

    @Transactional
    public InvoiceResponse getCurrentInvoice(String email) {
        UserInfo user = userFacade.findByEmail(email);
        Account account = accountService.findByUserId(user.id());
        CreditCard card = creditCardRepository.findByAccount(account).orElseThrow(
                () -> new CardNotFoundException(0L));
        Invoice invoice = getOrCreateOpenInvoice(card);
        return  toInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByCard(String email) {
        UserInfo user = userFacade.findByEmail(email);
        Account account = accountService.findByUserId(user.id());
        CreditCard card = creditCardRepository.findByAccount(account).orElseThrow(
                () -> new CardNotFoundException(0L));
        return invoiceRepository.findByCardIdOrderByReferenceMonthDesc(card.getId())
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listOpenInvoicesForAdmin() {
        return invoiceRepository.findByStatusOrderByClosingDateAsc(InvoiceStatus.OPEN)
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    @Transactional
    public void addCardPurchase(Invoice invoice, CardPurchase cardPurchase){
        invoice.setTotalAmount(invoice.getTotalAmount().add(cardPurchase.getAmount()));
        invoiceRepository.save(invoice);
    }

    /*
     * Fecha uma fatura específica. Marca como CLOSED e registra o instante.
     * Idempotente apenas pra OPEN — se já estiver fechada/paga, lança exception.
     */
    @Transactional
    public Invoice closeInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new InvoiceNotCloseableException(invoiceId, invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.CLOSED);
        invoice.setClosedAt(Instant.now());
        return invoiceRepository.save(invoice);
    }

    /**
     * Fecha todas as faturas OPEN cujo closingDate <= hoje.
     * Idempotente — rodar várias vezes no mesmo dia é seguro.
     */
    @Transactional
    public int closeDueInvoices() {
        LocalDate today = LocalDate.now(clock);

        List<Invoice> toClose = invoiceRepository.findByStatusAndClosingDateLessThanEqual(InvoiceStatus.OPEN, today);

        Instant now = Instant.now();
        for (Invoice invoice : toClose) {
            invoice.setStatus(InvoiceStatus.CLOSED);
            invoice.setClosedAt(now);
        }
        invoiceRepository.saveAll(toClose);
        return toClose.size();
    }

    @Transactional
    public InvoiceResponse payInvoice(Long invoiceId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        validatePositiveAmount(amount);

        if (invoice.getStatus() != InvoiceStatus.CLOSED
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new InvoiceNotPayableException(invoiceId, invoice.getStatus());
        }

        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Valor (R$ " + amount + ") excede o saldo devedor (R$ " + remaining + ")"
            );
        }

        CreditCard card = invoice.getCard();
        Account customerAccount = card.getAccount();
        String reference = "INVOICE-" + invoiceId;
        String description = "Pagamento da fatura " + invoice.getReferenceMonth();

        // Debita do cliente (usando metodo que tolera RESTRICTED)
        accountService.debitForInvoicePayment(customerAccount, amount, reference, description);

        invoice.setPaidAmount(invoice.getPaidAmount().add(amount));
        card.setAvailableLimit(card.getAvailableLimit().add(amount));

        boolean fullyPaid = invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0;
        if (fullyPaid) {
            boolean wasOverdue = invoice.getStatus() == InvoiceStatus.OVERDUE;
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());

            if (wasOverdue) {
                customerAccount.setStatus(AccountStatus.ACTIVE);
                card.setStatus(CreditCardStatus.ACTIVE);
                accountRepository.save(customerAccount);
            }

            eventPublisher.publishEvent(new InvoicePaidEvent(
                    invoice.getId(),
                    customerAccount.getUserId(),
                    getCustomerName(customerAccount.getUserId()),
                    getCustomerEmail(customerAccount.getUserId()),
                    invoice.getPaidAmount(),
                    invoice.getReferenceMonth(),
                    invoice.getPaidAt()
            ));
        }

        creditCardRepository.save(card);
        invoiceRepository.save(invoice);
        return toInvoiceResponse(invoice);
    }

    /*
     * Cobra automaticamente todas as faturas CLOSED com dueDate <= hoje.
     * Para cada fatura, tenta debitar o saldo devedor:
     *   - sucesso → marca PAID, libera limite, publica InvoicePaidEvent
     *   - falha (saldo insuficiente) → marca OVERDUE, restringe conta, bloqueia cartão,
     *     publica InvoiceOverdueEvent
     *
     * Idempotente: rodar duas vezes no mesmo dia é seguro (faturas já PAID/OVERDUE são ignoradas).
     */
    @Transactional
    public int chargeDueInvoices() {
        LocalDate today = LocalDate.now(clock);

        List<Invoice> due = invoiceRepository.findByStatusAndDueDateLessThanEqual(
                InvoiceStatus.CLOSED, today
        );

        for (Invoice invoice : due) {
            chargeOneInvoice(invoice);
        }
        return due.size();
    }

    private void chargeOneInvoice(Invoice invoice) {
        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (remaining.signum() <= 0) {
            // Já estava paga, só não tinha mudado o status — corrige
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            return;
        }

        CreditCard card = invoice.getCard();
        Account account = card.getAccount();

        try {
            // Tenta cobrar o valor restante de uma vez
            payInvoice(invoice.getId(), remaining);
        } catch (InsufficientBalanceException ex) {
            markAsOverdue(invoice, account, card);
        }
    }

    private void markAsOverdue(Invoice invoice, Account account, CreditCard card) {
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);

        account.setStatus(AccountStatus.RESTRICTED);
        card.setStatus(CreditCardStatus.BLOCKED);
        accountRepository.save(account);
        creditCardRepository.save(card);

        eventPublisher.publishEvent(new InvoiceOverdueEvent(
                invoice.getId(),
                account.getUserId(),
                getCustomerName(account.getUserId()),
                getCustomerEmail(account.getUserId()),
                invoice.getTotalAmount().subtract(invoice.getPaidAmount()),
                invoice.getDueDate(),
                invoice.getReferenceMonth()
        ));
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getReferenceMonth(),
                invoice.getClosingDate(),
                invoice.getDueDate(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getStatus(),
                invoice.getClosedAt(),
                invoice.getPaidAt()
        );
    }

    private Invoice newOpenInvoiceForNextAvailableMonth(CreditCard card) {
        YearMonth referenceMonth = YearMonth.from(LocalDate.now(clock));
        while (invoiceRepository.findByCardIdAndReferenceMonth(card.getId(), referenceMonth).isPresent()) {
            referenceMonth = referenceMonth.plusMonths(1);
        }
        return newOpenInvoice(card, referenceMonth);
    }

    private Invoice newOpenInvoice(CreditCard card, YearMonth referenceMonth) {
        LocalDate closingDate = safeDate(referenceMonth, card.getClosingDay(), 25);
        LocalDate dueDate = safeDate(referenceMonth.plusMonths(1), card.getDueDay(), 10);

        return Invoice.builder()
                .card(card)
                .referenceMonth(referenceMonth)
                .closingDate(closingDate)
                .dueDate(dueDate)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.OPEN)
                .build();
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }
    }

    private String getCustomerName(Long userId) {
        return userFacade.findById(userId).name();
    }

    private String getCustomerEmail(Long userId) {
        return userFacade.findById(userId).email();
    }

    private LocalDate safeDate(YearMonth month, Integer day, int defaultDay) {
        int requestedDay = day == null || day == 0 ? defaultDay : day;
        int validDay = Math.max(1, Math.min(requestedDay, month.lengthOfMonth()));
        return month.atDay(validDay);
    }
}
