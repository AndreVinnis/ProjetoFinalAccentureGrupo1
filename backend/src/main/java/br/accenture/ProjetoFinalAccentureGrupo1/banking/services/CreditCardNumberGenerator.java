package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CreditCardNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BANK_IDENTIFICATION_NUMBER = "543210";

    public String generateCardNumber() {
        String partialNumber = BANK_IDENTIFICATION_NUMBER + randomDigits(9);
        return partialNumber + calculateCheckDigit(partialNumber);
    }

    public String generateCvv() {
        return randomDigits(3);
    }

    private String randomDigits(int length) {
        return IntStream.range(0, length)
                .mapToObj(index -> String.valueOf(RANDOM.nextInt(10)))
                .collect(Collectors.joining());
    }

    private int calculateCheckDigit(String partialNumber) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int i = partialNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(partialNumber.charAt(i));
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }
}
