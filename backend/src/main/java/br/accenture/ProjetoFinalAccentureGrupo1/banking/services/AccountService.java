package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Transaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.TransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotActiveException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountRestrictedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvalidAmountException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final BigDecimal MERCHANT_INITIAL_BALANCE = new BigDecimal("10000000.00");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public Account createForUser(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> createCustomerAccount(userId));
    }

    @Transactional
    public Account createAccountForUser(Long userId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }
        return createCustomerAccount(userId);
    }

    @Transactional
    public Account createMerchantAccountIfMissing() {
        return accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .accountNumber(generateUniqueAccountNumber())
                        .balance(MERCHANT_INITIAL_BALANCE)
                        .accountType(AccountType.MERCHANT)
                        .status(AccountStatus.ACTIVE)
                        .build()));
    }

    @Transactional(readOnly = true)
    public AccountResponse findMyAccount(String email) {
        return toAccountResponse(findAccountByUserEmail(email));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return findByUserId(userId).getBalance();
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalanceByEmail(String email) {
        return findAccountByUserEmail(email).getBalance();
    }

    @Transactional
    public AccountResponse deposit(String email, BigDecimal amount, String description) {
        Account account = findAccountByUserEmail(email);
        validatePositiveAmount(amount);
        ensureNotBlocked(account);
        creditAccount(account, amount, null, description, TransactionType.DEPOSIT);
        return toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public Account debit(Account account, BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureCanDebit(account);
        ensureSufficientBalance(account, amount);
        debitAccount(account, amount, null, "Debito em conta", TransactionType.PAYMENT);
        return accountRepository.save(account);
    }

    @Transactional
    public void debit(Long userId, BigDecimal amount, String reference) {
        Account account = findByUserId(userId);
        validatePositiveAmount(amount);
        ensureCanDebit(account);
        ensureSufficientBalance(account, amount);
        debitAccount(account, amount, reference, "Debito em conta", TransactionType.PAYMENT);
        accountRepository.save(account);
    }

    @Transactional
    public void debitInvoicePayment(Long userId, BigDecimal amount, String reference, String description) {
        Account account = findByUserId(userId);
        validatePositiveAmount(amount);
        ensureCanDebit(account);
        ensureSufficientBalance(account, amount);
        debitAccount(account, amount, reference, description, TransactionType.PAYMENT);
        accountRepository.save(account);
    }

    @Transactional
    public Account credit(Account account, BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureNotBlocked(account);
        creditAccount(account, amount, null, "Credito em conta", TransactionType.REFUND);
        return accountRepository.save(account);
    }

    @Transactional
    public void credit(Long userId, BigDecimal amount, String reference) {
        Account account = findByUserId(userId);
        validatePositiveAmount(amount);
        ensureNotBlocked(account);
        creditAccount(account, amount, reference, "Credito em conta", TransactionType.REFUND);
        accountRepository.save(account);
    }

    @Transactional
    public void creditMerchant(BigDecimal amount, String reference, String description) {
        Account merchant = accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseGet(this::createMerchantAccountIfMissing);
        validatePositiveAmount(amount);
        ensureNotBlocked(merchant);
        creditAccount(merchant, amount, reference, description, TransactionType.PAYMENT);
        accountRepository.save(merchant);
    }

    @Transactional
    public void creditMerchantForCreditCardSale(BigDecimal amount) {
        creditMerchant(amount, null, "Venda aprovada no cartao de credito");
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listMyTransactions(String email) {
        Account account = findAccountByUserEmail(email);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Account findByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    public Account findAccountByUserId(Long userId) {
        return findByUserId(userId);
    }

    private Account createCustomerAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + userId));

        Account account = Account.builder()
                .userId(user.getId())
                .accountNumber(generateUniqueAccountNumber())
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        return accountRepository.save(account);
    }

    private Account findAccountByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
        return findByUserId(user.getId());
    }

    private void creditAccount(Account account, BigDecimal amount, String reference, String description, TransactionType type) {
        account.setBalance(account.getBalance().add(amount));
        saveTransaction(account, amount, reference, description, type);
    }

    private void debitAccount(Account account, BigDecimal amount, String reference, String description, TransactionType type) {
        account.setBalance(account.getBalance().subtract(amount));
        saveTransaction(account, amount, reference, description, type);
    }

    private void saveTransaction(Account account, BigDecimal amount, String reference, String description, TransactionType type) {
        transactionRepository.save(Transaction.builder()
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .reference(reference)
                .description(description)
                .build());
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }
    }

    private void ensureNotBlocked(Account account) {
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException();
        }
    }

    private void ensureCanDebit(Account account) {
        ensureNotBlocked(account);
        if (account.getStatus() == AccountStatus.RESTRICTED) {
            throw new AccountRestrictedException();
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(account.getStatus());
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getBalance(), amount);
        }
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = accountNumberGenerator.generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getAccountType(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
