package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CheckoutCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.OrderResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ecommerce/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> findMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.findMyOrders(userDetails.getUsername()));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> findMyOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.findMyOrder(orderId, userDetails.getUsername()));
    }

    @PostMapping("/checkout/pix")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> checkoutPix(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.checkoutPix(userDetails.getUsername()));
    }

    @PostMapping("/checkout/card")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Object> checkoutCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CheckoutCardRequest request
    ) {
        return ResponseEntity.ok(orderService.checkoutCard(userDetails.getUsername(), request.savedCardId(), request.cvv()));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId
    ) {
        orderService.cancel(orderId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
