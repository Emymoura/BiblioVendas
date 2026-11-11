package com.bibliovendas.model;

import java.math.BigDecimal;

public abstract class Pagamento {
    private final BigDecimal valor;
    private final String tipo;

    protected Pagamento(BigDecimal valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getTipo() {
        return tipo;
    }
}
