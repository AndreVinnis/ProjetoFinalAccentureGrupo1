package br.accenture.ProjetoFinalAccentureGrupo1.shared.security;

import br.accenture.ProjetoFinalAccentureGrupo1.shared.exceptions.CryptographyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class AESEncryptionService {

    @Value("${crypto.secret-key:1234567890123456}")
    private String SECRET;

    public String encrypt(String value) {
        try{
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            return Base64.getEncoder().encodeToString(encrypted);
        }
        catch (Exception e){
            throw new CryptographyException();
        }
    }

    public String decrypt(String encrypted) {
        try{
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decoded = Base64.getDecoder().decode(encrypted);

            return new String(cipher.doFinal(decoded));
        }
        catch (Exception e){
            throw new CryptographyException();
        }
    }
}
