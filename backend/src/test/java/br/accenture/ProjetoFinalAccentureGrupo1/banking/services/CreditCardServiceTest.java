package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CardPurchase;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardPurchaseResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditLimitResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
                .password("ENC(1234)")
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
        assertEquals(new BigDecimal("5000.00"), saved.getCreditLimit());
        assertEquals(new BigDecimal("5000.00"), saved.getAvailableLimit());
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
    void chargeCard_DeveCobrar_QuandoDadosValidos() {
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
        verify(creditCardRepository).save(card);

        // invoice atualizado
        verify(invoiceService).addCardPurchase(eq(openInvoice), any());

        // merchant creditado
        verify(accountService).creditMerchant(
                new BigDecimal("200.00"), "Compra teste", "ORDER-42"
        );
    }

    @Test
    void chargeCard_DeveParcelarCompraEmFaturasMensais() {
        Invoice currentInvoice = Invoice.builder()
                .id(50L)
                .referenceMonth(YearMonth.of(2026, 5))
                .build();
        Invoice secondInvoice = Invoice.builder()
                .id(51L)
                .referenceMonth(YearMonth.of(2026, 6))
                .build();
        Invoice thirdInvoice = Invoice.builder()
                .id(52L)
                .referenceMonth(YearMonth.of(2026, 7))
                .build();

        when(creditCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(encryptionService.encrypt("123")).thenReturn("ENC(123)");
        when(invoiceService.getOrCreateOpenInvoice(card)).thenReturn(currentInvoice);
        when(invoiceService.getOrCreateOpenInvoice(card, YearMonth.of(2026, 6))).thenReturn(secondInvoice);
        when(invoiceService.getOrCreateOpenInvoice(card, YearMonth.of(2026, 7))).thenReturn(thirdInvoice);

        creditCardService.chargeCard(100L, new BigDecimal("100.00"), "123",
                "Compra parcelada", "ORDER-77", 3);

        ArgumentCaptor<CardPurchase> purchaseCaptor = ArgumentCaptor.forClass(CardPurchase.class);
        verify(cardPurchaseRepository, times(3)).save(purchaseCaptor.capture());

        List<CardPurchase> purchases = purchaseCaptor.getAllValues();
        assertEquals(new BigDecimal("33.34"), purchases.get(0).getAmount());
        assertEquals(new BigDecimal("33.33"), purchases.get(1).getAmount());
        assertEquals(new BigDecimal("33.33"), purchases.get(2).getAmount());
        assertEquals(1, purchases.get(0).getInstallmentNumber());
        assertEquals(2, purchases.get(1).getInstallmentNumber());
        assertEquals(3, purchases.get(2).getInstallmentNumber());
        assertEquals(3, purchases.get(0).getInstallmentTotal());
        assertEquals("Compra parcelada (1/3)", purchases.get(0).getDescription());
        assertEquals("Compra parcelada (3/3)", purchases.get(2).getDescription());
        assertEquals(purchases.get(0).getInstallmentGroupId(), purchases.get(1).getInstallmentGroupId());
        assertEquals(purchases.get(1).getInstallmentGroupId(), purchases.get(2).getInstallmentGroupId());

        verify(invoiceService).addCardPurchase(eq(currentInvoice), eq(purchases.get(0)));
        verify(invoiceService).addCardPurchase(eq(secondInvoice), eq(purchases.get(1)));
        verify(invoiceService).addCardPurchase(eq(thirdInvoice), eq(purchases.get(2)));
        assertEquals(new BigDecimal("900.00"), card.getAvailableLimit());
        verify(accountService).creditMerchant(new BigDecimal("100.00"), "Compra parcelada", "ORDER-77");
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
        verify(creditCardRepository).save(card);
    }

    @Test
    void unblockCardByAccount_DeveAlterarStatusParaActive() {
        card.setStatus(CreditCardStatus.BLOCKED);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));
        when(creditCardRepository.save(card)).thenReturn(card);

        creditCardService.unblockCardByAccount(account);

        assertEquals(CreditCardStatus.ACTIVE, card.getStatus());
        verify(creditCardRepository).save(card);
    }

    @Test
    void findMyCard_DeveRetornarDadosDoCartao_QuandoExisteESenhaCorreta() {
        when(accountService.findAccountByUserEmail("ana@email.com")).thenReturn(account);
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));

        when(encryptionService.decrypt(anyString())).thenReturn("1234");
        when(encryptionService.decrypt(card.getNumberHash())).thenReturn("1234567890123456");
        when(encryptionService.decrypt(card.getCvvHash())).thenReturn("123");

        CreditCardResponse response = creditCardService.findMyCard("ana@email.com", "1234");

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Ana Silva", response.holderName());
        assertEquals("1234567890123456", response.cardNumbers());
    }

    @Test
    void findMyCard_DeveLancarException_QuandoSenhaIncorreta() {
        when(accountService.findAccountByUserEmail("ana@email.com")).thenReturn(account);
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));

        when(encryptionService.decrypt(anyString())).thenReturn(anyString());

        assertThrows(
                WrongPasswordException.class,
                () -> creditCardService.findMyCard("ana@email.com", "senhaErrada")
        );
    }

    @Test
    void findMyCard_DeveLancarException_QuandoUsuarioSemCartao() {
        when(accountService.findAccountByUserEmail("ana@email.com")).thenReturn(account);
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.empty());

        assertThrows(
                CreditCardNotFoundException.class,
                () -> creditCardService.findMyCard("ana@email.com", "1234")
        );
    }

    @Test
    void findMyCardByAccount_DeveRetornarCartao_QuandoExiste() {
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));

        CreditCard foundCard = creditCardService.findMyCardByAccount(account);

        assertNotNull(foundCard);
        assertEquals(100L, foundCard.getId());
    }

    @Test
    void findMyCardByAccount_DeveLancarException_QuandoNaoExiste() {
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.empty());

        assertThrows(
                CreditCardNotFoundException.class,
                () -> creditCardService.findMyCardByAccount(account)
        );
    }

    @Test
    void findMyLimit_DeveRetornarLimiteDoCartao() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));

        CreditLimitResponse limitResponse = creditCardService.findMyLimit("ana@email.com");

        assertNotNull(limitResponse);
        assertEquals(new BigDecimal("1000.00"), limitResponse.creditLimit());
        assertEquals(new BigDecimal("1000.00"), limitResponse.availableLimit());
        assertEquals(new BigDecimal("0.00"), limitResponse.usedLimit());
    }

    @Test
    void findMyPurchases_DeveRetornarListaDeCompras() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo);
        when(accountService.findByUserId(userInfo.id())).thenReturn(account);
        when(creditCardRepository.findByAccount(account)).thenReturn(Optional.of(card));

        Invoice invoice = Invoice.builder().id(1L).build();
        CardPurchase purchase = CardPurchase.builder()
                .id(1L)
                .card(card)
                .invoice(invoice)
                .amount(new BigDecimal("150.00"))
                .description("Amazon")
                .reference("REF-123")
                .purchaseDate(Instant.now())
                .build();

        when(cardPurchaseRepository.findByCardIdOrderByPurchaseDateDesc(card.getId()))
                .thenReturn(List.of(purchase));

        List<CardPurchaseResponse> purchases = creditCardService.findMyPurchases("ana@email.com");

        assertNotNull(purchases);
        assertFalse(purchases.isEmpty());
        assertEquals(1, purchases.size());
        assertEquals("Amazon", purchases.get(0).description());
        assertEquals(new BigDecimal("150.00"), purchases.get(0).amount());
    }

    @Test
    void validateCard_DeveRetornarCardValidationResponse_QuandoDadosValidos() {
        when(encryptionService.encrypt("1234567890123456")).thenReturn("ENC(1234567890123456)");
        when(creditCardRepository.findByNumberHash("ENC(1234567890123456)")).thenReturn(Optional.of(card));
        when(encryptionService.decrypt("ENC(123)")).thenReturn("123");

        CardValidationResponse response = creditCardService.validateCard(
                "1234567890123456",
                "123",
                12,
                LocalDate.now().getYear() + 5
        );

        assertNotNull(response);
        assertEquals(100L, response.cardId());
        assertEquals("3456", response.lastForDigits());
    }

    @Test
    void validateCard_DeveLancarException_QuandoCartaoNaoExiste() {
        when(encryptionService.encrypt("9999999999999999")).thenReturn("ENC(9999999999999999)");
        when(creditCardRepository.findByNumberHash("ENC(9999999999999999)")).thenReturn(Optional.empty());

        assertThrows(
                InvalidCardException.class,
                () -> creditCardService.validateCard(
                        "9999999999999999",
                        "123",
                        12,
                        2030
                )
        );
    }

    @Test
    void validateCard_DeveLancarException_QuandoCvvInvalido() {
        when(encryptionService.encrypt("1234567890123456")).thenReturn("ENC(1234567890123456)");
        when(creditCardRepository.findByNumberHash("ENC(1234567890123456)")).thenReturn(Optional.of(card));
        when(encryptionService.decrypt("ENC(123)")).thenReturn("123");

        assertThrows(
                InvalidCardException.class,
                () -> creditCardService.validateCard(
                        "1234567890123456",
                        "999",
                        12,
                        LocalDate.now().getYear() + 5
                )
        );
    }

    @Test
    void validateCard_DeveLancarException_QuandoMesExpiracaoInvalido() {
        when(encryptionService.encrypt("1234567890123456")).thenReturn("ENC(1234567890123456)");
        when(creditCardRepository.findByNumberHash("ENC(1234567890123456)")).thenReturn(Optional.of(card));
        when(encryptionService.decrypt("ENC(123)")).thenReturn("123");

        assertThrows(
                InvalidCardException.class,
                () -> creditCardService.validateCard(
                        "1234567890123456",
                        "123",
                        11, // Mês errado
                        LocalDate.now().getYear() + 5
                )
        );
    }

    @Test
    void validateCard_DeveLancarException_QuandoAnoExpiracaoInvalido() {
        when(encryptionService.encrypt("1234567890123456")).thenReturn("ENC(1234567890123456)");
        when(creditCardRepository.findByNumberHash("ENC(1234567890123456)")).thenReturn(Optional.of(card));
        when(encryptionService.decrypt("ENC(123)")).thenReturn("123");

        assertThrows(
                InvalidCardException.class,
                () -> creditCardService.validateCard(
                        "1234567890123456",
                        "123",
                        12,
                        2000 // Ano errado
                )
        );
    }
}
