package br.accenture.ProjetoFinalAccentureGrupo1.banking.service;

import com.grupo.projeto.banking.api.BankingFacade;
import com.grupo.projeto.banking.domain.Account;
import com.grupo.projeto.banking.domain.Transaction;
import com.grupo.projeto.banking.repository.AccountRepository;
import com.grupo.projeto.banking.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BankingFacadeImpl implements BankingFacade {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public BigDecimal getBalance(Long userId) {
        return accountRepository.findById(userId)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void chargeCard(Long userId, BigDecimal amount, String description, String reference) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setDescription(description + " - " + reference);
        tx.setValue(amount.negate());
        tx.setTimestamp(LocalDateTime.now());
        tx.setAccount(account);
        
        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void issueRefund(Long toUserId, BigDecimal amount, String description) {
        Account account = accountRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setDescription("ESTORNO: " + description);
        tx.setValue(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setAccount(account);
        
        transactionRepository.save(tx);
    }
}