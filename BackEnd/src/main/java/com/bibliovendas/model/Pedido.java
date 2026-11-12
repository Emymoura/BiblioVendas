package com.bibliovendas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido {
    public static final int DURACAO_ALUGUEL_DIAS = 14;

    private final Long id;
    private final Cliente cliente;
    private final LocalDate data;
    private final LocalDate dataFimAluguel;
    private StatusPedido status;
    private final TipoPedido tipoPedido;
    private final Pagamento pagamento;
    private final List<ItemPedido> itens = new ArrayList<>();

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento) {
        this(id, cliente, tipoPedido, pagamento, LocalDate.now(), StatusPedido.PENDENTE);
    }

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento,
                  LocalDate data, StatusPedido status) {
        this(id, cliente, tipoPedido, pagamento, data, status,
                tipoPedido == TipoPedido.ALUGUEL ? data.plusDays(DURACAO_ALUGUEL_DIAS) : null);
    }

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento,
                  LocalDate data, StatusPedido status, int duracaoAluguelDias) {
        this(id, cliente, tipoPedido, pagamento, data, status,
                tipoPedido == TipoPedido.ALUGUEL ? data.plusDays(duracaoAluguelDias) : null);
    }

    public Pedido(Long id, Cliente cliente, TipoPedido tipoPedido, Pagamento pagamento,
                  LocalDate data, StatusPedido status, LocalDate dataFimAluguel) {
        this.id = id;
        this.cliente = cliente;
        this.data = data;
        this.dataFimAluguel = dataFimAluguel;
        this.tipoPedido = tipoPedido;
        this.pagamento = pagamento;
        this.status = status;
    }

    public void adicionarItem(Livro livro, int quantidade) {
        adicionarItem(livro, quantidade, livro.getNome(), livro.getPreco());
    }

    public void adicionarItem(Livro livro, int quantidade, String nomeLivro, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        itens.add(new ItemPedido(livro, quantidade, nomeLivro, precoUnitario));
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

    public LocalDate getDataFimAluguel() {
        return dataFimAluguel;
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
