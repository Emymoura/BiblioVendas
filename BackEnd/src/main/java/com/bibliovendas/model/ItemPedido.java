package com.bibliovendas.model;

import java.math.BigDecimal;

public class ItemPedido {
    private final Livro livro;
    private final int quantidade;
    private final String nomeLivro;
    private final BigDecimal precoUnitario;

    public ItemPedido(Livro livro, int quantidade) {
        this(livro, quantidade, livro.getNome(), livro.getPreco());
    }

    public ItemPedido(Livro livro, int quantidade, String nomeLivro, BigDecimal precoUnitario) {
        this.livro = livro;
        this.quantidade = quantidade;
        this.nomeLivro = nomeLivro;
        this.precoUnitario = precoUnitario;
    }

    public Livro getLivro() {
        return livro;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getNomeLivro() {
        return nomeLivro;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
