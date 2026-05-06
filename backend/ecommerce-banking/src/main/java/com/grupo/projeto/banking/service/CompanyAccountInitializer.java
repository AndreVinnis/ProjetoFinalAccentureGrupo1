package com.grupo.projeto.banking.service;

import com.grupo.projeto.banking.domain.Account;
import com.grupo.projeto.banking.domain.AccountType;
import com.grupo.projeto.banking.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CompanyAccountInitializer implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.findByAccountNumber("MERCHANT-001").isEmpty()) {
            Account merchant = new Account();
            merchant.setAccountNumber("MERCHANT-001");
            merchant.setBalance(new BigDecimal("10000000.00")); // R$ 10 Milhões
            merchant.setAccountType(AccountType.MERCHANT);
            
            accountRepository.save(merchant);
            System.out.println("=== Conta MERCHANT criada com sucesso! ===");
        }
    }
}