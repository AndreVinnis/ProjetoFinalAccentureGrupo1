package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotActiveException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
/* Autores:
 * Antônio Hortêncio Batista Rocha de Queiroga
 * André Vinícius Barros Macambira
 * Cainã Moura Araújo
 */
public class AccountService {

    public static final String MERCHANT_ACCOUNT_NUMBER = "MERCHANT-001";

    private final AccountRepository accountRepository;

    /*
     * Cria a conta corrente para um usuário recém-cadastrado.
     * Idempotente — se já existir conta, retorna a existente.
     * André Vinícius Barros Macambira
     */
    @Transactional
    public Account createForUser(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Account account = Account.builder()
                            .userId(userId)
                            .accountNumber(generateNextCustomerNumber())
                            .accountType(AccountType.CUSTOMER)
                            .status(AccountStatus.ACTIVE)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return accountRepository.save(account);
                });
    }

    // André Vinícius Barros Macambira
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return findByUserId(userId).getBalance();
    }

    // André Vinícius Barros Macambira
    @Transactional(readOnly = true)
    public Account findByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    // Debita um valor da conta. Exige status ACTIVE e saldo suficiente.
    // André Vinícius Barros Macambira
    @Transactional
    public Account debit(Account account, BigDecimal amount) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(account.getStatus());
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getBalance(), amount);
        }
        account.setBalance(account.getBalance().subtract(amount));
        return accountRepository.save(account);
    }


     // Credita um valor na conta. Permitido em ACTIVE e RESTRICTED (mas não em BLOCKED).
    @Transactional
    // André Vinícius Barros Macambira
    public Account credit(Account account, BigDecimal amount) {
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountNotActiveException(account.getStatus());
        }
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    // André Vinícius Barros Macambira
    private String generateNextCustomerNumber() {
        long nextSeq = accountRepository.countByAccountType(AccountType.CUSTOMER) + 1;
        return String.format("%05d-0", nextSeq);
    }

    /**
     * Crédito na conta do lojista após venda aprovada no cartão de crédito virtual.
     */
    @Transactional
    public void creditMerchantForCreditCardSale(BigDecimal amount) {
        Account merchant = accountRepository
                .findByAccountNumber(MERCHANT_ACCOUNT_NUMBER)
                .orElseThrow(() -> new AccountNotFoundException(1L));
        merchant.setBalance(merchant.getBalance().add(amount));
        accountRepository.save(merchant);
    }
}
