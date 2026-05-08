package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.service;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.api.BankingFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingFacadeImpl implements BankingFacade {

    private final AccountService accountService;

    @Override
    public BigDecimal getBalance(Long userId) {
        return accountService.getBalanceByUserId(userId);
    }

    /**
     * Débito em conta corrente do cliente (não confundir com fatura do cartão de crédito virtual).
     */
    @Override
    public void chargeCard(Long userId, BigDecimal amount, String description, String reference) {
        accountService.debitCustomerAccount(userId, amount);
        log.debug("Débito em conta userId={} valor={} ref={} ({})", userId, amount, reference, description);
    }

    @Override
    public void issueRefund(Long toUserId, BigDecimal amount, String description) {
        accountService.creditCustomerAccount(toUserId, amount);
        log.debug("Crédito (estorno) userId={} valor= {} ({})", toUserId, amount, description);
    }
}
