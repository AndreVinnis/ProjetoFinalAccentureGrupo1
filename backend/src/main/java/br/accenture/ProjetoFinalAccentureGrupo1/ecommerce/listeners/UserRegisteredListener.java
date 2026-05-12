package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("ecommerceUserRegisteredListener")
@RequiredArgsConstructor
@Slf4j
//Autor: Cainã Moura Araújo
public class UserRegisteredListener {

    private final CustomerService customerService;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String address = formatAddress(event);
        customerService.createCustomer(event.userId(), address, event.phone());
        log.info("Cliente ecommerce criado para o usuario {} (e-mail: {}).", event.userId(), event.email());
    }

    private String formatAddress(UserRegisteredEvent event) {
        String baseAddress = "%s, %s - %s, %s - %s, %s"
                .formatted(
                        event.street(),
                        event.number(),
                        event.neighborhood(),
                        event.city(),
                        event.state(),
                        event.zipCode()
                );

        if (event.complement() == null || event.complement().isBlank()) {
            return baseAddress;
        }

        return baseAddress + " - " + event.complement();
    }
}
