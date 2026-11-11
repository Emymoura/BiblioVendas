package com.bibliovendas.model;

import java.math.BigDecimal;

public class ItemPedido {
    private final Livro livro;
    private final int quantidade;

    public ItemPedido(Livro livro, int quantidade) {
        this.livro = livro;
        this.quantidade = quantidade;
    }

    public Livro getLivro() {
        return livro;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorTotal() {
        return livro.getPreco().multiply(BigDecimal.valueOf(quantidade));
    }
}
