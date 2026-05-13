package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.RegisterSavedCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.SavedCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.SavedCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ecommerce/cards")
@RequiredArgsConstructor
public class SavedCardController {

    private final SavedCardService savedCardService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SavedCardResponse> registerCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid RegisterSavedCardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedCardService.registerCard(userDetails.getUsername(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SavedCardResponse>> listMyCards(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(savedCardService.listMyCards(userDetails.getUsername()));
    }

    @DeleteMapping("/{savedCardId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long savedCardId
    ) {
        savedCardService.delete(userDetails.getUsername(), savedCardId);
        return ResponseEntity.noContent().build();
    }
}
