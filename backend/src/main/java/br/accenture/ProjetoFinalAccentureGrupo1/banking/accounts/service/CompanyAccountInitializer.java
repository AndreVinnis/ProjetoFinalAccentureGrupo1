package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.service;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyAccountInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.findByAccountNumber(AccountService.MERCHANT_ACCOUNT_NUMBER).isEmpty()) {
            Account merchant = new Account();
            merchant.setAccountNumber(AccountService.MERCHANT_ACCOUNT_NUMBER);
            merchant.setBalance(new BigDecimal("10000000.00"));
            merchant.setAccountType(AccountType.MERCHANT);
            accountRepository.save(merchant);
            log.info("Conta MERCHANT-001 criada com sucesso");
        }
    }
}
