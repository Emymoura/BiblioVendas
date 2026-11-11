package com.bibliovendas.model;

import java.math.BigDecimal;

public class Livro {
    private Long id;
    private String nome;
    private String sinopse;
    private String categoria;
    private String genero;
    private String autor;
    private String editora;
    private int numeroPaginas;
    private int anoPublicacao;
    private BigDecimal preco;
    private int quantidade;
    private boolean disponivel;
    private boolean digital;

    public Livro(Long id, String nome, String sinopse, String categoria, String genero,
                String autor, String editora, int numeroPaginas, int anoPublicacao,
                BigDecimal preco, int quantidade, boolean disponivel, boolean digital) {
        this.id = id;
        this.nome = nome;
        this.sinopse = sinopse;
        this.categoria = categoria;
        this.genero = genero;
        this.autor = autor;
        this.editora = editora;
        this.numeroPaginas = numeroPaginas;
        this.anoPublicacao = anoPublicacao;
        this.preco = preco;
        this.quantidade = quantidade;
        this.disponivel = disponivel;
        this.digital = digital;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSinopse() {
        return sinopse;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getGenero() {
        return genero;
    }

    public String getAutor() {
        return autor;
    }

    public String getEditora() {
        return editora;
    }

    public int getNumeroPaginas() {
        return numeroPaginas;
    }

    public int getAnoPublicacao() {
        return anoPublicacao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public boolean isDisponivel() {
        return disponivel && quantidade > 0;
    }

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }

    public boolean isDigital() {
        return digital;
    }

    public void reduzirQuantidade(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        this.quantidade = Math.max(0, this.quantidade - unidades);
        this.disponivel = this.quantidade > 0;
    }

    public void aumentarQuantidade(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        this.quantidade += unidades;
        this.disponivel = true;
    }
}
