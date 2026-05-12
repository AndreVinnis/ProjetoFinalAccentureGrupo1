package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CustomerResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCustomerRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
//Autor: Cainã Moura Araújo
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponse> findMyCustomer(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerService.findMyCustomer(userDetails.getUsername()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponse> updateMyCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(customerService.updateMyCustomer(
                userDetails.getUsername(),
                request.shippingAddress(),
                request.phone()
        ));
    }
}
