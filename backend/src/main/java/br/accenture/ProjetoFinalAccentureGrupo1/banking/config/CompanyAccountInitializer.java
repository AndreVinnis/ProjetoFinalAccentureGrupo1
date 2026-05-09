package br.accenture.ProjetoFinalAccentureGrupo1.banking.config;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
/*
 * Garante que existe uma conta MERCHANT (da empresa) ao subir a aplicação.
 * Idempotente — não cria se já existir.
 * Autor: André Vinícius Barros Macambira
 */
public class CompanyAccountInitializer {

    private static final String COMPANY_ACCOUNT_NUMBER = "00000-0";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000000.00");

    private final AccountRepository accountRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureCompanyAccountExists() {
        if (accountRepository.findFirstByAccountType(AccountType.MERCHANT).isPresent()) {
            log.info("Conta da empresa já existe — pulando inicialização.");
            return;
        }

        Account merchant = Account.builder()
                .accountNumber(COMPANY_ACCOUNT_NUMBER)
                .balance(INITIAL_BALANCE)
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(merchant);
        log.info("Conta MERCHANT criada (número: {}, saldo inicial: R$ {}).",
                COMPANY_ACCOUNT_NUMBER, INITIAL_BALANCE);
    }
}
