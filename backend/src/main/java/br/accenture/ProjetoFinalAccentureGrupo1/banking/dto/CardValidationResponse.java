package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

public record CardValidationResponse(
        Long cardId,
        String lastForDigits
) {
}
