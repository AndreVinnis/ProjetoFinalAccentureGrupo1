package br.accenture.ProjetoFinalAccentureGrupo1.banking.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditCardCreationListener {

    private final CreditCardService creditCardService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        creditCardService.createVirtualCardForUser(event.userId());
    }
}
