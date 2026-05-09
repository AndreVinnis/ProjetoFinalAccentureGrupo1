package br.accenture.ProjetoFinalAccentureGrupo1.banking.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerAccountCreationListener {

//    private final AccountService accountService;
//
//    @EventListener
//    public void onUserRegistered(UserRegisteredEvent event) {
//        accountService.createCustomerBankingProfile(event);
//    }
}
