package br.accenture.ProjetoFinalAccentureGrupo1.banking.service;

import com.grupo.projeto.banking.api.BankingFacade;
import com.grupo.projeto.banking.service.AccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class BankingFacadeImpl implements BankingFacade {

    @Autowired
    private AccountService accountService;

    @Override
    public BigDecimal getBalance(Long userId) {
        return accountService.getBalance(userId);
    }

    @Override
    public void chargeCard(Long userId, BigDecimal amount, String description, String reference) {
        System.out.println("Cobrando R$ " + amount + " do usuário " + userId);
    }

    @Override
    public void issueRefund(Long toUserId, BigDecimal amount, String description) {
        System.out.println("Estornando R$ " + amount + " para o usuário " + toUserId);
    }
}