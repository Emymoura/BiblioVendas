package com.bibliovendas.model;

import java.math.BigDecimal;

public class Cartao extends Pagamento {
    private final String numero;
    private final String titular;

    public Cartao(BigDecimal valor, String numero, String titular) {
        super(valor, "CARTAO");
        this.numero = numero;
        this.titular = titular;
    }

    public String getNumero() {
        return numero;
    }

    public String getTitular() {
        return titular;
    }
}
