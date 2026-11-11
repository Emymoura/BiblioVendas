package com.bibliovendas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido {
    private final Long id;
    private final Cliente cliente;
    private final LocalDate data;
    private StatusPedido status;
    private final TipoPedido tipoPedido;
    private final Pagamento pagamento;
    private final List<ItemPedido> itens = new ArrayList<>();

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento) {
        this(id, cliente, tipoPedido, pagamento, LocalDate.now(), StatusPedido.PENDENTE);
    }

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento,
                  LocalDate data, StatusPedido status) {
        this.id = id;
        this.cliente = cliente;
        this.data = data;
        this.tipoPedido = tipoPedido;
        this.pagamento = pagamento;
        this.status = status;
    }

    public void adicionarItem(Livro livro, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        itens.add(new ItemPedido(livro, quantidade));
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDate getData() {
        return data;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public TipoPedido getTipoPedido() {
        return tipoPedido;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public BigDecimal getValorTotal() {
        return itens.stream()
                .map(ItemPedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
