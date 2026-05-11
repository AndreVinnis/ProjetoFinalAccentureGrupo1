package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentReceivedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.*;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CardPurchaseRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.repository.CreditCardRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.utils.CreditCardNumberGenerator;
import br.accenture.ProjetoFinalAccentureGrupo1.shared.security.AESEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock private UserFacade userFacade;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CardPurchaseRepository cardPurchaseRepository;
    @Mock private CreditCardNumberGenerator cardNumberGenerator;
    @Mock private InvoiceService invoiceService;
    @Mock private AccountService accountService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AESEncryptionService encryptionService;

    @InjectMocks
    private CreditCardService creditCardService;

    private Account account;
    private CreditCard card;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .userId(10L)
                .build();

        card = CreditCard.builder()
                .id(100L)
                .account(account)
                .holderName("Ana Silva")
                .numberHash("ENC(1234567890123456)")
                .cvvHash("ENC(123)")
                .expirationMonth(12)
                .expirationYear(LocalDate.now().getYear() + 5)
                .status(CreditCardStatus.ACTIVE)
                .creditLimit(new BigDecimal("1000.00"))
                .availableLimit(new BigDecimal("1000.00"))
                .closingDay(25)
                .dueDay(10)
                .build();

        userInfo = new UserInfo(10L, "Ana Silva", "ana@email.com",
                "12345678901", LocalDate.of(1990, 5, 15), Set.of(Role.CUSTOMER));
    }

    @Test
    void createCardForAccount_DeveCriarCartao_QuandoUsuarioNaoTemCartao() {
        when(creditCardRepository.existsByAccount(account)).thenReturn(false);
        when(userFacade.findById(10L)).thenReturn(userInfo);
        when(cardNumberGenerator.generateCardNumber()).thenReturn("4111111111111111");
        when(cardNumberGenerator.generateCvv()).thenReturn("123");
        when(encryptionService.encrypt("4111111111111111")).thenReturn("ENC(num)");
        when(encryptionService.encrypt("123")).thenReturn("ENC(123)");

        creditCardService.createCardForAccount(account);

        ArgumentCaptor<CreditCard> captor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository).save(captor.capture());
        CreditCard saved = captor.getValue();
        assertEquals("Ana Silva", saved.getHolderName());
        assertEquals("ENC(num)", saved.getNumberHash());
        assertEquals("ENC(123)", saved.getCvvHash());
        assertEquals(CreditCardStatus.ACTIVE, saved.getStatus());
        assertEquals(new BigDecimal("1000.00"), saved.getCreditLimit());
        assertEquals(new BigDecimal("1000.00"), saved.getAvailableLimit());
    }

    @Test
    void createCardForAccount_DeveLancarException_QuandoCartaoJaExiste() {
        when(creditCardRepository.existsByAccount(account)).thenReturn(true);

        assertThrows(
                CreditCardAlreadyExistsException.class,
                () -> creditCardService.createCardForAccount(account)
        );
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void chargeCard_DeveCobrarEPublicarEvento_QuandoDadosValidos() {
        Invoice openInvoice = Invoice.builder().id(50L).build();

        when(creditCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(encryptionService.encrypt("123")).thenReturn("ENC(123)");
        when(invoiceService.getOrCreateOpenInvoice(card)).thenReturn(openInvoice);

        creditCardService.chargeCard(100L, new BigDecimal("200.00"), "123",
                "Compra teste", "ORDER-42");

        // limite decrementado
        assertEquals(new BigDecimal("800.00"), card.getAvailableLimit());

        // CardPurchase salvo
        verify(cardPurchaseRepository).save(any());

        // invoice atualizado
        verify(invoiceService).addCardPurchase(eq(openInvoice), any());

        // merchant creditado
        verify(accountService).creditMerchant(
                new BigDecimal("200.00"), "Compra teste", "ORDER-42"
        );

        // evento publicado
        ArgumentCaptor<PaymentReceivedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentReceivedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentReceivedEvent event = eventCaptor.getValue();
        assertEquals("ORDER-42", event.reference());
        assertEquals(10L, event.payerUserId());
        assertEquals(new BigDecimal("200.00"), event.amount());
    }

    @Test
    void chargeCard_DeveLancarException_QuandoCartaoBloqueado() {
        card.setStatus(CreditCardStatus.BLOCKED);
        when(creditCardRepository.findById(100L)).thenReturn(Optional.of(card));

        assertThrows(
                CreditCardBlockedException.class,
                () -> creditCardService.chargeCard(100L, new BigDecimal("100.00"),
                        "123", "Teste", "ORDER-1")
        );
        verify(eventPublisher, never()).publishEvent(any());
        verify(accountService, never()).creditMerchant(any(), any(), any());
    }

    @Test
    void chargeCard_DeveLancarException_QuandoLimiteInsuficiente() {
        card.setAvailableLimit(new BigDecimal("50.00"));
        when(creditCardRepository.findById(100L)).thenReturn(Optional.of(card));

        assertThrows(
                InsufficientCreditLimitException.class,
                () -> creditCardService.chargeCard(100L, new BigDecimal("100.00"),
                        "123", "Teste", "ORDER-1")
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void chargeCard_DeveLancarException_QuandoCvvErrado() {
        when(creditCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(encryptionService.encrypt("999")).thenReturn("ENC(999)");

        assertThrows(
                WrongCvvException.class,
                () -> creditCardService.chargeCard(100L, new BigDecimal("100.00"),
                        "999", "Teste", "ORDER-1")
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void chargeCard_DeveLancarException_QuandoCartaoNaoEncontrado() {
        when(creditCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> creditCardService.chargeCard(999L, new BigDecimal("100.00"),
                        "123", "Teste", "ORDER-1")
        );
    }

    @Test
    void blockCardByAccount_DeveAlterarStatusParaBlocked() {
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));
        when(creditCardRepository.save(card)).thenReturn(card);

        creditCardService.blockCardByAccount(account);

        assertEquals(CreditCardStatus.BLOCKED, card.getStatus());
        verify(creditCardRepository).findByAccount(account);
        verify(creditCardRepository).save(card);
    }

    @Test
    void unblockCardByAccount_DeveAlterarStatusParaActive() {
        card.setStatus(CreditCardStatus.BLOCKED);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));
        when(creditCardRepository.save(card)).thenReturn(card);

        creditCardService.unblockCardByAccount(account);

        assertEquals(CreditCardStatus.ACTIVE, card.getStatus());
        verify(creditCardRepository).findByAccount(account);
        verify(creditCardRepository).save(card);
    }

    @Test
    void findMyCard_DeveRetornarDadosDoCartao_QuandoExiste() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));
        when(encryptionService.decrypt("ENC(1234567890123456)"))
                .thenReturn("1234567890123456");
        when(encryptionService.decrypt("ENC(123)")).thenReturn("123");

        var response = creditCardService.findMyCard("ana@email.com");

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Ana Silva", response.holderName());
    }

    @Test
    void findMyCard_DeveLancarException_QuandoUsuarioSemCartao() {
        when(userFacade.findByEmail("naotem@email.com")).thenReturn(
                new UserInfo(99L, "X", "naotem@email.com", "00000000000",
                        LocalDate.of(2000, 1, 1), Set.of(Role.CUSTOMER))
        );
        when(accountService.findByUserId(anyLong())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.empty());

        assertThrows(
                CreditCardNotFoundException.class,
                () -> creditCardService.findMyCard("naotem@email.com")
        );
    }
}