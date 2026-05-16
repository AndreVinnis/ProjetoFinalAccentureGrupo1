package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserFacade userFacade;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private AESEncryptionService encryptionService;

    @InjectMocks
    private AccountService accountService;

    private Account active;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        active = Account.builder()
                .id(1L)
                .userId(10L)
                .accountNumber("00001-0")
                .balance(new BigDecimal("100.00"))
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        userInfo = new UserInfo(10L, "Ana", "ana@email.com",
                "12345678901", LocalDate.of(1990, 1, 1), Set.of(Role.CUSTOMER));
    }

    // ---------------------------------------------------------------
    // createForUser
    // ---------------------------------------------------------------

    @Test
    void createForUser_DeveCriarConta_QuandoUsuarioNaoTemConta() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("00001-0");
        when(accountRepository.existsByAccountNumber("00001-0")).thenReturn(false);
        when(encryptionService.encrypt("1234")).thenReturn("ENC1234");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.createForUser(10L, "1234");

        assertEquals(10L, created.getUserId());
        assertEquals("00001-0", created.getAccountNumber());
        assertEquals(AccountType.CUSTOMER, created.getAccountType());
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertEquals(0, created.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("ENC1234", created.getPassword());
    }

    @Test
    void createForUser_DeveSerIdempotente_QuandoContaJaExiste() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        Account result = accountService.createForUser(10L, "1234");

        assertSame(active, result);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createForUser_DeveRegerarNumero_QuandoColisao() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(accountNumberGenerator.generateAccountNumber())
                .thenReturn("00001-0", "00002-0");
        when(accountRepository.existsByAccountNumber("00001-0")).thenReturn(true);
        when(accountRepository.existsByAccountNumber("00002-0")).thenReturn(false);
        when(encryptionService.encrypt("1234")).thenReturn("ENC1234");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.createForUser(10L, "1234");

        assertEquals("00002-0", created.getAccountNumber());
        verify(accountNumberGenerator, times(2)).generateAccountNumber();
    }

    // ---------------------------------------------------------------
    // createMerchantAccountIfMissing
    // ---------------------------------------------------------------

    @Test
    void createMerchantAccountIfMissing_DeveCriarMerchant_QuandoNaoExiste() {
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.empty());
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("99999-0");
        when(accountRepository.existsByAccountNumber("99999-0")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account merchant = accountService.createMerchantAccountIfMissing();

        assertEquals(AccountType.MERCHANT, merchant.getAccountType());
        assertEquals(AccountStatus.ACTIVE, merchant.getStatus());
        assertEquals(new BigDecimal("10000000.00"), merchant.getBalance());
    }

    @Test
    void createMerchantAccountIfMissing_DeveSerIdempotente_QuandoJaExiste() {
        Account merchant = Account.builder()
                .id(2L).accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));

        Account result = accountService.createMerchantAccountIfMissing();

        assertSame(merchant, result);
        verify(accountRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // getBalance / getBalanceByEmail
    // ---------------------------------------------------------------

    @Test
    void getBalance_DeveRetornarSaldo_QuandoContaExiste() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        BigDecimal balance = accountService.getBalance(10L);

        assertEquals(new BigDecimal("100.00"), balance);
    }

    @Test
    void getBalance_DeveLancarException_QuandoContaNaoExiste() {
        when(accountRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getBalance(99L));
    }

    @Test
    void getBalanceByEmail_DeveRetornarSaldo_QuandoContaExiste() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        BigDecimal balance = accountService.getBalanceByEmail("ana@email.com");

        assertEquals(new BigDecimal("100.00"), balance);
    }

    // ---------------------------------------------------------------
    // findMyAccount / findByIdAdmin
    // ---------------------------------------------------------------

    @Test
    void findMyAccount_DeveRetornarResponse() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        AccountResponse response = accountService.findMyAccount("ana@email.com");

        assertEquals(1L, response.id());
        assertEquals("00001-0", response.accountNumber());
        assertEquals(AccountType.CUSTOMER, response.accountType());
    }

    @Test
    void findByIdAdmin_DeveRetornarConta_QuandoExiste() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));

        Account result = accountService.findByIdAdmin(1L);

        assertSame(active, result);
    }

    @Test
    void findByIdAdmin_DeveLancarException_QuandoNaoExiste() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.findByIdAdmin(99L));
    }

    // ---------------------------------------------------------------
    // debit
    // ---------------------------------------------------------------

    @Test
    void debit_DeveDescontarSaldo_QuandoContaAtivaESaldoSuficiente() {
        when(accountRepository.save(active)).thenReturn(active);

        Account result = accountService.debit(active, new BigDecimal("30.00"), null, null, TransactionType.PAYMENT);

        assertEquals(new BigDecimal("70.00"), result.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void debit_DeveLancarException_QuandoSaldoInsuficiente() {
        assertThrows(InsufficientBalanceException.class,
                () -> accountService.debit(active, new BigDecimal("200.00"), null, null, null));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_DeveLancarException_QuandoContaRestricted() {
        active.setStatus(AccountStatus.RESTRICTED);

        assertThrows(AccountRestrictedException.class,
                () -> accountService.debit(active, new BigDecimal("10.00"), null, null, null));
    }

    @Test
    void debit_DeveLancarException_QuandoContaBlocked() {
        active.setStatus(AccountStatus.BLOCKED);

        assertThrows(AccountBlockedException.class,
                () -> accountService.debit(active, new BigDecimal("10.00"), null, null, null));
    }

    @Test
    void debit_DeveLancarException_QuandoValorInvalido() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.debit(active, BigDecimal.ZERO, null, null, null));
        assertThrows(InvalidAmountException.class,
                () -> accountService.debit(active, null, null, null, null));
        assertThrows(InvalidAmountException.class,
                () -> accountService.debit(active, new BigDecimal("-1.00"), null, null, null));
    }

    // ---------------------------------------------------------------
    // credit
    // ---------------------------------------------------------------

    @Test
    void credit_DeveAdicionarSaldo_QuandoContaAtiva() {
        when(accountRepository.save(active)).thenReturn(active);

        Account result = accountService.credit(active, new BigDecimal("50.00"), null, null, TransactionType.DEPOSIT);

        assertEquals(new BigDecimal("150.00"), result.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void credit_DevePermitir_QuandoContaRestricted() {
        active.setStatus(AccountStatus.RESTRICTED);
        when(accountRepository.save(active)).thenReturn(active);

        Account result = accountService.credit(active, new BigDecimal("50.00"), null, null, null);

        assertEquals(new BigDecimal("150.00"), result.getBalance());
    }

    @Test
    void credit_DeveLancarException_QuandoContaBlocked() {
        active.setStatus(AccountStatus.BLOCKED);

        assertThrows(AccountBlockedException.class,
                () -> accountService.credit(active, new BigDecimal("50.00"), null, null, null));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void credit_DeveLancarException_QuandoValorInvalido() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.credit(active, BigDecimal.ZERO, null, null, null));
    }

    // ---------------------------------------------------------------
    // deposit (por email)
    // ---------------------------------------------------------------

    @Test
    void deposit_DeveCreditarConta_QuandoValido() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(active)).thenReturn(active);

        AccountResponse response = accountService.deposit("ana@email.com",
                new BigDecimal("25.00"), "depósito ATM");

        assertEquals(new BigDecimal("125.00"), response.balance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction t = captor.getValue();
        assertEquals(TransactionType.DEPOSIT, t.getType());
        assertEquals("depósito ATM", t.getDescription());
    }

    @Test
    void deposit_DeveLancarException_QuandoContaBlocked() {
        active.setStatus(AccountStatus.BLOCKED);
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        assertThrows(AccountBlockedException.class,
                () -> accountService.deposit("ana@email.com", new BigDecimal("10.00"), "x"));
    }

    // ---------------------------------------------------------------
    // refund / cashback
    // ---------------------------------------------------------------

    @Test
    void refund_DeveDebitarMerchantECreditarCustomer_QuandoEstornoValido() {
        Account merchant = Account.builder()
                .id(2L).accountNumber("00000-0")
                .balance(new BigDecimal("10000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno");

        assertEquals(new BigDecimal("9950.00"), merchant.getBalance());
        assertEquals(new BigDecimal("150.00"), active.getBalance());
    }

    @Test
    void refund_DeveLancarException_QuandoMerchantSemSaldo() {
        Account merchant = Account.builder()
                .accountNumber("00000-0").balance(new BigDecimal("10.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        assertThrows(InsufficientBalanceException.class,
                () -> accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno"));
    }

    @Test
    void refund_DeveCreditarMesmoEmContaRestricted_QuandoCustomerEstaRestrito() {
        Account merchant = Account.builder()
                .id(2L).accountNumber("00000-0")
                .balance(new BigDecimal("10000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();

        active.setStatus(AccountStatus.RESTRICTED);

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno");

        assertEquals(new BigDecimal("150.00"), active.getBalance());
    }

    @Test
    void refund_DeveLancarException_QuandoMerchantNaoExiste() {
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> accountService.refund(10L, new BigDecimal("50.00"), "REF", "x"));
    }

    @Test
    void cashback_DeveDebitarMerchantECreditarCustomer_ComTransacaoCashback() {
        Account merchant = Account.builder()
                .id(2L).accountNumber("00000-0")
                .balance(new BigDecimal("10000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.cashback(10L, new BigDecimal("15.00"), "ORDER-42", "Cashback");

        assertEquals(new BigDecimal("9985.00"), merchant.getBalance());
        assertEquals(new BigDecimal("115.00"), active.getBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        captor.getAllValues().forEach(t -> assertEquals(TransactionType.CASHBACK, t.getType()));
    }

    @Test
    void cashback_DeveLancarException_QuandoMerchantNaoExiste() {
        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> accountService.cashback(10L, new BigDecimal("10.00"), "REF", "x"));
    }

    // ---------------------------------------------------------------
    // creditMerchant
    // ---------------------------------------------------------------

    @Test
    void creditMerchant_DeveCreditar_QuandoMerchantExiste() {
        Account merchant = Account.builder()
                .id(2L).accountNumber("00000-0")
                .balance(new BigDecimal("1000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));

        accountService.creditMerchant(new BigDecimal("200.00"), "ORDER-1", "Pagamento");

        assertEquals(new BigDecimal("1200.00"), merchant.getBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(TransactionType.PAYMENT, captor.getValue().getType());
    }

    // ---------------------------------------------------------------
    // debitForInvoicePayment
    // ---------------------------------------------------------------

    @Test
    void debitForInvoicePayment_DeveDebitar_QuandoContaAtivaComSaldo() {
        accountService.debitForInvoicePayment(active, new BigDecimal("40.00"), "INVOICE-1", "Fatura");

        assertEquals(new BigDecimal("60.00"), active.getBalance());
        verify(accountRepository).save(active);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void debitForInvoicePayment_DevePermitir_QuandoContaRestricted() {
        active.setStatus(AccountStatus.RESTRICTED);

        accountService.debitForInvoicePayment(active, new BigDecimal("40.00"), "INVOICE-1", "Fatura");

        assertEquals(new BigDecimal("60.00"), active.getBalance());
    }

    @Test
    void debitForInvoicePayment_DeveLancarException_QuandoContaBlocked() {
        active.setStatus(AccountStatus.BLOCKED);

        assertThrows(AccountNotActiveException.class,
                () -> accountService.debitForInvoicePayment(active, new BigDecimal("10.00"), "INV", "x"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debitForInvoicePayment_DeveLancarException_QuandoSaldoInsuficiente() {
        assertThrows(InsufficientBalanceException.class,
                () -> accountService.debitForInvoicePayment(active, new BigDecimal("500.00"), "INV", "x"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debitForInvoicePayment_DeveLancarException_QuandoValorInvalido() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.debitForInvoicePayment(active, BigDecimal.ZERO, "INV", "x"));
    }

    // ---------------------------------------------------------------
    // listMyTransactions
    // ---------------------------------------------------------------

    @Test
    void listMyTransactions_DeveRetornarListaMapeada() {
        Transaction t1 = Transaction.builder()
                .id(1L).account(active).type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("10.00")).balanceAfter(new BigDecimal("110.00"))
                .build();
        Transaction t2 = Transaction.builder()
                .id(2L).account(active).type(TransactionType.PAYMENT)
                .amount(new BigDecimal("5.00")).balanceAfter(new BigDecimal("105.00"))
                .build();

        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t1, t2));

        List<TransactionResponse> result = accountService.listMyTransactions("ana@email.com");

        assertEquals(2, result.size());
        assertEquals(TransactionType.DEPOSIT, result.get(0).type());
        assertEquals(TransactionType.PAYMENT, result.get(1).type());
    }

    // ---------------------------------------------------------------
    // findAllAccounts
    // ---------------------------------------------------------------

    @Test
    void findAllAccounts_DeveRetornarTodasMapeadas() {
        Account other = Account.builder()
                .id(2L).userId(20L).accountNumber("00002-0")
                .balance(new BigDecimal("250.00"))
                .accountType(AccountType.CUSTOMER).status(AccountStatus.ACTIVE).build();
        when(accountRepository.findAll()).thenReturn(List.of(active, other));

        List<AccountResponse> result = accountService.findAllAccounts();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
    }

    // ---------------------------------------------------------------
    // adminDeposit
    // ---------------------------------------------------------------

    @Test
    void adminDeposit_DeveCreditarComDescricaoCustomizada() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));
        when(accountRepository.save(active)).thenReturn(active);

        AccountResponse response = accountService.adminDeposit(1L,
                new BigDecimal("50.00"), "Bônus");

        assertEquals(new BigDecimal("150.00"), response.balance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals("Bônus", captor.getValue().getDescription());
        assertEquals(TransactionType.DEPOSIT, captor.getValue().getType());
    }

    @Test
    void adminDeposit_DeveUsarDescricaoPadrao_QuandoNulaOuVazia() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));
        when(accountRepository.save(active)).thenReturn(active);

        accountService.adminDeposit(1L, new BigDecimal("10.00"), "   ");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals("Depósito administrativo", captor.getValue().getDescription());
    }

    @Test
    void adminDeposit_DeveLancarException_QuandoContaNaoExiste() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.adminDeposit(99L, new BigDecimal("10.00"), null));
    }

    @Test
    void adminDeposit_DeveLancarException_QuandoContaBlocked() {
        active.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThrows(AccountBlockedException.class,
                () -> accountService.adminDeposit(1L, new BigDecimal("10.00"), null));
    }

    // ---------------------------------------------------------------
    // blockAccount / unBlockAccount
    // ---------------------------------------------------------------

    @Test
    void blockAccount_DeveBloquearConta() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));
        when(accountRepository.save(active)).thenReturn(active);

        AccountResponse response = accountService.blockAccount(1L);

        assertEquals(AccountStatus.BLOCKED, active.getStatus());
        assertEquals(AccountStatus.BLOCKED, response.status());
    }

    @Test
    void blockAccount_DeveLancarException_QuandoContaNaoExiste() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.blockAccount(99L));
    }

    @Test
    void unBlockAccount_DeveReativarConta() {
        active.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(active));
        when(accountRepository.save(active)).thenReturn(active);

        AccountResponse response = accountService.unBlockAccount(1L);

        assertEquals(AccountStatus.ACTIVE, active.getStatus());
        assertEquals(AccountStatus.ACTIVE, response.status());
    }

    @Test
    void unBlockAccount_DeveLancarException_QuandoContaNaoExiste() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.unBlockAccount(99L));
    }

    // ---------------------------------------------------------------
    // findByUserId / findAccountByUserId
    // ---------------------------------------------------------------

    @Test
    void findByUserId_DeveRetornarConta_QuandoExiste() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        assertSame(active, accountService.findByUserId(10L));
        assertSame(active, accountService.findAccountByUserId(10L));
    }

    @Test
    void findByUserId_DeveLancarException_QuandoNaoExiste() {
        when(accountRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.findByUserId(99L));
    }
}
