package br.accenture.ProjetoFinalAccentureGrupo1.banking.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;


// André Vinícius Barros Macambira
public record InvoicePaidEvent(
        Long invoiceId,
        Long userId,
        String customerName,
        String customerEmail,
        BigDecimal amountPaid,
        YearMonth referenceMonth,
        Instant paidAt
) {}
