package com.bibliovendas.model;

import java.math.BigDecimal;

public class Pix extends Pagamento {
    private final String chavePix;

    public Pix(BigDecimal valor, String chavePix) {
        super(valor, "PIX");
        this.chavePix = chavePix;
    }

    public String getChavePix() {
        return chavePix;
    }
}
