package com.bibliovendas.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public final class SenhaUtil {
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaUtil() {
    }

    public static String gerarHash(String senha) {
        validarPolitica(senha);

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(senha.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static void validarPolitica(String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }

        boolean maiuscula = false;
        boolean numero = false;
        boolean especial = false;
        for (int indice = 0; indice < senha.length();) {
            int caractere = senha.codePointAt(indice);
            maiuscula |= Character.isUpperCase(caractere);
            numero |= Character.isDigit(caractere);
            especial |= !Character.isLetterOrDigit(caractere);
            indice += Character.charCount(caractere);
        }
        if (!maiuscula || !numero || !especial) {
            throw new IllegalArgumentException(
                    "A senha deve conter uma letra maiúscula, um número e um caractere especial.");
        }
    }

    public static boolean verificar(String senha, String senhaHash) {
        if (senha == null || senhaHash == null) {
            return false;
        }

        try {
            String[] partes = senhaHash.split("\\$");
            if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
                return false;
            }
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[3]);
            byte[] hashInformado = derivar(senha.toCharArray(), salt, iteracoes);
            if (hashInformado.length != hashEsperado.length) {
                return false;
            }

            int diferenca = 0;
            for (int i = 0; i < hashEsperado.length; i++) {
                diferenca |= hashEsperado[i] ^ hashInformado[i];
            }
            return diferenca == 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Não foi possível gerar o hash da senha.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
