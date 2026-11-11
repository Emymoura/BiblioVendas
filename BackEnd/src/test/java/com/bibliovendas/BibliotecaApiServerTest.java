package com.bibliovendas;

import com.bibliovendas.api.BibliotecaApiServer;
import com.bibliovendas.model.Livro;
import com.bibliovendas.service.BibliotecaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibliotecaApiServerTest {
    private final ObjectMapper json = new ObjectMapper();
    private BibliotecaService service;
    private BibliotecaApiServer api;

    @BeforeEach
    void iniciarApi() throws Exception {
        service = new BibliotecaService();
        service.cadastrarLivro("Java para Todos", "Introdução", "Tecnologia",
                "Programação", "Autora", "Editora", 200, 2025,
                new BigDecimal("25.00"), 5, true, true);
        service.cadastrarLivro("Arquitetura Limpa", "Projeto de software", "Tecnologia",
                "Programação", "Autor", "Editora", 300, 2024,
                new BigDecimal("49.90"), 4, true, true);
        api = new BibliotecaApiServer(service, 0);
        api.start();
    }

    @AfterEach
    void pararApi() {
        api.close();
    }

    @Test
    void deveConsultarCatalogoCadastrarClienteERegistrarPedido() throws Exception {
        JsonNode livros = request("GET", "/api/livros?busca=Java", null, null).body;
        assertEquals(1, livros.size());
        assertEquals("Java para Todos", livros.get(0).get("nome").asText());

        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"ana@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        assertEquals(201, cadastro.status);
        String token = cadastro.body.get("token").asText();

        Response pedido = request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");
        assertEquals(201, pedido.status);
        assertEquals("CONFIRMADO", pedido.body.get("status").asText());

        Response avaliacao = request("POST", "/api/avaliacoes", token,
                "{\"livroId\":1,\"nota\":5,\"comentario\":\"Gostei muito\"}");
        assertEquals(201, avaliacao.status);
        assertEquals("Ana", avaliacao.body.get("cliente").asText());

        Response avaliacoes = request("GET", "/api/avaliacoes?livroId=1", null, null);
        assertEquals(200, avaliacoes.status);
        assertEquals(1, avaliacoes.body.size());
        assertEquals(5, avaliacoes.body.get(0).get("nota").asInt());

        Response historico = request("GET", "/api/pedidos", token, null);
        assertEquals(200, historico.status);
        assertEquals(1, historico.body.size());
        assertEquals("Java para Todos", historico.body.get(0).get("itens").get(0).get("nome").asText());
    }

    @Test
    void deveExigirAutenticacaoParaCriarPedido() throws Exception {
        Response resposta = request("POST", "/api/pedidos", null,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");

        assertEquals(401, resposta.status);
        assertTrue(resposta.body.has("erro"));
    }

    @Test
    void deveValidarSessaoAutenticada() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"sessao@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();

        Response resposta = request("GET", "/api/auth/session", token, null);

        assertEquals(200, resposta.status);
        assertEquals("Ana", resposta.body.get("usuario").get("nome").asText());
    }

    @Test
    void naoDevePermitirAvaliacaoSemCompraOuAluguel() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"sem-pedido@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        Response resposta = request("POST", "/api/avaliacoes", cadastro.body.get("token").asText(),
                "{\"livroId\":1,\"nota\":4,\"comentario\":\"Ainda não comprei\"}");

        assertEquals(409, resposta.status);
        assertTrue(resposta.body.get("erro").asText().contains("comprou ou alugou"));
    }

    @Test
    void deveExigirSenhaForteNoCadastro() throws Exception {
        Response resposta = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"senha-fraca@example.test\",\"senha\":\"senha123!\",\"endereco\":\"Centro\"}");

        assertEquals(400, resposta.status);
        assertTrue(resposta.body.get("erro").asText().contains("letra maiúscula"));
    }

    @Test
    void deveImpedirAluguelDuplicadoEInformarOPrazo() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"aluguel@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();
        Response primeiro = request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"ALUGUEL\",\"pagamento\":\"PIX\"}");

        assertEquals(201, primeiro.status);
        assertEquals(java.time.LocalDate.now().plusDays(14).toString(),
                primeiro.body.get("dataFimAluguel").asText());

        Response duplicado = request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"ALUGUEL\",\"pagamento\":\"PIX\"}");
        assertEquals(409, duplicado.status);

        Response alugueis = request("GET", "/api/pedidos?pagina=1&limite=5&tipo=ALUGUEL", token, null);
        assertEquals(200, alugueis.status);
        assertEquals(1, alugueis.body.get("total").asInt());
        assertEquals("Java para Todos",
                alugueis.body.get("pedidos").get(0).get("itens").get(0).get("nome").asText());
    }

    @Test
    void deveExibirNoHistoricoOLivroRealmenteAlugado() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"aluguel-livro@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();

        Response pedido = request("POST", "/api/pedidos", token,
                "{\"livroId\":2,\"quantidade\":1,\"tipo\":\"ALUGUEL\",\"pagamento\":\"PIX\"}");
        Response historico = request("GET", "/api/pedidos?pagina=1&limite=5&tipo=ALUGUEL", token, null);

        assertEquals(201, pedido.status);
        assertEquals(2, pedido.body.get("itens").get(0).get("livroId").asLong());
        assertEquals("Arquitetura Limpa", pedido.body.get("itens").get(0).get("nome").asText());
        assertEquals(1, historico.body.get("pedidos").size());
        assertEquals(2, historico.body.get("pedidos").get(0).get("itens").get(0).get("livroId").asLong());
        assertEquals("Arquitetura Limpa",
                historico.body.get("pedidos").get(0).get("itens").get(0).get("nome").asText());
    }

    @Test
    void deveFinalizarCarrinhoComVariosLivrosEValidarEstoqueAntesDeAlterar() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"carrinho@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();

        Response compra = request("POST", "/api/pedidos", token,
                "{\"itens\":[{\"livroId\":1,\"quantidade\":2},{\"livroId\":2,\"quantidade\":1}],"
                        + "\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");

        assertEquals(201, compra.status);
        assertEquals(2, compra.body.get("itens").size());
        assertEquals(0, new BigDecimal("99.90").compareTo(compra.body.get("valorTotal").decimalValue()));

        Response estoqueInsuficiente = request("POST", "/api/pedidos", token,
                "{\"itens\":[{\"livroId\":1,\"quantidade\":1},{\"livroId\":2,\"quantidade\":5}],"
                        + "\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");
        Response primeiroLivro = request("GET", "/api/livros/1", null, null);

        assertEquals(409, estoqueInsuficiente.status);
        assertEquals(3, primeiroLivro.body.get("quantidade").asInt());
    }

    @Test
    void devePaginarHistoricoDePedidos() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"paginacao@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();
        request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");
        request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":1,\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");

        Response primeira = request("GET", "/api/pedidos?pagina=1&limite=1", token, null);
        Response segunda = request("GET", "/api/pedidos?pagina=2&limite=1", token, null);

        assertEquals(2, primeira.body.get("total").asInt());
        assertEquals(2, primeira.body.get("totalPaginas").asInt());
        assertEquals(1, primeira.body.get("pedidos").size());
        assertTrue(primeira.body.get("pedidos").get(0).get("id").asLong()
                > segunda.body.get("pedidos").get(0).get("id").asLong());
    }

    @Test
    void deveReembolsarCompraEImpedirNovoReembolso() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"reembolso@example.test\",\"senha\":\"Senha123!\",\"endereco\":\"Centro\"}");
        String token = cadastro.body.get("token").asText();
        Response compra = request("POST", "/api/pedidos", token,
                "{\"livroId\":1,\"quantidade\":2,\"tipo\":\"COMPRA\",\"pagamento\":\"PIX\"}");
        long pedidoId = compra.body.get("id").asLong();

        Response reembolso = request("POST", "/api/pedidos/" + pedidoId + "/reembolso", token, null);
        Response repetido = request("POST", "/api/pedidos/" + pedidoId + "/reembolso", token, null);
        Response livro = request("GET", "/api/livros/1", null, null);

        assertEquals(200, reembolso.status);
        assertEquals("REEMBOLSADO", reembolso.body.get("status").asText());
        assertEquals(409, repetido.status);
        assertEquals(5, livro.body.get("quantidade").asInt());
    }

    private Response request(String method, String path, String token, String body) throws Exception {
        URL url = new URL("http://127.0.0.1:" + api.getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
        }
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (body != null) {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int status = connection.getResponseCode();
        try (InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int lidos;
            while ((lidos = input.read(buffer)) != -1) {
                output.write(buffer, 0, lidos);
            }
            return new Response(status, json.readTree(output.toByteArray()));
        } finally {
            connection.disconnect();
        }
    }

    private static final class Response {
        private final int status;
        private final JsonNode body;

        private Response(int status, JsonNode body) {
            this.status = status;
            this.body = body;
        }
    }
}
