package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Transaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.DepositRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banking/admin/accounts")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('BANKING_ADMIN')")
public class AdminAccountController {

    private final AccountService accountService;
    private final TransactionService transactionService; // <-- Injetando o novo serviço

    // GET /banking/admin/accounts - Lista todas as contas
    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.findAllAccounts();
    }

    // GET /banking/admin/accounts/transactions - Lista TODAS as transações do sistema (Novo endpoint do áudio)
    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return transactionService.findAll();
    }

    // POST /banking/admin/accounts/{id}/deposit - Depósito manual
    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(
            @PathVariable Long id, 
            @RequestBody DepositRequest request) {
        return accountService.adminDeposit(id, request.amount(), request.description());
    }

    // POST /banking/admin/accounts/{id}/block - Bloqueio manual
    @PostMapping("/{id}/block")
    public AccountResponse blockAccount(@PathVariable Long id) {
        return accountService.blockAccount(id);
    }
}