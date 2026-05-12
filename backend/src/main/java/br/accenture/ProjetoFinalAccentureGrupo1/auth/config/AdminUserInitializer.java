package br.accenture.ProjetoFinalAccentureGrupo1.auth.config;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAdminUsers() {
        createEcommerceAdminIfMissing();
        createBankingAdminIfMissing();
    }

    private void createEcommerceAdminIfMissing() {
        String adminEmail = "ecommerce.admin@accenture.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User ecommerceAdmin = User.builder()
                    .name("Admin E-Commerce")
                    .email(adminEmail)
                    .cpf("11111111111")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .roles(Set.of(Role.ECOMMERCE_ADMIN))
                    .build();

            userRepository.save(ecommerceAdmin);
            System.out.println("Usuário ECOMMERCE_ADMIN criado com sucesso.");
        }
    }

    private void createBankingAdminIfMissing() {
        String adminEmail = "banking.admin@accenture.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User bankingAdmin = User.builder()
                    .name("Admin Banking")
                    .email(adminEmail)
                    .cpf("22222222222")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .roles(Set.of(Role.BANKING_ADMIN))
                    .build();

            userRepository.save(bankingAdmin);
            System.out.println("Usuário BANKING_ADMIN criado com sucesso.");
        }
    }
}
