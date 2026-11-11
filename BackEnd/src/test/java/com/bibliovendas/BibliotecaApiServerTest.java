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
                "{\"nome\":\"Ana\",\"email\":\"ana@example.test\",\"senha\":\"senha12345\",\"endereco\":\"Centro\"}");
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
    void naoDevePermitirAvaliacaoSemCompraOuAluguel() throws Exception {
        Response cadastro = request("POST", "/api/clientes", null,
                "{\"nome\":\"Ana\",\"email\":\"sem-pedido@example.test\",\"senha\":\"senha12345\",\"endereco\":\"Centro\"}");
        Response resposta = request("POST", "/api/avaliacoes", cadastro.body.get("token").asText(),
                "{\"livroId\":1,\"nota\":4,\"comentario\":\"Ainda não comprei\"}");

        assertEquals(409, resposta.status);
        assertTrue(resposta.body.get("erro").asText().contains("comprou ou alugou"));
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
