package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int availableStock, // Retornamos apenas o que o cliente pode comprar
        Long categoryId,
        String categoryName
) {}
