package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.api.BankingFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.SavedCard;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.RegisterSavedCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.SavedCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.SavedCardAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.SavedCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedCardServiceTest {

    @Mock private SavedCardRepository savedCardRepository;
    @Mock private CustomerService customerService;
    @Mock private BankingFacade bankingFacade;

    @InjectMocks
    private SavedCardService savedCardService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(10L)
                .shippingAddress("Rua A")
                .phone("11999999999")
                .build();
    }

    @Test
    void registerCard_DeveValidarNoBankingESalvarSomenteTokenEUltimosDigitos() {
        RegisterSavedCardRequest request = new RegisterSavedCardRequest(
                "4111111111111111",
                "123",
                12,
                2030,
                " Ana Silva "
        );
        when(customerService.findByEmail("ana@email.com")).thenReturn(customer);
        when(bankingFacade.verifyCard("4111111111111111", "123", 12, 2030))
                .thenReturn(new CardValidationResponse(50L, "1111"));
        when(savedCardRepository.existsByCustomer_IdAndBankingCardId(1L, 50L)).thenReturn(false);
        when(savedCardRepository.save(any(SavedCard.class))).thenAnswer(inv -> {
            SavedCard savedCard = inv.getArgument(0);
            savedCard.setId(99L);
            return savedCard;
        });

        SavedCardResponse response = savedCardService.registerCard("ana@email.com", request);

        assertEquals(99L, response.id());
        assertEquals("1111", response.last4Digits());
        assertEquals("Ana Silva", response.holderName());
    }

    @Test
    void registerCard_DeveLancarException_QuandoCartaoJaEstaSalvo() {
        RegisterSavedCardRequest request = new RegisterSavedCardRequest(
                "4111111111111111",
                "123",
                12,
                2030,
                null
        );
        when(customerService.findByEmail("ana@email.com")).thenReturn(customer);
        when(bankingFacade.verifyCard("4111111111111111", "123", 12, 2030))
                .thenReturn(new CardValidationResponse(50L, "1111"));
        when(savedCardRepository.existsByCustomer_IdAndBankingCardId(1L, 50L)).thenReturn(true);

        assertThrows(SavedCardAlreadyExistsException.class,
                () -> savedCardService.registerCard("ana@email.com", request));
        verify(savedCardRepository, never()).save(any());
    }

    @Test
    void listMyCards_DeveRetornarCartoesDoCliente() {
        SavedCard savedCard = SavedCard.builder()
                .id(99L)
                .customer(customer)
                .bankingCardId(50L)
                .last4Digits("1111")
                .holderName("Ana Silva")
                .build();
        when(customerService.findByEmail("ana@email.com")).thenReturn(customer);
        when(savedCardRepository.findByCustomer_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(savedCard));

        List<SavedCardResponse> cards = savedCardService.listMyCards("ana@email.com");

        assertEquals(1, cards.size());
        assertEquals("1111", cards.get(0).last4Digits());
    }
}
