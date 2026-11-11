package com.bibliovendas;

import com.bibliovendas.model.SenhaUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaUtilTest {
    @Test
    void deveArmazenarSenhaComHashEValidarCredencial() {
        String hash = SenhaUtil.gerarHash("Senha-segura1!");

        assertNotEquals("Senha-segura1!", hash);
        assertTrue(SenhaUtil.verificar("Senha-segura1!", hash));
        assertFalse(SenhaUtil.verificar("senha-incorreta", hash));
    }

    @Test
    void deveExigirMaiusculaNumeroEspecialEComprimentoMinimo() {
        assertTrue(validarSenha("Abcdefg1!"));
        assertFalse(validarSenha("abcdefg1!"));
        assertFalse(validarSenha("Abcdefgh!"));
        assertFalse(validarSenha("Abcdefg1"));
        assertFalse(validarSenha("Ab1!"));
    }

    private boolean validarSenha(String senha) {
        try {
            SenhaUtil.validarPolitica(senha);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
