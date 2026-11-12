package com.bibliovendas.persistence;

import com.bibliovendas.model.Avaliacao;
import com.bibliovendas.model.Administrador;
import com.bibliovendas.model.Boleto;
import com.bibliovendas.model.Cartao;
import com.bibliovendas.model.Cliente;
import com.bibliovendas.model.Livro;
import com.bibliovendas.model.ItemPedido;
import com.bibliovendas.model.Pagamento;
import com.bibliovendas.model.Pedido;
import com.bibliovendas.model.Pix;
import com.bibliovendas.model.Pedido;
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
import java.util.ArrayList;
import java.util.Collections;
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

    public void garantirAdministrador(String nome, String email, String senha) {
        Optional<Usuario> administrador = buscarUsuarioPorEmail(email, "ADMIN");
        if (administrador.isPresent()) {
            return;
        }
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT tipo FROM usuarios WHERE LOWER(email) = LOWER(?)")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new IllegalStateException(
                            "O e-mail configurado para ADMIN já pertence a outro perfil.");
                }
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível verificar a conta administrativa.", ex);
        }
        cadastrarUsuario(nome, email, senha, "Administração", "ADMIN");
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

    public Optional<Administrador> loginAdministrador(String email, String senha) {
        return buscarUsuarioPorEmail(email, "ADMIN")
                .filter(usuario -> usuario.autenticar(senha))
                .map(usuario -> (Administrador) usuario);
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

    public void atualizarEstoqueLivro(Long livroId, int quantidade, boolean disponivel) {
        try (Connection connection = database.conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement localizar = connection.prepareStatement(
                        "SELECT id FROM livros WHERE id = ? FOR UPDATE")) {
                    localizar.setLong(1, livroId);
                    try (ResultSet result = localizar.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Livro não encontrado.");
                        }
                    }
                }
                try (PreparedStatement atualizar = connection.prepareStatement(
                        "UPDATE livros SET quantidade = ?, disponivel = ? WHERE id = ?")) {
                    atualizar.setInt(1, quantidade);
                    atualizar.setBoolean(2, disponivel && quantidade > 0);
                    atualizar.setLong(3, livroId);
                    atualizar.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível atualizar o estoque do livro.", ex);
        }
    }

    public String obterConfiguracao(String chave, String padrao) {
        String sql = "SELECT valor FROM configuracoes_sistema WHERE chave = ?";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, chave);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("valor") : padrao;
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível carregar as configurações do sistema.", ex);
        }
    }

    public void salvarConfiguracoes(int duracaoAluguelDias, String formasPagamento) {
        String sql = "INSERT INTO configuracoes_sistema (chave, valor) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE valor = VALUES(valor)";
        try (Connection connection = database.conectar()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "duracao_aluguel_dias");
                statement.setString(2, String.valueOf(duracaoAluguelDias));
                statement.addBatch();
                statement.setString(1, "formas_pagamento");
                statement.setString(2, formasPagamento);
                statement.addBatch();
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível salvar as configurações do sistema.", ex);
        }
    }

    public Pedido registrarPedido(Cliente cliente, Livro livro, int quantidade, Pagamento pagamento,
                                 TipoPedido tipoPedido, StatusPedido status) {
        return registrarPedido(cliente, Collections.singletonList(new ItemPedido(livro, quantidade)),
                pagamento, tipoPedido, status, Pedido.DURACAO_ALUGUEL_DIAS);
    }

    public Pedido registrarPedido(Cliente cliente, Livro livro, int quantidade, Pagamento pagamento,
                                 TipoPedido tipoPedido, StatusPedido status, int duracaoAluguelDias) {
        return registrarPedido(cliente, Collections.singletonList(new ItemPedido(livro, quantidade)),
                pagamento, tipoPedido, status, duracaoAluguelDias);
    }

    public Pedido registrarPedido(Cliente cliente, List<ItemPedido> itens, Pagamento pagamento,
                                 TipoPedido tipoPedido, StatusPedido status) {
        return registrarPedido(cliente, itens, pagamento, tipoPedido, status,
                Pedido.DURACAO_ALUGUEL_DIAS);
    }

    public Pedido registrarPedido(Cliente cliente, List<ItemPedido> itens, Pagamento pagamento,
                                 TipoPedido tipoPedido, StatusPedido status, int duracaoAluguelDias) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos um livro.");
        }
        if (tipoPedido == TipoPedido.ALUGUEL && itens.size() != 1) {
            throw new IllegalArgumentException("Cada aluguel deve conter exatamente um livro.");
        }
        String inserirPedido = "INSERT INTO pedidos (cliente_id, status, tipo_pedido, pagamento_tipo, "
                + "pagamento_valor, data_fim_aluguel) VALUES (?, ?, ?, ?, ?, ?)";
        String inserirItem = "INSERT INTO itens_pedido "
                + "(pedido_id, livro_id, nome_livro, quantidade, preco_unitario) VALUES (?, ?, ?, ?, ?)";
        String reduzirEstoque = "UPDATE livros SET quantidade = quantidade - ?, "
                + "disponivel = quantidade > 0 WHERE id = ? AND disponivel = TRUE AND quantidade >= ?";

        try (Connection connection = database.conectar()) {
            connection.setAutoCommit(false);
            try {
                if (tipoPedido == TipoPedido.COMPRA) {
                    try (PreparedStatement estoque = connection.prepareStatement(reduzirEstoque)) {
                        for (ItemPedido item : itens) {
                            estoque.setInt(1, item.getQuantidade());
                            estoque.setLong(2, item.getLivro().getId());
                            estoque.setInt(3, item.getQuantidade());
                            if (estoque.executeUpdate() == 0) {
                                throw new IllegalStateException(
                                        "Estoque insuficiente para \"" + item.getNomeLivro() + "\".");
                            }
                        }
                    }
                } else {
                    validarAluguelNoBanco(connection, cliente.getId(), itens.get(0).getLivro().getId());
                }

                long pedidoId;
                try (PreparedStatement pedido = connection.prepareStatement(
                        inserirPedido, Statement.RETURN_GENERATED_KEYS)) {
                    pedido.setLong(1, cliente.getId());
                    pedido.setString(2, status.name());
                    pedido.setString(3, tipoPedido.name());
                    pedido.setString(4, pagamento.getTipo());
                    pedido.setBigDecimal(5, pagamento.getValor());
                    if (tipoPedido == TipoPedido.ALUGUEL) {
                        pedido.setDate(6, java.sql.Date.valueOf(
                                    LocalDate.now().plusDays(duracaoAluguelDias)));
                    } else {
                        pedido.setNull(6, java.sql.Types.DATE);
                    }
                    pedido.executeUpdate();
                    try (ResultSet keys = pedido.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("O MySQL não retornou o identificador do pedido.");
                        }
                        pedidoId = keys.getLong(1);
                    }
                }

                try (PreparedStatement item = connection.prepareStatement(inserirItem)) {
                    for (ItemPedido livroPedido : itens) {
                        item.setLong(1, pedidoId);
                        item.setLong(2, livroPedido.getLivro().getId());
                        item.setString(3, livroPedido.getNomeLivro());
                        item.setInt(4, livroPedido.getQuantidade());
                        item.setBigDecimal(5, livroPedido.getPrecoUnitario());
                        item.addBatch();
                    }
                    item.executeBatch();
                }
                connection.commit();

                if (tipoPedido == TipoPedido.COMPRA) {
                    for (ItemPedido item : itens) {
                        Livro atualizado = buscarLivro(item.getLivro().getId())
                                .orElseThrow(() -> new IllegalStateException("Livro não encontrado após a compra."));
                        item.getLivro().setQuantidade(atualizado.getQuantidade());
                        item.getLivro().setDisponivel(atualizado.isDisponivel());
                    }
                }

                LocalDate data = LocalDate.now();
                Pedido resultado = new Pedido(pedidoId, cliente, tipoPedido, pagamento,
                        data, status, tipoPedido == TipoPedido.ALUGUEL
                                ? data.plusDays(duracaoAluguelDias) : null);
                for (ItemPedido item : itens) {
                    resultado.adicionarItem(item.getLivro(), item.getQuantidade(),
                            item.getNomeLivro(), item.getPrecoUnitario());
                }
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
        return consultarHistoricoCliente(clienteId, 1, Integer.MAX_VALUE, null);
    }

    public List<Pedido> consultarHistoricoCliente(Long clienteId, int pagina, int limite, TipoPedido tipo) {
        if (pagina < 1 || limite < 1) {
            throw new IllegalArgumentException("A página e o limite devem ser positivos.");
        }
        Cliente cliente = buscarCliente(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }

        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT id, data_criacao, data_fim_aluguel, status, tipo_pedido, "
                + "pagamento_tipo, pagamento_valor FROM pedidos WHERE cliente_id = ?"
                + (tipo == null ? "" : " AND tipo_pedido = ?")
                + " ORDER BY data_criacao DESC, id DESC LIMIT ? OFFSET ?";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            encerrarAlugueisExpirados(connection);
            statement.setLong(1, clienteId);
            int parametro = 2;
            if (tipo != null) {
                statement.setString(parametro++, tipo.name());
            }
            statement.setInt(parametro++, limite);
            statement.setLong(parametro, (long) (pagina - 1) * limite);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long pedidoId = result.getLong("id");
                    TipoPedido tipoPedido = TipoPedido.valueOf(result.getString("tipo_pedido"));
                    StatusPedido status = StatusPedido.valueOf(result.getString("status"));
                    BigDecimal valor = result.getBigDecimal("pagamento_valor");
                    Pagamento pagamento = criarPagamento(result.getString("pagamento_tipo"), valor);
                    LocalDate data = result.getTimestamp("data_criacao").toLocalDateTime().toLocalDate();
                    java.sql.Date dataFim = result.getDate("data_fim_aluguel");
                    Pedido pedido = new Pedido(pedidoId, cliente, tipoPedido, pagamento, data, status,
                            dataFim == null ? null : dataFim.toLocalDate());
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

    public int contarPedidosCliente(Long clienteId, TipoPedido tipo) {
        if (buscarCliente(clienteId) == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        String sql = "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?"
                + (tipo == null ? "" : " AND tipo_pedido = ?");
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            encerrarAlugueisExpirados(connection);
            statement.setLong(1, clienteId);
            if (tipo != null) {
                statement.setString(2, tipo.name());
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível contar os pedidos do cliente.", ex);
        }
    }

    public void reembolsarPedido(Long clienteId, Long pedidoId) {
        try (Connection connection = database.conectar()) {
            connection.setAutoCommit(false);
            try {
                TipoPedido tipo;
                StatusPedido status;
                boolean dentroPrazo;
                String consultar = "SELECT tipo_pedido, status, "
                        + "(data_criacao >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)) AS dentro_prazo "
                        + "FROM pedidos WHERE id = ? AND cliente_id = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(consultar)) {
                    statement.setLong(1, pedidoId);
                    statement.setLong(2, clienteId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Pedido não encontrado.");
                        }
                        tipo = TipoPedido.valueOf(result.getString("tipo_pedido"));
                        status = StatusPedido.valueOf(result.getString("status"));
                        dentroPrazo = result.getBoolean("dentro_prazo");
                    }
                }
                if (tipo != TipoPedido.COMPRA) {
                    throw new IllegalStateException("Somente compras podem ser reembolsadas.");
                }
                if (status != StatusPedido.CONFIRMADO) {
                    throw new IllegalStateException("Este pedido não está disponível para reembolso.");
                }
                if (!dentroPrazo) {
                    throw new IllegalStateException("O prazo de reembolso de 7 dias terminou.");
                }

                List<long[]> itens = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT livro_id, quantidade FROM itens_pedido WHERE pedido_id = ?")) {
                    statement.setLong(1, pedidoId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            itens.add(new long[]{result.getLong("livro_id"), result.getLong("quantidade")});
                        }
                    }
                }
                if (itens.isEmpty()) {
                    throw new IllegalStateException("O pedido não possui itens para reembolsar.");
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE pedidos SET status = 'REEMBOLSADO' WHERE id = ? AND status = 'CONFIRMADO'")) {
                    statement.setLong(1, pedidoId);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalStateException("Este pedido já foi atualizado.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE livros SET quantidade = quantidade + ?, disponivel = TRUE WHERE id = ?")) {
                    for (long[] item : itens) {
                        statement.setLong(1, item[1]);
                        statement.setLong(2, item[0]);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível processar o reembolso.", ex);
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

    public List<Avaliacao> buscarAvaliacoesLivro(Long livroId) {
        String sql = "SELECT a.livro_id, a.nota, a.comentario, a.data_criacao, "
                + "u.nome AS nome_cliente, (u.tipo = 'DEMO') AS demonstracao "
                + "FROM avaliacoes a JOIN usuarios u ON u.id = a.cliente_id "
                + "WHERE (? IS NULL OR a.livro_id = ?) ORDER BY a.data_criacao DESC, a.id DESC";
        List<Avaliacao> avaliacoes = new ArrayList<>();
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (livroId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, livroId);
                statement.setLong(2, livroId);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    avaliacoes.add(new Avaliacao(result.getLong("livro_id"),
                            result.getInt("nota"), result.getString("comentario"),
                            result.getString("nome_cliente"),
                            result.getTimestamp("data_criacao").toLocalDateTime().toLocalDate(),
                            result.getBoolean("demonstracao")));
                }
            }
            return avaliacoes;
        } catch (SQLException ex) {
            throw erro("Não foi possível carregar as avaliações dos livros.", ex);
        }
    }

    public boolean clientePodeAvaliar(Long clienteId, Long livroId) {
        String sql = "SELECT COUNT(*) FROM pedidos p JOIN itens_pedido i ON i.pedido_id = p.id "
                + "WHERE p.cliente_id = ? AND i.livro_id = ? "
                + "AND p.status IN ('CONFIRMADO', 'ENTREGUE', 'ATIVO', 'EXPIRADO')";
        try (Connection connection = database.conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clienteId);
            statement.setLong(2, livroId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException ex) {
            throw erro("Não foi possível validar a compra ou aluguel para avaliação.", ex);
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
        String sql = "SELECT livro_id, nome_livro, quantidade, preco_unitario "
                + "FROM itens_pedido WHERE pedido_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pedido.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Livro livro = buscarLivro(result.getLong("livro_id"))
                            .orElseThrow(() -> new IllegalStateException("Livro do pedido não encontrado."));
                    pedido.adicionarItem(livro, result.getInt("quantidade"),
                            result.getString("nome_livro"), result.getBigDecimal("preco_unitario"));
                }
            }
        }
    }

    private void validarAluguelNoBanco(Connection connection, Long clienteId, Long livroId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT digital, disponivel FROM livros WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, livroId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Livro não encontrado.");
                }
                if (!result.getBoolean("digital")) {
                    throw new IllegalStateException("Aluguel somente para livros digitais.");
                }
                if (!result.getBoolean("disponivel")) {
                    throw new IllegalStateException("Livro indisponível para aluguel.");
                }
            }
        }
        encerrarAlugueisExpirados(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM pedidos p JOIN itens_pedido i ON i.pedido_id = p.id "
                        + "WHERE p.cliente_id = ? AND i.livro_id = ? AND p.tipo_pedido = 'ALUGUEL' "
                        + "AND p.status = 'ATIVO' AND p.data_fim_aluguel > CURRENT_DATE")) {
            statement.setLong(1, clienteId);
            statement.setLong(2, livroId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getLong(1) > 0) {
                    throw new IllegalStateException("Você já possui um aluguel ativo deste livro.");
                }
            }
        }
    }

    private void encerrarAlugueisExpirados(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE pedidos SET status = 'EXPIRADO' "
                    + "WHERE tipo_pedido = 'ALUGUEL' AND status = 'ATIVO' "
                    + "AND data_fim_aluguel <= CURRENT_DATE");
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
        if ("ADMIN".equals(tipo)) {
            return new Administrador(id, nome, email, senhaHash, endereco, true);
        }
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
