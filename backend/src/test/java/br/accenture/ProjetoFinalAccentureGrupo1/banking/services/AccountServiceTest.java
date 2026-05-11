package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountRestrictedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.AccountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.TransactionRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.utils.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private AccountService accountService;

    private Account active;

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
    }

    @Test
    void createForUser_DeveCriarConta_QuandoUsuarioNaoTemConta() {
        User user = User.builder()
                .id(10L)
                .name("Ana Silva")
                .email("ana@email.com")
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        when(accountRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("00001-0");
        when(accountRepository.existsByAccountNumber("00001-0")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.createForUser(10L);

        assertEquals(10L, created.getUserId());
        assertEquals("00001-0", created.getAccountNumber());
        assertEquals(AccountType.CUSTOMER, created.getAccountType());
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertEquals(0, created.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void createForUser_DeveSerIdempotente_QuandoContaJaExiste() {
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        Account result = accountService.createForUser(10L);

        assertSame(active, result);
        verify(accountRepository, never()).save(any());
    }

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
    void debit_DeveDescontarSaldo_QuandoContaAtivaESaldoSuficiente() {
        when(accountRepository.save(active)).thenReturn(active);

        Account result = accountService.debit(active, new BigDecimal("30.00"), null, null, null);

        assertEquals(new BigDecimal("70.00"), result.getBalance());
    }

    @Test
    void debit_DeveLancarException_QuandoSaldoInsuficiente() {
        assertThrows(InsufficientBalanceException.class, () -> accountService.debit(active, new BigDecimal("200.00"), null, null, null));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_DeveLancarException_QuandoContaRestricted() {
        active.setStatus(AccountStatus.RESTRICTED);

        assertThrows(AccountRestrictedException.class, () -> accountService.debit(active, new BigDecimal("10.00"), null, null, null));
    }

    @Test
    void credit_DeveAdicionarSaldo_QuandoContaAtiva() {
        when(accountRepository.save(active)).thenReturn(active);

        Account result = accountService.credit(active, new BigDecimal("50.00"), null, null, null);

        assertEquals(new BigDecimal("150.00"), result.getBalance());
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

        assertThrows(AccountBlockedException.class, () -> accountService.credit(active, new BigDecimal("50.00"), null, null, null));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void refund_DeveDebitarMerchantECreditarCustomer_QuandoEstornoValido() {
        Account merchant = Account.builder()
                .id(1L)
                .accountNumber("00000-0")
                .balance(new BigDecimal("10000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno");

        assertEquals(new BigDecimal("9950.00"), merchant.getBalance());
        assertEquals(new BigDecimal("150.00"), active.getBalance());
    }

    @Test
    void refund_DeveLancarException_QuandoMerchantSemSaldo() {
        Account merchant = Account.builder()
                .accountNumber("00000-0")
                .balance(new BigDecimal("10.00"))   // saldo insuficiente
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));

        assertThrows(
                InsufficientBalanceException.class,
                () -> accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno")
        );
    }

    @Test
    void refund_DeveCreditarMesmoEmContaRestricted_QuandoCustomerEstaRestrito() {
        Account merchant = Account.builder()
                .id(1L)
                .accountNumber("00000-0")
                .balance(new BigDecimal("10000.00"))
                .accountType(AccountType.MERCHANT)
                .status(AccountStatus.ACTIVE)
                .build();

        active.setStatus(AccountStatus.RESTRICTED);

        when(accountRepository.findFirstByAccountType(AccountType.MERCHANT))
                .thenReturn(Optional.of(merchant));
        when(accountRepository.findByUserId(10L)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Não deve lançar exception
        accountService.refund(10L, new BigDecimal("50.00"), "ORDER-42", "Estorno");

        assertEquals(new BigDecimal("150.00"), active.getBalance());
    }
}
