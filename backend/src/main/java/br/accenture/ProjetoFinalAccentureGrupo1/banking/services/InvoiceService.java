package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvalidAmountException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvalidInvoicePaymentException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvoiceNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public Invoice getOrCreateOpenInvoice(CreditCard card) {
        return invoiceRepository.findByCardIdAndStatus(card.getId(), InvoiceStatus.OPEN)
                .orElseGet(() -> invoiceRepository.save(newOpenInvoiceForNextAvailableMonth(card)));
    }

    @Transactional
    public InvoiceResponse getCurrentInvoice(CreditCard card) {
        return toInvoiceResponse(getOrCreateOpenInvoice(card));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByCard(CreditCard card) {
        return invoiceRepository.findByCardIdOrderByReferenceMonthDesc(card.getId())
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    @Transactional
    public InvoiceResponse payInvoice(Long invoiceId, String userEmail, BigDecimal amount) {
        validatePositiveAmount(amount);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + userEmail));
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        CreditCard card = invoice.getCard();

        if (!card.getUser().getId().equals(user.getId())) {
            throw new InvalidInvoicePaymentException("Fatura nao pertence ao usuario autenticado");
        }
        if (invoice.getStatus() == InvoiceStatus.OPEN) {
            throw new InvalidInvoicePaymentException("Fatura aberta ainda nao pode ser paga");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoicePaymentException("Fatura ja esta paga");
        }

        BigDecimal remainingAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInvoicePaymentException("Fatura nao possui saldo em aberto");
        }
        if (amount.compareTo(remainingAmount) > 0) {
            throw new InvalidInvoicePaymentException("Valor de pagamento maior que o saldo da fatura");
        }

        String reference = "INVOICE-" + invoice.getId();
        accountService.debitInvoicePayment(user.getId(), amount, reference, "Pagamento de fatura");

        invoice.setPaidAmount(invoice.getPaidAmount().add(amount));
        restoreAvailableLimit(card, amount);

        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            Instant paidAt = Instant.now(clock);
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(paidAt);
            eventPublisher.publishEvent(new InvoicePaidEvent(
                    invoice.getId(),
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    invoice.getPaidAmount(),
                    invoice.getReferenceMonth(),
                    paidAt
            ));
        }

        creditCardRepository.save(card);
        return toInvoiceResponse(invoiceRepository.save(invoice));
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

    private void restoreAvailableLimit(CreditCard card, BigDecimal amount) {
        BigDecimal restoredLimit = card.getAvailableLimit().add(amount);
        if (restoredLimit.compareTo(card.getCreditLimit()) > 0) {
            restoredLimit = card.getCreditLimit();
        }
        card.setAvailableLimit(restoredLimit);
        card.setInvoiceBalance(card.getInvoiceBalance().subtract(amount));
        if (card.getInvoiceBalance().compareTo(BigDecimal.ZERO) < 0) {
            card.setInvoiceBalance(BigDecimal.ZERO);
        }
    }

    private LocalDate safeDate(YearMonth month, Integer day, int defaultDay) {
        int requestedDay = day == null || day == 0 ? defaultDay : day;
        int validDay = Math.max(1, Math.min(requestedDay, month.lengthOfMonth()));
        return month.atDay(validDay);
    }
}
