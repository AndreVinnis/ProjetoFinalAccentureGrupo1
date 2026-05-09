package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.BalanceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.DepositRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.TransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banking/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountResponse> findMyAccount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(accountService.findMyAccount(userDetails.getUsername()));
    }

    @GetMapping("/me/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BalanceResponse> findMyBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(new BalanceResponse(accountService.getBalanceByEmail(userDetails.getUsername())));
    }

    @PostMapping("/me/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountResponse> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid DepositRequest request
    ) {
        return ResponseEntity.ok(accountService.deposit(userDetails.getUsername(), request.amount(), request.description()));
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TransactionResponse>> findMyTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(accountService.listMyTransactions(userDetails.getUsername()));
    }
}
