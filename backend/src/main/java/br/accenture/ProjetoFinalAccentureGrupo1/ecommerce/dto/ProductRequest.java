package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        int initialStock,
        String categoryName
) {}
