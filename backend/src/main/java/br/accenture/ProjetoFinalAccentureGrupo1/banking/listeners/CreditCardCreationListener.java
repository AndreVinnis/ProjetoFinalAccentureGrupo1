package br.accenture.ProjetoFinalAccentureGrupo1.banking.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditCardCreationListener {

    private final AccountService accountService;
    private final CreditCardService creditCardService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        Account account = accountService.createForUser(event.userId());
        creditCardService.createCardForAccount(account);
    }
}
