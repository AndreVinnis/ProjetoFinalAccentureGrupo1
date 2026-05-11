package br.accenture.ProjetoFinalAccentureGrupo1.banking.config;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyAccountInitializer {

    private final AccountService accountService;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureMerchantAccount() {
        accountService.createMerchantAccountIfMissing();
    }
}
