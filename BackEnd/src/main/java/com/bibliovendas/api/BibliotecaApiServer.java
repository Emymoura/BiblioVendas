package com.bibliovendas.api;

import com.bibliovendas.model.Boleto;
import com.bibliovendas.model.Cartao;
import com.bibliovendas.model.Cliente;
import com.bibliovendas.model.Livro;
import com.bibliovendas.model.Pagamento;
import com.bibliovendas.model.Pedido;
import com.bibliovendas.model.Pix;
import com.bibliovendas.model.TipoPedido;
import com.bibliovendas.persistence.PersistenceException;
import com.bibliovendas.service.BibliotecaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class BibliotecaApiServer implements AutoCloseable {
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private final BibliotecaService service;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Cliente> sessoes = new ConcurrentHashMap<>();
    private final HttpServer server;

    public BibliotecaApiServer(BibliotecaService service, int port) throws IOException {
        this.service = service;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.createContext("/api", this::handle);
        this.server.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "bibliovendas-api");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/api/health".equals(path)) {
                responder(exchange, 200, json.createObjectNode().put("status", "ok"));
            } else if ("GET".equals(method) && "/api/livros".equals(path)) {
                listarLivros(exchange);
            } else if ("POST".equals(method) && "/api/clientes".equals(path)) {
                cadastrarCliente(exchange);
            } else if ("POST".equals(method) && "/api/auth/login".equals(path)) {
                login(exchange);
            } else if ("POST".equals(method) && "/api/pedidos".equals(path)) {
                criarPedido(exchange);
            } else if ("GET".equals(method) && "/api/pedidos".equals(path)) {
                listarPedidos(exchange);
            } else {
                responder(exchange, 404, json.createObjectNode().put("erro", "Rota não encontrada."));
            }
        } catch (IllegalArgumentException ex) {
            responder(exchange, 400, erro(ex.getMessage()));
        } catch (IllegalStateException ex) {
            responder(exchange, 409, erro(ex.getMessage()));
        } catch (SecurityException ex) {
            responder(exchange, 401, erro(ex.getMessage()));
        } catch (PersistenceException ex) {
            if (isConstraintViolation(ex)) {
                responder(exchange, 409, erro("Já existe um cadastro com esses dados."));
            } else {
                responder(exchange, 500, erro("Erro ao acessar o banco de dados."));
            }
        } catch (JsonProcessingException ex) {
            responder(exchange, 400, erro("O corpo da requisição deve conter JSON válido."));
        } catch (RuntimeException ex) {
            responder(exchange, 500, erro("Erro interno ao processar a solicitação."));
        } finally {
            exchange.close();
        }
    }

    private void listarLivros(HttpExchange exchange) throws IOException {
        String termo = parametroConsulta(exchange, "busca");
        List<Livro> livros = service.buscarLivros(termo);
        ArrayNode resposta = json.createArrayNode();
        for (Livro livro : livros) {
            resposta.add(livroJson(livro));
        }
        responder(exchange, 200, resposta);
    }

    private void cadastrarCliente(HttpExchange exchange) throws IOException {
        JsonNode corpo = lerCorpo(exchange);
        String nome = textoObrigatorio(corpo, "nome");
        String email = textoObrigatorio(corpo, "email");
        String senha = textoObrigatorio(corpo, "senha");
        String endereco = textoObrigatorio(corpo, "endereco");
        if (senha.length() < 4) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 4 caracteres.");
        }

        Cliente cliente = service.cadastrarCliente(nome, email, senha, endereco);
        responder(exchange, 201, sessaoJson(cliente));
    }

    private void login(HttpExchange exchange) throws IOException {
        JsonNode corpo = lerCorpo(exchange);
        String email = textoObrigatorio(corpo, "email");
        String senha = textoObrigatorio(corpo, "senha");
        Cliente cliente = service.loginCliente(email, senha)
                .orElseThrow(() -> new SecurityException("E-mail ou senha inválidos."));
        responder(exchange, 200, sessaoJson(cliente));
    }

    private void criarPedido(HttpExchange exchange) throws IOException {
        Cliente cliente = clienteAutenticado(exchange);
        JsonNode corpo = lerCorpo(exchange);
        long livroId = numeroObrigatorio(corpo, "livroId");
        int quantidade = inteiroObrigatorio(corpo, "quantidade");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser positiva.");
        }

        TipoPedido tipo;
        try {
            tipo = TipoPedido.valueOf(textoObrigatorio(corpo, "tipo").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("O tipo deve ser COMPRA ou ALUGUEL.");
        }

        Livro livro = service.listarCatalogo().stream()
                .filter(item -> item.getId().longValue() == livroId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));
        String metodo = textoObrigatorio(corpo, "pagamento").toUpperCase();
        Pagamento pagamento = criarPagamento(metodo, livro, quantidade, cliente);

        Pedido pedido = tipo == TipoPedido.COMPRA
                ? service.efetuarCompra(cliente, livroId, quantidade, pagamento)
                : service.efetuarAluguel(cliente, livroId, quantidade, pagamento);
        responder(exchange, 201, pedidoJson(pedido));
    }

    private void listarPedidos(HttpExchange exchange) throws IOException {
        Cliente cliente = clienteAutenticado(exchange);
        List<Pedido> pedidos = service.consultarHistoricoCliente(cliente.getId());
        ArrayNode resposta = json.createArrayNode();
        for (Pedido pedido : pedidos) {
            resposta.add(pedidoJson(pedido));
        }
        responder(exchange, 200, resposta);
    }

    private Pagamento criarPagamento(String metodo, Livro livro, int quantidade, Cliente cliente) {
        java.math.BigDecimal total = livro.getPreco().multiply(java.math.BigDecimal.valueOf(quantidade));
        if ("PIX".equals(metodo)) {
            return new Pix(total, "pagamento-simulado");
        }
        if ("BOLETO".equals(metodo)) {
            return new Boleto(total, "pagamento-simulado");
        }
        if ("CARTAO".equals(metodo)) {
            return new Cartao(total, "0000", cliente.getNome());
        }
        throw new IllegalArgumentException("O pagamento deve ser PIX, CARTAO ou BOLETO.");
    }

    private ObjectNode sessaoJson(Cliente cliente) {
        String token = UUID.randomUUID().toString();
        sessoes.put(token, cliente);
        ObjectNode usuario = json.createObjectNode()
                .put("id", cliente.getId())
                .put("nome", cliente.getNome())
                .put("email", cliente.getEmail());
        return json.createObjectNode()
                .put("token", token)
                .set("usuario", usuario);
    }

    private Cliente clienteAutenticado(HttpExchange exchange) {
        String autorizacao = exchange.getRequestHeaders().getFirst("Authorization");
        if (autorizacao == null || !autorizacao.startsWith("Bearer ")) {
            throw new SecurityException("Autenticação necessária.");
        }
        Cliente cliente = sessoes.get(autorizacao.substring("Bearer ".length()).trim());
        if (cliente == null) {
            throw new SecurityException("Sessão inválida ou expirada. Faça login novamente.");
        }
        return cliente;
    }

    private ObjectNode livroJson(Livro livro) {
        return json.createObjectNode()
                .put("id", livro.getId())
                .put("nome", livro.getNome())
                .put("sinopse", livro.getSinopse())
                .put("categoria", livro.getCategoria())
                .put("genero", livro.getGenero())
                .put("autor", livro.getAutor())
                .put("editora", livro.getEditora())
                .put("numeroPaginas", livro.getNumeroPaginas())
                .put("anoPublicacao", livro.getAnoPublicacao())
                .put("preco", livro.getPreco())
                .put("quantidade", livro.getQuantidade())
                .put("disponivel", livro.isDisponivel())
                .put("digital", livro.isDigital());
    }

    private ObjectNode pedidoJson(Pedido pedido) {
        ObjectNode resposta = json.createObjectNode()
                .put("id", pedido.getId())
                .put("data", pedido.getData().toString())
                .put("status", pedido.getStatus().name())
                .put("tipo", pedido.getTipoPedido().name())
                .put("pagamento", pedido.getPagamento().getTipo())
                .put("valorTotal", pedido.getValorTotal());
        ArrayNode itens = resposta.putArray("itens");
        pedido.getItens().forEach(item -> itens.add(json.createObjectNode()
                .put("livroId", item.getLivro().getId())
                .put("nome", item.getLivro().getNome())
                .put("quantidade", item.getQuantidade())
                .put("valor", item.getValorTotal())));
        return resposta;
    }

    private JsonNode lerCorpo(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        try (InputStream input = exchange.getRequestBody()) {
            int lidos;
            while ((lidos = input.read(buffer)) != -1) {
                total += lidos;
                if (total > MAX_REQUEST_BYTES) {
                    throw new IllegalArgumentException("A requisição excede o tamanho permitido.");
                }
                output.write(buffer, 0, lidos);
            }
        }
        JsonNode corpo = json.readTree(output.toByteArray());
        if (corpo == null || !corpo.isObject()) {
            throw new IllegalArgumentException("O corpo da requisição deve ser um objeto JSON.");
        }
        return corpo;
    }

    private String textoObrigatorio(JsonNode corpo, String campo) {
        JsonNode valor = corpo.get(campo);
        if (valor == null || !valor.isTextual() || valor.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.asText().trim();
    }

    private long numeroObrigatorio(JsonNode corpo, String campo) {
        JsonNode valor = corpo.get(campo);
        if (valor == null || !valor.canConvertToLong()) {
            throw new IllegalArgumentException("O campo '" + campo + "' deve ser um número inteiro.");
        }
        return valor.asLong();
    }

    private int inteiroObrigatorio(JsonNode corpo, String campo) {
        long numero = numeroObrigatorio(corpo, campo);
        if (numero > Integer.MAX_VALUE || numero < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("O campo '" + campo + "' está fora do intervalo permitido.");
        }
        return (int) numero;
    }

    private String parametroConsulta(HttpExchange exchange, String nome) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String parte : query.split("&")) {
            String[] chaveValor = parte.split("=", 2);
            if (URLDecoder.decode(chaveValor[0], "UTF-8").equals(nome)) {
                return chaveValor.length == 2 ? URLDecoder.decode(chaveValor[1], "UTF-8") : "";
            }
        }
        return null;
    }

    private ObjectNode erro(String mensagem) {
        return json.createObjectNode().put("erro", mensagem);
    }

    private void responder(HttpExchange exchange, int status, Object corpo) throws IOException {
        byte[] bytes = json.writeValueAsBytes(corpo);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private boolean isConstraintViolation(PersistenceException ex) {
        Throwable causa = ex.getCause();
        return causa instanceof SQLException
                && ((SQLException) causa).getSQLState() != null
                && ((SQLException) causa).getSQLState().startsWith("23");
    }
}
