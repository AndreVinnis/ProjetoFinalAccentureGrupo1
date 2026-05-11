package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardPurchaseResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditLimitResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banking/cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CreditCardResponse> findMyCard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditCardService.findMyCard(userDetails.getUsername()));
    }

    @GetMapping("/me/limit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CreditLimitResponse> findMyLimit(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditCardService.findMyLimit(userDetails.getUsername()));
    }

    @GetMapping("/me/purchases")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<CardPurchaseResponse>> findMyPurchases(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(creditCardService.findMyPurchases(userDetails.getUsername()));
    }

}
