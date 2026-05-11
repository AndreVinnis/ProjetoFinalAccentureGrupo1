package br.accenture.ProjetoFinalAccentureGrupo1.banking.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// André Vinícius Barros Macambira
public class UserRegisteredListener {

    private final AccountService accountService;
    private final CreditCardService creditCardService;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        Account account= accountService.createForUser(event.userId());
        creditCardService.createCardForAccount(account);
        log.info("Conta criada para o usuário {} (e-mail: {}).",
                event.userId(), event.email());
    }
}
