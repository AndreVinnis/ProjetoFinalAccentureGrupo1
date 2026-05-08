package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PayInvoiceRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banking/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final CreditCardService creditCardService;
    private final InvoiceService invoiceService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<InvoiceResponse> findCurrentInvoice(@AuthenticationPrincipal UserDetails userDetails) {
        CreditCard card = creditCardService.findMyCardEntity(userDetails.getUsername());
        return ResponseEntity.ok(invoiceService.getCurrentInvoice(card));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@AuthenticationPrincipal UserDetails userDetails) {
        CreditCard card = creditCardService.findMyCardEntity(userDetails.getUsername());
        return ResponseEntity.ok(invoiceService.listByCard(card));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PayInvoiceRequest request
    ) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, userDetails.getUsername(), request.amount()));
    }
}
