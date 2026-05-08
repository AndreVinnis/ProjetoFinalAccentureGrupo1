package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

public record InvoiceResponse(
        Long id,
        YearMonth referenceMonth,
        LocalDate closingDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        InvoiceStatus status,
        Instant closedAt,
        Instant paidAt
) {}
