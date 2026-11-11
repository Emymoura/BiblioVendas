package com.bibliovendas.service;

import com.bibliovendas.model.Avaliacao;
import com.bibliovendas.model.Cartao;
import com.bibliovendas.model.Catalogo;
import com.bibliovendas.model.Cliente;
import com.bibliovendas.model.Livro;
import com.bibliovendas.model.ItemPedido;
import com.bibliovendas.model.Pagamento;
import com.bibliovendas.model.Pedido;
import com.bibliovendas.model.Pix;
import com.bibliovendas.model.PrestadorServico;
import com.bibliovendas.model.Boleto;
import com.bibliovendas.model.SenhaUtil;
import com.bibliovendas.model.StatusPedido;
import com.bibliovendas.model.TipoPedido;
import com.bibliovendas.persistence.MySqlBibliotecaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BibliotecaService {
    private final Map<Long, Cliente> clientes = new HashMap<>();
    private final Map<Long, PrestadorServico> prestadores = new HashMap<>();
    private final Catalogo catalogo = new Catalogo();
    private final Map<Long, Pedido> pedidos = new HashMap<>();
    private final MySqlBibliotecaRepository repositorio;
    private long nextId = 1L;

    public BibliotecaService() {
        this.repositorio = null;
    }

    public BibliotecaService(MySqlBibliotecaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public static BibliotecaService conectarAoMySql() {
        return new BibliotecaService(MySqlBibliotecaRepository.conectarConfigurado());
    }

    public Cliente cadastrarCliente(String nome, String email, String senha, String endereco) {
        SenhaUtil.validarPolitica(senha);
        if (repositorio != null) {
            return repositorio.cadastrarCliente(nome, email, senha, endereco);
        }
        Cliente cliente = new Cliente(nextId++, nome, email, senha, endereco);
        clientes.put(cliente.getId(), cliente);
        return cliente;
    }

    public PrestadorServico cadastrarPrestador(String nome, String email, String senha, String endereco) {
        SenhaUtil.validarPolitica(senha);
        if (repositorio != null) {
            return repositorio.cadastrarPrestador(nome, email, senha, endereco);
        }
        PrestadorServico prestador = new PrestadorServico(nextId++, nome, email, senha, endereco);
        prestadores.put(prestador.getId(), prestador);
        return prestador;
    }

    public Optional<Cliente> loginCliente(String email, String senha) {
        if (repositorio != null) {
            return repositorio.loginCliente(email, senha);
        }
        return clientes.values().stream()
                .filter(cliente -> cliente.getEmail().equalsIgnoreCase(email)
                        && cliente.autenticar(senha))
                .findFirst();
    }

    public Optional<PrestadorServico> loginPrestador(String email, String senha) {
        if (repositorio != null) {
            return repositorio.loginPrestador(email, senha);
        }
        return prestadores.values().stream()
                .filter(prestador -> prestador.getEmail().equalsIgnoreCase(email)
                        && prestador.autenticar(senha))
                .findFirst();
    }

    public Livro cadastrarLivro(String nome, String sinopse, String categoria, String genero,
                               String autor, String editora, int numeroPaginas, int anoPublicacao,
                               BigDecimal preco, int quantidade, boolean disponivel, boolean digital) {
        if (repositorio != null) {
            return repositorio.cadastrarLivro(nome, sinopse, categoria, genero, autor, editora,
                    numeroPaginas, anoPublicacao, preco, quantidade, disponivel, digital);
        }
        Livro livro = new Livro(nextId++, nome, sinopse, categoria, genero, autor, editora,
                numeroPaginas, anoPublicacao, preco, quantidade, disponivel, digital);
        catalogo.adicionarLivro(livro);
        return livro;
    }

    public List<Livro> buscarLivros(String termo) {
        if (repositorio != null) {
            return repositorio.buscarLivros(termo);
        }
        return catalogo.buscarPorTermo(termo);
    }

    public List<Livro> listarCatalogo() {
        if (repositorio != null) {
            return repositorio.listarLivros();
        }
        return catalogo.listarLivros();
    }

    public Optional<Livro> buscarLivro(Long livroId) {
        if (repositorio != null) {
            return repositorio.buscarLivro(livroId);
        }
        return catalogo.buscarPorId(livroId);
    }

    public Pedido efetuarCompra(Cliente cliente, Long livroId, int quantidade, Pagamento pagamento) {
        Map<Long, Integer> itens = new HashMap<>();
        itens.put(livroId, quantidade);
        return efetuarCompra(cliente, itens, pagamento);
    }

    public Pedido efetuarCompra(Cliente cliente, Map<Long, Integer> quantidades, Pagamento pagamento) {
        if (quantidades == null || quantidades.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um livro ao carrinho.");
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
            if (entrada.getKey() == null || entrada.getValue() == null || entrada.getValue() <= 0) {
                throw new IllegalArgumentException("Cada item deve informar um livro e uma quantidade positiva.");
            }
            Livro livro = buscarLivro(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));
            if (!livro.isDisponivel() || livro.getQuantidade() < entrada.getValue()) {
                throw new IllegalStateException("Estoque insuficiente para \"" + livro.getNome() + "\".");
            }
            itens.add(new ItemPedido(livro, entrada.getValue()));
        }
        if (repositorio != null) {
            return repositorio.registrarPedido(cliente, itens, pagamento,
                    TipoPedido.COMPRA, StatusPedido.CONFIRMADO);
        }

        Pedido pedido = new Pedido(nextId++, cliente, TipoPedido.COMPRA, pagamento);
        for (ItemPedido item : itens) {
            Livro livro = item.getLivro();
            pedido.adicionarItem(livro, item.getQuantidade(),
                    item.getNomeLivro(), item.getPrecoUnitario());
            livro.reduzirQuantidade(item.getQuantidade());
        }
        pedido.setStatus(StatusPedido.CONFIRMADO);
        cliente.adicionarPedido(pedido);
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public Pedido efetuarAluguel(Cliente cliente, Long livroId, int quantidade, Pagamento pagamento) {
        if (quantidade != 1) {
            throw new IllegalArgumentException("Cada aluguel deve conter exatamente um livro.");
        }
        if (repositorio != null) {
            Livro livro = repositorio.buscarLivro(livroId)
                    .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));
            if (!livro.isDigital()) {
                throw new IllegalStateException("Aluguel somente para livros digitais.");
            }
            if (!livro.isDisponivel()) {
                throw new IllegalStateException("Aluguel inválido.");
            }
            return repositorio.registrarPedido(cliente, livro, quantidade, pagamento,
                    TipoPedido.ALUGUEL, StatusPedido.ATIVO);
        }
        Livro livro = catalogo.buscarPorId(livroId)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));

        if (!livro.isDigital()) {
            throw new IllegalStateException("Aluguel somente para livros digitais.");
        }
        if (!livro.isDisponivel() || quantidade <= 0) {
            throw new IllegalStateException("Aluguel inválido.");
        }
        atualizarAlugueisExpirados(cliente);
        boolean aluguelAtivo = cliente.getHistoricoPedidos().stream()
                .filter(pedido -> pedido.getTipoPedido() == TipoPedido.ALUGUEL
                        && pedido.getStatus() == StatusPedido.ATIVO)
                .flatMap(pedido -> pedido.getItens().stream())
                .anyMatch(item -> Objects.equals(item.getLivro().getId(), livroId));
        if (aluguelAtivo) {
            throw new IllegalStateException("Você já possui um aluguel ativo deste livro.");
        }

        Pedido pedido = new Pedido(nextId++, cliente, TipoPedido.ALUGUEL, pagamento);
        pedido.adicionarItem(livro, quantidade);
        pedido.setStatus(StatusPedido.ATIVO);
        cliente.adicionarPedido(pedido);
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public void removerOuDesativarLivro(Long livroId, boolean remover) {
        if (repositorio != null) {
            if (remover) {
                repositorio.removerLivro(livroId);
            } else {
                repositorio.desativarLivro(livroId);
            }
            return;
        }
        Livro livro = catalogo.buscarPorId(livroId)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));

        if (remover) {
            catalogo.removerLivro(livroId);
        } else {
            livro.setDisponivel(false);
        }
    }

    public void atualizarStatusPedido(Long pedidoId, StatusPedido status) {
        if (repositorio != null) {
            repositorio.atualizarStatusPedido(pedidoId, status);
            return;
        }
        Pedido pedido = pedidos.get(pedidoId);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido não encontrado.");
        }
        pedido.setStatus(status);
    }

    public List<Pedido> consultarHistoricoCliente(Long clienteId) {
        if (repositorio != null) {
            return repositorio.consultarHistoricoCliente(clienteId);
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        atualizarAlugueisExpirados(cliente);
        return new ArrayList<>(cliente.getHistoricoPedidos());
    }

    public List<Pedido> consultarHistoricoCliente(Long clienteId, int pagina, int limite, TipoPedido tipo) {
        if (pagina < 1 || limite < 1 || limite > 50) {
            throw new IllegalArgumentException("A página deve ser positiva e o limite deve estar entre 1 e 50.");
        }
        if (repositorio != null) {
            return repositorio.consultarHistoricoCliente(clienteId, pagina, limite, tipo);
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        atualizarAlugueisExpirados(cliente);
        return cliente.getHistoricoPedidos().stream()
                .filter(pedido -> tipo == null || pedido.getTipoPedido() == tipo)
                .sorted(Comparator.comparing(Pedido::getData).reversed()
                        .thenComparing(Pedido::getId, Comparator.reverseOrder()))
                .skip((long) (pagina - 1) * limite)
                .limit(limite)
                .collect(Collectors.toList());
    }

    public int contarPedidosCliente(Long clienteId, TipoPedido tipo) {
        if (repositorio != null) {
            return repositorio.contarPedidosCliente(clienteId, tipo);
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        atualizarAlugueisExpirados(cliente);
        return (int) cliente.getHistoricoPedidos().stream()
                .filter(pedido -> tipo == null || pedido.getTipoPedido() == tipo)
                .count();
    }

    public void reembolsarPedido(Long clienteId, Long pedidoId) {
        if (repositorio != null) {
            repositorio.reembolsarPedido(clienteId, pedidoId);
            return;
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        Pedido pedido = cliente.getHistoricoPedidos().stream()
                .filter(item -> Objects.equals(item.getId(), pedidoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if (pedido.getTipoPedido() != TipoPedido.COMPRA) {
            throw new IllegalStateException("Somente compras podem ser reembolsadas.");
        }
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new IllegalStateException("Este pedido não está disponível para reembolso.");
        }
        if (pedido.getData().isBefore(LocalDate.now().minusDays(7))) {
            throw new IllegalStateException("O prazo de reembolso de 7 dias terminou.");
        }
        pedido.setStatus(StatusPedido.REEMBOLSADO);
        pedido.getItens().forEach(item -> {
            Livro livro = item.getLivro();
            livro.setQuantidade(livro.getQuantidade() + item.getQuantidade());
            livro.setDisponivel(true);
        });
    }

    private void atualizarAlugueisExpirados(Cliente cliente) {
        LocalDate hoje = LocalDate.now();
        cliente.getHistoricoPedidos().stream()
                .filter(pedido -> pedido.getTipoPedido() == TipoPedido.ALUGUEL
                        && pedido.getStatus() == StatusPedido.ATIVO
                        && pedido.getDataFimAluguel() != null
                        && !hoje.isBefore(pedido.getDataFimAluguel()))
                .forEach(pedido -> pedido.setStatus(StatusPedido.EXPIRADO));
    }

    public void atualizarDadosCliente(Long clienteId, String nome, String email, String endereco) {
        if (repositorio != null) {
            repositorio.atualizarCliente(clienteId, nome, email, endereco);
            return;
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        cliente.atualizarDados(nome, email, endereco);
    }

    public void avaliarExperiencia(Long clienteId, Long livroId, int nota, String comentario) {
        if (repositorio != null) {
            if (nota < 0 || nota > 5) {
                throw new IllegalArgumentException("A nota deve estar entre 0 e 5.");
            }
            if (repositorio.buscarCliente(clienteId) == null) {
                throw new IllegalArgumentException("Cliente não encontrado.");
            }
            if (!repositorio.clientePodeAvaliar(clienteId, livroId)) {
                throw new IllegalStateException("Avalie somente livros que você já comprou ou alugou.");
            }
            repositorio.avaliarExperiencia(clienteId, livroId, nota, comentario);
            return;
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        if (nota < 0 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 5.");
        }
        boolean possuiPedido = cliente.getHistoricoPedidos().stream()
                .filter(pedido -> pedido.getStatus() != StatusPedido.PENDENTE
                        && pedido.getStatus() != StatusPedido.CANCELADO)
                .flatMap(pedido -> pedido.getItens().stream())
                .anyMatch(item -> Objects.equals(item.getLivro().getId(), livroId));
        if (!possuiPedido) {
            throw new IllegalStateException("Avalie somente livros que você já comprou ou alugou.");
        }
        cliente.adicionarAvaliacao(new Avaliacao(
                livroId, nota, comentario, cliente.getNome(), java.time.LocalDate.now(), false));
    }

    public List<Avaliacao> buscarAvaliacoesLivro(Long livroId) {
        if (repositorio != null) {
            return repositorio.buscarAvaliacoesLivro(livroId);
        }
        return clientes.values().stream()
                .flatMap(cliente -> cliente.getAvaliacoes().stream())
                .filter(avaliacao -> livroId == null || Objects.equals(avaliacao.getLivroId(), livroId))
                .collect(Collectors.toList());
    }

    public List<Avaliacao> consultarAvaliacoes(Long clienteId) {
        if (repositorio != null) {
            if (repositorio.buscarCliente(clienteId) == null) {
                throw new IllegalArgumentException("Cliente não encontrado.");
            }
            return repositorio.buscarAvaliacoes(clienteId);
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        return new ArrayList<>(cliente.getAvaliacoes());
    }

    public boolean acessarLivroDigital(Long clienteId, Long livroId) {
        if (repositorio != null) {
            return repositorio.podeAcessarLivroDigital(clienteId, livroId);
        }
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            return false;
        }

        return cliente.getHistoricoPedidos().stream()
                .filter(pedido -> pedido.getTipoPedido() == TipoPedido.ALUGUEL || pedido.getTipoPedido() == TipoPedido.COMPRA)
                .anyMatch(pedido -> pedido.getItens().stream()
                        .anyMatch(item -> Objects.equals(item.getLivro().getId(), livroId)
                                && (pedido.getStatus() == StatusPedido.ATIVO || pedido.getStatus() == StatusPedido.CONFIRMADO)));
    }

    public void cadastrarCartao(Cliente cliente, BigDecimal valor, String numero, String titular) {
        if (repositorio != null) {
            String digitos = numero == null ? "" : numero.replaceAll("\\D", "");
            if (digitos.length() < 4) {
                throw new IllegalArgumentException("O número do cartão deve ter pelo menos 4 dígitos.");
            }
            repositorio.cadastrarCartao(cliente.getId(), titular,
                    digitos.substring(digitos.length() - 4));
            return;
        }
        new Cartao(valor, numero, titular);
    }

    public void cadastrarPix(Cliente cliente, BigDecimal valor, String chavePix) {
        new Pix(valor, chavePix);
    }

    public void cadastrarBoleto(Cliente cliente, BigDecimal valor, String codigoBarra) {
        new Boleto(valor, codigoBarra);
    }

    public List<Livro> buscarPorCategoria(String categoria) {
        if (repositorio != null) {
            return repositorio.buscarPorCategoria(categoria);
        }
        return catalogo.listarLivros().stream()
                .filter(livro -> livro.getCategoria().equalsIgnoreCase(categoria))
                .collect(Collectors.toList());
    }
}
