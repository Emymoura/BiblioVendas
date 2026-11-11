package com.bibliovendas.model;

import java.math.BigDecimal;

public class Boleto extends Pagamento {
    private final String codigoBarra;

    public Boleto(BigDecimal valor, String codigoBarra) {
        super(valor, "BOLETO");
        this.codigoBarra = codigoBarra;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }
}
