package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

//    private final AccountService accountService;
//
//    @GetMapping("/me/balance")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public BigDecimal getMyBalance(@AuthenticationPrincipal UserDetails userDetails) {
//        return accountService.getBalanceByUserEmail(userDetails.getUsername());
//    }
//
//    /**
//     * Consulta por id do titular — restrita a administradores.
//     */
//    @GetMapping("/{id}/balance")
//    @PreAuthorize("hasAnyRole('BANKING_ADMIN', 'ECOMMERCE_ADMIN')")
//    public BigDecimal getBalanceByHolderId(@PathVariable Long id) {
//        return accountService.getBalanceByHolderId(id);
//    }
}
