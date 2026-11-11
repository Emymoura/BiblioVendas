package com.bibliovendas.persistence;

import com.bibliovendas.model.Avaliacao;
import com.bibliovendas.model.Boleto;
import com.bibliovendas.model.Cartao;
import com.bibliovendas.model.Cliente;
import com.bibliovendas.model.Livro;
import com.bibliovendas.model.Pagamento;
import com.bibliovendas.model.Pedido;
import com.bibliovendas.model.Pix;
import com.bibliovendas.model.PrestadorServico;
import com.bibliovendas.model.SenhaUtil;
import com.bibliovendas.model.StatusPedido;
import com.bibliovendas.model.TipoPedido;
import com.bibliovendas.model.Usuario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MySqlBibliotecaRepository {
    private final MySqlDatabase database;

    public MySqlBibliotecaRepository(MySqlDatabase database) {
        this.database = database;
        database.inicializarSchema();
    }

    public static MySqlBibliotecaRepository conectarConfigurado() {
        return new MySqlBibliotecaRepository(new MySqlDatabase());
    }

    public Cliente cadastrarCliente(String nome, String email, String senha, String endereco) {
        return (Cliente) cadastrarUsuario(nome, email, senha, endereco, "CLIENTE");
    }

    public PrestadorServico cadastrarPrestador(String nome, String email, String senha, String endereco) {
        return (PrestadorServico) cadastrarUsuario(nome, email, senha, endereco, "PRESTADOR");
    }

    private Usuario cadastrarUsuario(String nome, String email, String senha, String endereco, String tipo) {
        String sql = "INSERT INTO usuarios (nome, email, senha_hash, endereco, tipo) VALUES (?, ?, ?, ?, ?)";
        String senhaHash = SenhaUtil.gerarHash(senha);
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, nome);
            statement.setString(2, email);
            statement.setString(3, senhaHash);
            statement.setString(4, endereco);
            statement.setString(5, tipo);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("O MySQL não retornou o identificador do usuário.");
                }
                return criarUsuario(keys.getLong(1), nome, email, senhaHash, endereco, tipo);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível cadastrar o usuário. O e-mail pode já estar em uso.", ex);
        }
    }

    public Optional<Cliente> loginCliente(String email, String senha) {
        return buscarUsuarioPorEmail(email, "CLIENTE")
                .filter(cliente -> cliente.autenticar(senha))
                .map(usuario -> (Cliente) usuario);
    }

    public Optional<PrestadorServico> loginPrestador(String email, String senha) {
        return buscarUsuarioPorEmail(email, "PRESTADOR")
                .filter(usuario -> usuario.autenticar(senha))
                .map(usuario -> (PrestadorServico) usuario);
    }

    public Optional<Usuario> buscarUsuarioPorEmail(String email, String tipo) {
        String sql = "SELECT id, nome, email, senha_hash, endereco, tipo FROM usuarios "
                + "WHERE LOWER(email) = LOWER(?) AND tipo = ?";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, tipo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(criarUsuario(result.getLong("id"), result.getString("nome"),
                                result.getString("email"), result.getString("senha_hash"),
                                result.getString("endereco"), result.getString("tipo")))
                        : Optional.empty();
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível consultar o usuário.", ex);
        }
    }

    public Cliente buscarCliente(Long clienteId) {
        String sql = "SELECT id, nome, email, senha_hash, endereco FROM usuarios WHERE id = ? AND tipo = 'CLIENTE'";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Cliente(result.getLong("id"), result.getString("nome"),
                        result.getString("email"), result.getString("senha_hash"),
                        result.getString("endereco"), true);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível consultar o cliente.", ex);
        }
    }

    public void atualizarCliente(Long clienteId, String nome, String email, String endereco) {
        String sql = "UPDATE usuarios SET nome = ?, email = ?, endereco = ? WHERE id = ? AND tipo = 'CLIENTE'";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            statement.setString(2, email);
            statement.setString(3, endereco);
            statement.setLong(4, clienteId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Cliente não encontrado.");
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível atualizar o cliente.", ex);
        }
    }

    public Livro cadastrarLivro(String nome, String sinopse, String categoria, String genero,
                                String autor, String editora, int numeroPaginas, int anoPublicacao,
                                BigDecimal preco, int quantidade, boolean disponivel, boolean digital) {
        String sql = "INSERT INTO livros (nome, sinopse, categoria, genero, autor, editora, "
                + "numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherLivro(statement, nome, sinopse, categoria, genero, autor, editora,
                    numeroPaginas, anoPublicacao, preco, quantidade, disponivel, digital);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("O MySQL não retornou o identificador do livro.");
                }
                return new Livro(keys.getLong(1), nome, sinopse, categoria, genero, autor, editora,
                        numeroPaginas, anoPublicacao, preco, quantidade, disponivel, digital);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível cadastrar o livro.", ex);
        }
    }

    public List<Livro> listarLivros() {
        return consultarLivros("SELECT * FROM livros ORDER BY nome", null);
    }

    public Optional<Livro> buscarLivro(Long id) {
        List<Livro> livros = consultarLivros("SELECT * FROM livros WHERE id = ?", id);
        return livros.isEmpty() ? Optional.empty() : Optional.of(livros.get(0));
    }

    public List<Livro> buscarLivros(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarLivros();
        }
        String sql = "SELECT * FROM livros WHERE LOWER(nome) LIKE ? OR LOWER(autor) LIKE ? "
                + "OR LOWER(genero) LIKE ? OR LOWER(editora) LIKE ? OR LOWER(categoria) LIKE ? ORDER BY nome";
        String parametro = "%" + termo.toLowerCase(Locale.ROOT) + "%";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                statement.setString(i, parametro);
            }
            return mapearLivros(statement.executeQuery());
        } catch (SQLException ex) {
            throw erro("Não foi possível pesquisar o catálogo.", ex);
        }
    }

    public List<Livro> buscarPorCategoria(String categoria) {
        String sql = "SELECT * FROM livros WHERE LOWER(categoria) = LOWER(?) ORDER BY nome";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, categoria);
            return mapearLivros(statement.executeQuery());
        } catch (SQLException ex) {
            throw erro("Não foi possível consultar a categoria.", ex);
        }
    }

    public void removerLivro(Long livroId) {
        executarAtualizacao("DELETE FROM livros WHERE id = ?", livroId, "Livro não encontrado.");
    }

    public void desativarLivro(Long livroId) {
        executarAtualizacao("UPDATE livros SET disponivel = FALSE WHERE id = ?", livroId, "Livro não encontrado.");
    }

    public Pedido registrarPedido(Cliente cliente, Livro livro, int quantidade, Pagamento pagamento,
                                 TipoPedido tipoPedido, StatusPedido status) {
        String inserirPedido = "INSERT INTO pedidos (cliente_id, status, tipo_pedido, pagamento_tipo, pagamento_valor) "
                + "VALUES (?, ?, ?, ?, ?)";
        String inserirItem = "INSERT INTO itens_pedido (pedido_id, livro_id, quantidade, preco_unitario) "
                + "VALUES (?, ?, ?, ?)";
        String reduzirEstoque = "UPDATE livros SET quantidade = quantidade - ?, "
                + "disponivel = quantidade > 0 WHERE id = ? AND disponivel = TRUE AND quantidade >= ?";

        try (Connection connection = database.conectar()) {
            connection.setAutoCommit(false);
            try {
                if (tipoPedido == TipoPedido.COMPRA) {
                    try (PreparedStatement estoque = connection.prepareStatement(reduzirEstoque)) {
                        estoque.setInt(1, quantidade);
                        estoque.setLong(2, livro.getId());
                        estoque.setInt(3, quantidade);
                        if (estoque.executeUpdate() == 0) {
                            throw new IllegalStateException("Livro indisponível para compra.");
                        }
                    }
                }

                long pedidoId;
                try (PreparedStatement pedido = connection.prepareStatement(
                        inserirPedido, Statement.RETURN_GENERATED_KEYS)) {
                    pedido.setLong(1, cliente.getId());
                    pedido.setString(2, status.name());
                    pedido.setString(3, tipoPedido.name());
                    pedido.setString(4, pagamento.getTipo());
                    pedido.setBigDecimal(5, pagamento.getValor());
                    pedido.executeUpdate();
                    try (ResultSet keys = pedido.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("O MySQL não retornou o identificador do pedido.");
                        }
                        pedidoId = keys.getLong(1);
                    }
                }

                try (PreparedStatement item = connection.prepareStatement(inserirItem)) {
                    item.setLong(1, pedidoId);
                    item.setLong(2, livro.getId());
                    item.setInt(3, quantidade);
                    item.setBigDecimal(4, livro.getPreco());
                    item.executeUpdate();
                }
                connection.commit();

                if (tipoPedido == TipoPedido.COMPRA) {
                    Livro atualizado = buscarLivro(livro.getId())
                            .orElseThrow(() -> new IllegalStateException("Livro não encontrado após a compra."));
                    livro.setQuantidade(atualizado.getQuantidade());
                    livro.setDisponivel(atualizado.isDisponivel());
                }

                Pedido resultado = new Pedido(pedidoId, cliente, tipoPedido, pagamento);
                resultado.adicionarItem(livro, quantidade);
                resultado.setStatus(status);
                cliente.adicionarPedido(resultado);
                return resultado;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível registrar o pedido.", ex);
        }
    }

    public void atualizarStatusPedido(Long pedidoId, StatusPedido status) {
        executarAtualizacao("UPDATE pedidos SET status = ? WHERE id = ?", status.name(), pedidoId,
                "Pedido não encontrado.");
    }

    public List<Pedido> consultarHistoricoCliente(Long clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }

        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT id, data_criacao, status, tipo_pedido, pagamento_tipo, pagamento_valor "
                + "FROM pedidos WHERE cliente_id = ? ORDER BY data_criacao DESC, id DESC";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long pedidoId = result.getLong("id");
                    TipoPedido tipo = TipoPedido.valueOf(result.getString("tipo_pedido"));
                    StatusPedido status = StatusPedido.valueOf(result.getString("status"));
                    BigDecimal valor = result.getBigDecimal("pagamento_valor");
                    Pagamento pagamento = criarPagamento(result.getString("pagamento_tipo"), valor);
                    LocalDate data = result.getTimestamp("data_criacao").toLocalDateTime().toLocalDate();
                    Pedido pedido = new Pedido(pedidoId, cliente, tipo, pagamento, data, status);
                    carregarItens(connection, pedido);
                    pedidos.add(pedido);
                    cliente.adicionarPedido(pedido);
                }
            }
            return pedidos;
        } catch (SQLException ex) {
            throw erro("Não foi possível carregar o histórico de pedidos.", ex);
        }
    }

    public void avaliarExperiencia(Long clienteId, Long livroId, int nota, String comentario) {
        String sql = "INSERT INTO avaliacoes (cliente_id, livro_id, nota, comentario) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            statement.setLong(2, livroId);
            statement.setInt(3, nota);
            statement.setString(4, comentario);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw erro("Não foi possível salvar a avaliação.", ex);
        }
    }

    public List<Avaliacao> buscarAvaliacoes(Long clienteId) {
        String sql = "SELECT livro_id, nota, comentario FROM avaliacoes WHERE cliente_id = ? ORDER BY id";
        List<Avaliacao> avaliacoes = new ArrayList<>();
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    avaliacoes.add(new Avaliacao(result.getLong("livro_id"),
                            result.getInt("nota"), result.getString("comentario")));
                }
            }
            return avaliacoes;
        } catch (SQLException ex) {
            throw erro("Não foi possível carregar as avaliações.", ex);
        }
    }

    public boolean podeAcessarLivroDigital(Long clienteId, Long livroId) {
        String sql = "SELECT COUNT(*) FROM pedidos p JOIN itens_pedido i ON i.pedido_id = p.id "
                + "JOIN livros l ON l.id = i.livro_id WHERE p.cliente_id = ? AND l.id = ? AND l.digital = TRUE "
                + "AND ((p.tipo_pedido = 'ALUGUEL' AND p.status = 'ATIVO') "
                + "OR (p.tipo_pedido = 'COMPRA' AND p.status = 'CONFIRMADO'))";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            statement.setLong(2, livroId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível verificar o acesso ao livro digital.", ex);
        }
    }

    public void cadastrarCartao(Long clienteId, String titular, String ultimosQuatro) {
        String sql = "INSERT INTO cartoes_cliente (cliente_id, titular, ultimos_quatro) VALUES (?, ?, ?)";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            statement.setString(2, titular);
            statement.setString(3, ultimosQuatro);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw erro("Não foi possível cadastrar o cartão.", ex);
        }
    }

    private void carregarItens(Connection connection, Pedido pedido) throws SQLException {
        String sql = "SELECT livro_id, quantidade FROM itens_pedido WHERE pedido_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pedido.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Livro livro = buscarLivro(result.getLong("livro_id"))
                            .orElseThrow(() -> new IllegalStateException("Livro do pedido não encontrado."));
                    pedido.adicionarItem(livro, result.getInt("quantidade"));
                }
            }
        }
    }

    private List<Livro> consultarLivros(String sql, Long id) {
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (id != null) {
                statement.setLong(1, id);
            }
            return mapearLivros(statement.executeQuery());
        } catch (SQLException ex) {
            throw erro("Não foi possível consultar o catálogo.", ex);
        }
    }

    private List<Livro> mapearLivros(ResultSet result) throws SQLException {
        List<Livro> livros = new ArrayList<>();
        try (ResultSet rows = result) {
            while (rows.next()) {
                livros.add(new Livro(rows.getLong("id"), rows.getString("nome"),
                        rows.getString("sinopse"), rows.getString("categoria"),
                        rows.getString("genero"), rows.getString("autor"), rows.getString("editora"),
                        rows.getInt("numero_paginas"), rows.getInt("ano_publicacao"),
                        rows.getBigDecimal("preco"), rows.getInt("quantidade"),
                        rows.getBoolean("disponivel"), rows.getBoolean("digital")));
            }
        }
        return livros;
    }

    private void preencherLivro(PreparedStatement statement, String nome, String sinopse, String categoria,
                                String genero, String autor, String editora, int numeroPaginas,
                                int anoPublicacao, BigDecimal preco, int quantidade,
                                boolean disponivel, boolean digital) throws SQLException {
        statement.setString(1, nome);
        statement.setString(2, sinopse);
        statement.setString(3, categoria);
        statement.setString(4, genero);
        statement.setString(5, autor);
        statement.setString(6, editora);
        statement.setInt(7, numeroPaginas);
        statement.setInt(8, anoPublicacao);
        statement.setBigDecimal(9, preco);
        statement.setInt(10, quantidade);
        statement.setBoolean(11, disponivel);
        statement.setBoolean(12, digital);
    }

    private Usuario criarUsuario(Long id, String nome, String email, String senhaHash,
                                 String endereco, String tipo) {
        if ("CLIENTE".equals(tipo)) {
            return new Cliente(id, nome, email, senhaHash, endereco, true);
        }
        return new PrestadorServico(id, nome, email, senhaHash, endereco, true);
    }

    private Pagamento criarPagamento(String tipo, BigDecimal valor) {
        if ("CARTAO".equalsIgnoreCase(tipo)) {
            return new Cartao(valor, "", "");
        }
        if ("PIX".equalsIgnoreCase(tipo)) {
            return new Pix(valor, "");
        }
        return new Boleto(valor, "");
    }

    private void executarAtualizacao(String sql, Object parametro, String mensagem) {
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parametro);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException(mensagem);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível atualizar o banco de dados.", ex);
        }
    }

    private void executarAtualizacao(String sql, Object primeiro, Object segundo, String mensagem) {
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, primeiro);
            statement.setObject(2, segundo);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException(mensagem);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível atualizar o banco de dados.", ex);
        }
    }

    private PersistenceException erro(String mensagem, SQLException causa) {
        return new PersistenceException(mensagem, causa);
    }
}
