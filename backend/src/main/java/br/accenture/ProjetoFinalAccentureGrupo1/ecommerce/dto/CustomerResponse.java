package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;

import java.time.Instant;
//Autor: Cainã Moura Araújo
public record CustomerResponse(
        Long id,
        Long userId,
        int quantityPurchases,
        CustomerTier tier,
        String shippingAddress,
        String phone,
        Instant createdAt
) {}
