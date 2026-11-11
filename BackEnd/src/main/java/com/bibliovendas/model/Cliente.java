package com.bibliovendas.model;

import java.util.ArrayList;
import java.util.List;

public class Cliente extends Usuario {
    private final List<Pedido> historicoPedidos = new ArrayList<>();
    private final List<Avaliacao> avaliacoes = new ArrayList<>();

    public Cliente(Long id, String nome, String email, String senha, String endereco) {
        super(id, nome, email, senha, endereco);
    }

    public Cliente(Long id, String nome, String email, String senha, String endereco,
                   boolean senhaJaCriptografada) {
        super(id, nome, email, senha, endereco, senhaJaCriptografada);
    }

    public List<Pedido> getHistoricoPedidos() {
        return historicoPedidos;
    }

    public void adicionarPedido(Pedido pedido) {
        historicoPedidos.add(pedido);
    }

    public List<Avaliacao> getAvaliacoes() {
        return avaliacoes;
    }

    public void adicionarAvaliacao(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
    }
}
