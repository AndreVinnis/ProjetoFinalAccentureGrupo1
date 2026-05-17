package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.DepositRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.TransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banking/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BANKING_ADMIN')")
public class AdminAccountController {

    private final AccountService accountService;
    private final CreditCardService creditCardService;
    private final TransactionService transactionService;
    private final InvoiceService invoiceService;
    private final BillingScheduler billingScheduler;

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.findAllAccounts();
    }

    @GetMapping("/accounts/transactions")
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.findAll();
    }

    @GetMapping("/billing/invoices/open")
    public List<InvoiceResponse> getOpenInvoices() {
        return invoiceService.listOpenInvoicesForAdmin();
    }

    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(
            @PathVariable Long id, 
            @RequestBody DepositRequest request) {
        return accountService.adminDeposit(id, request.amount(), request.description());
    }

    @PostMapping("/accounts/{id}/block")
    public AccountResponse blockAccount(@PathVariable Long id) {
        Account account = accountService.findByIdAdmin(id);
        creditCardService.blockCardByAccount(account);
        return accountService.blockAccount(id);
    }

    @PostMapping("/accounts/{id}/unblock")
    public AccountResponse unBlockAccount(@PathVariable Long id) {
        Account account = accountService.findByIdAdmin(id);
        creditCardService.unblockCardByAccount(account);
        return accountService.unBlockAccount(id);
    }

    @PostMapping("/billing/invoices/{id}/close")
    public ResponseEntity<Void> closeInvoice(@PathVariable Long id){
        invoiceService.closeInvoice(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/billing/charge-overdue")
    public ResponseEntity<Void> chargeInvoices(){
        invoiceService.chargeDueInvoices();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/billing/run-day")
    public ResponseEntity<Void> runDayBilling(){
        billingScheduler.runDailyBilling();
        return ResponseEntity.ok().build();
    }
}
