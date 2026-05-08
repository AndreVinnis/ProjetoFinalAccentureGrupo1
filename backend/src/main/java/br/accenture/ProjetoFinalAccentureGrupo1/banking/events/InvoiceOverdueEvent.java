package br.accenture.ProjetoFinalAccentureGrupo1.banking.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/*
 * Publicado quando uma fatura vence sem pagamento.
 * Causa: conta do cliente passa para RESTRICTED, cartão para BLOCKED.
 *
 * Autor: André Vinícius Barros Macambira
 */
public record InvoiceOverdueEvent(
        Long invoiceId,
        Long userId,
        String customerName,
        String customerEmail,
        BigDecimal amount,
        LocalDate dueDate,
        YearMonth referenceMonth
) {}
