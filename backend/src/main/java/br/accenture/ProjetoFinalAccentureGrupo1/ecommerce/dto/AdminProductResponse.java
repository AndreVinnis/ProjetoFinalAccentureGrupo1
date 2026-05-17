package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int totalStock,
        int reservedStock,
        int availableStock,
        boolean active,
        Instant createdAt,
        Long categoryId,
        String categoryName
) {}
