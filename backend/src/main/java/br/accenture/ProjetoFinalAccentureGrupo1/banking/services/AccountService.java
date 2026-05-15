package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Transaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.TransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotActiveException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountRestrictedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvalidAmountException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.TransactionRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.utils.AccountNumberGenerator;
import br.accenture.ProjetoFinalAccentureGrupo1.shared.security.AESEncryptionService;
import br.accenture.ProjetoFinalAccentureGrupo1.shared.security.EncryptionService;
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

    private final UserFacade userFacade;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AESEncryptionService encryptionService;

    @Transactional
    public Account createForUser(Long userId, String password) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> createCustomerAccount(userId, password));
    }

    @Transactional
    public Account createMerchantAccountIfMissing() {
        return accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .accountNumber(generateUniqueAccountNumber())
                        .password("0000")
                        .balance(MERCHANT_INITIAL_BALANCE)
                        .accountType(AccountType.MERCHANT)
                        .status(AccountStatus.ACTIVE)
                        .build()));
    }

    @Transactional(readOnly = true)
    public AccountResponse findMyAccount(String email) {
        return toAccountResponse(findAccountByUserEmail(email));
    }

    public Account findByIdAdmin(Long id){
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
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
    public Account debit(Account account, BigDecimal amount, String reference, String description, TransactionType type) {
        validatePositiveAmount(amount);
        ensureCanDebit(account);
        ensureSufficientBalance(account, amount);
        debitAccount(account, amount, reference, description, type);
        return accountRepository.save(account);
    }

    @Transactional
    public Account credit(Account account, BigDecimal amount, String reference, String description, TransactionType type) {
        validatePositiveAmount(amount);
        ensureNotBlocked(account);
        creditAccount(account, amount, reference, description, type);
        return accountRepository.save(account);
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
    public void refund(Long toUserId, BigDecimal amount, String reference, String description) {
        Account merchant = accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseThrow(() -> new IllegalStateException(
                        "Conta MERCHANT não encontrada — CompanyAccountInitializer não rodou!"
                ));

        Account customer = findByUserId(toUserId);
        debit(merchant, amount, reference, description, TransactionType.REFUND);
        credit(customer, amount, reference, description, TransactionType.REFUND);
    }

    @Transactional
    public void cashback(Long toUserId, BigDecimal amount, String reference, String description) {
        Account merchant = accountRepository.findFirstByAccountType(AccountType.MERCHANT)
                .orElseThrow(() -> new IllegalStateException(
                        "Conta MERCHANT não encontrada — CompanyAccountInitializer não rodou!"
                ));

        Account customer = findByUserId(toUserId);
        debit(merchant, amount, reference, description, TransactionType.CASHBACK);
        credit(customer, amount, reference, description, TransactionType.CASHBACK);
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
    public void debitForInvoicePayment(Account account, BigDecimal amount, String reference, String description) {
        validatePositiveAmount(amount);
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountNotActiveException(account.getStatus());
        }
        ensureSufficientBalance(account, amount);
        debitAccount(account, amount, reference, description, TransactionType.PAYMENT);
        accountRepository.save(account);
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

    @Transactional(readOnly = true)
    public List<AccountResponse> findAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional
    public AccountResponse adminDeposit(Long accountId, BigDecimal amount, String description) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        validatePositiveAmount(amount);
        ensureNotBlocked(account); 
        
        String finalDescription = (description != null && !description.isBlank()) 
                ? description 
                : "Depósito administrativo";
                
        creditAccount(account, amount, null, finalDescription, TransactionType.DEPOSIT);
        return toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse blockAccount(Long accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        account.setStatus(AccountStatus.BLOCKED);
        return toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse unBlockAccount(Long accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        account.setStatus(AccountStatus.ACTIVE);
        return toAccountResponse(accountRepository.save(account));
    }

    private Account  createCustomerAccount(Long userId, String password) {
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(generateUniqueAccountNumber())
                .password(encryptionService.encrypt(password))
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        return accountRepository.save(account);
    }

    public Account findAccountByUserEmail(String email) {
        UserInfo user = userFacade.findByEmail(email);
        return findByUserId(user.id());
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
