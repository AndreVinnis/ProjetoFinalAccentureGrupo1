package br.accenture.ProjetoFinalAccentureGrupo1.shared.security;

public interface EncryptionService {

    String encrypt(String value);

    String decrypt(String value);
}
