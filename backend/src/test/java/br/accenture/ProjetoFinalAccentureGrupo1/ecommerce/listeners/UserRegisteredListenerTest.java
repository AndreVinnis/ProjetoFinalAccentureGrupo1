package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private UserRegisteredListener listener;

    @Test
    void onUserRegistered_DeveCriarCustomerComEnderecoFormatado() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                10L,
                "Ana Silva",
                "ana@email.com",
                "12345678901",
                LocalDate.of(1990, 1, 1),
                "11999999999",
                "01001-000",
                "SP",
                "Sao Paulo",
                "Centro",
                "Rua A",
                "123",
                "Apto 45"
        );

        listener.onUserRegistered(event);

        ArgumentCaptor<String> addressCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerService).createCustomer(
                org.mockito.ArgumentMatchers.eq(10L),
                addressCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("11999999999")
        );
        assertEquals("Rua A, 123 - Centro, Sao Paulo - SP, 01001-000 - Apto 45", addressCaptor.getValue());
    }
}
