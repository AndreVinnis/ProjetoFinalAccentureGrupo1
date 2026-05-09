package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PayPixRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PaymentRequestResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.PixService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/banking/pix")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
// Autor: André Vinícius Barros Macambira
public class PixController {

    private final PixService pixService;

    @GetMapping("/{code}")
    public ResponseEntity<PaymentRequestResponse> getByCode(@PathVariable String code) {
        PaymentRequest request = pixService.getByCode(code);
        return ResponseEntity.ok(PaymentRequestResponse.from(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentRequestResponse> pay(
            @RequestBody @Valid PayPixRequest body,
            Authentication authentication
    ) {
        String payerEmail = authentication.getName();
        PaymentRequest paid = pixService.payByCode(body.code(), payerEmail);
        return ResponseEntity.ok(PaymentRequestResponse.from(paid));
    }
}
