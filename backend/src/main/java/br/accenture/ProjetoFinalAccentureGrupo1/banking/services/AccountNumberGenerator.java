package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateAccountNumber() {
        int branch = 1000 + RANDOM.nextInt(9000);
        int number = 100000 + RANDOM.nextInt(900000);
        int digit = RANDOM.nextInt(10);
        return branch + String.valueOf(number) + digit;
    }
}
