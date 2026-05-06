package com.grupo.projeto.banking.service;

import com.grupo.projeto.banking.domain.Account;
import com.grupo.projeto.banking.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public BigDecimal getBalance(Long id) {
        Account account = accountRepository.findByHolderId(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        return account.getBalance();
    }
}