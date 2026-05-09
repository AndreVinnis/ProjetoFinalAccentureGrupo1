package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

//    public static final String MERCHANT_ACCOUNT_NUMBER = "MERCHANT-001";
//
//    private final AccountRepository accountRepository;
//    private final AccountHolderRepository accountHolderRepository;
//    private final UserRepository userRepository;
//
//    @Transactional
//    public void createCustomerBankingProfile(UserRegisteredEvent event) {
//        if (accountRepository.findByHolder_UserId(event.userId()).isPresent()) {
//            return;
//        }
//
//        AccountHolder holder = new AccountHolder();
//        holder.setName(event.name());
//        holder.setDocument(event.cpf());
//        holder.setUserId(event.userId());
//        accountHolderRepository.save(holder);
//
//        Account account = new Account();
//        account.setHolder(holder);
//        account.setAccountNumber("CUST-" + event.userId());
//        account.setBalance(BigDecimal.ZERO);
//        account.setAccountType(AccountType.CUSTOMER);
//        accountRepository.save(account);
//    }
//
//    @Transactional(readOnly = true)
//    public BigDecimal getBalanceByUserEmail(String email) {
//        Long userId = userRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"))
//                .getId();
//        return getBalanceByUserId(userId);
//    }
//
//    @Transactional(readOnly = true)
//    public BigDecimal getBalanceByUserId(Long userId) {
//        Account account = accountRepository
//                .findByHolder_UserId(userId)
//                .orElseThrow(() -> new BankAccountNotFoundException("Conta não encontrada para o usuário informado"));
//        return account.getBalance();
//    }
//
//    @Transactional(readOnly = true)
//    public BigDecimal getBalanceByHolderId(Long holderId) {
//        Account account = accountRepository
//                .findByHolder_Id(holderId)
//                .orElseThrow(() -> new BankAccountNotFoundException("Conta não encontrada para o titular informado"));
//        return account.getBalance();
//    }
//
//    /**
//     * Crédito na conta do lojista após venda aprovada no cartão de crédito virtual.
//     */
//    @Transactional
//    public void creditMerchantForCreditCardSale(BigDecimal amount) {
//        Account merchant = accountRepository
//                .findByAccountNumber(MERCHANT_ACCOUNT_NUMBER)
//                .orElseThrow(() -> new BankAccountNotFoundException("Conta do lojista não configurada"));
//        merchant.setBalance(merchant.getBalance().add(amount));
//        accountRepository.save(merchant);
//    }
//
//    @Transactional
//    public void debitCustomerAccount(Long userId, BigDecimal amount) {
//        Account account = accountRepository
//                .findByHolder_UserId(userId)
//                .orElseThrow(() -> new BankAccountNotFoundException("Conta não encontrada para o usuário informado"));
//        if (account.getBalance().compareTo(amount) < 0) {
//            throw new BankAccountInsufficientFundsException("Saldo insuficiente na conta");
//        }
//        account.setBalance(account.getBalance().subtract(amount));
//        accountRepository.save(account);
//    }
//
//    @Transactional
//    public void creditCustomerAccount(Long userId, BigDecimal amount) {
//        Account account = accountRepository
//                .findByHolder_UserId(userId)
//                .orElseThrow(() -> new BankAccountNotFoundException("Conta não encontrada para o usuário informado"));
//        account.setBalance(account.getBalance().add(amount));
//        accountRepository.save(account);
//    }
}
