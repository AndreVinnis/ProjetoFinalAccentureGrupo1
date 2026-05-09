package br.accenture.ProjetoFinalAccentureGrupo1.banking.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
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

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        accountService.createForUser(event.userId());
        log.info("Conta criada para o usuário {} (e-mail: {}).",
                event.userId(), event.email());
    }
}
