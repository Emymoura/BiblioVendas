package com.bibliovendas;

import com.bibliovendas.model.SenhaUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaUtilTest {
    @Test
    void deveArmazenarSenhaComHashEValidarCredencial() {
        String hash = SenhaUtil.gerarHash("senha-segura");

        assertNotEquals("senha-segura", hash);
        assertTrue(SenhaUtil.verificar("senha-segura", hash));
        assertFalse(SenhaUtil.verificar("senha-incorreta", hash));
    }
}
