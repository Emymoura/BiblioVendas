package com.bibliovendas;

import com.bibliovendas.model.Cartao;
import com.bibliovendas.model.Cliente;
import com.bibliovendas.model.Livro;
import com.bibliovendas.model.Pedido;
import com.bibliovendas.model.PrestadorServico;
import com.bibliovendas.model.StatusPedido;
import com.bibliovendas.service.BibliotecaService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BibliotecaServiceTest {

    @Test
    void deveCadastrarClienteELogin() {
        BibliotecaService service = new BibliotecaService();

        Cliente cliente = service.cadastrarCliente("Ana", "ana@email.com", "Senha123!", "Rua A, 123");

        assertNotNull(cliente);
        assertEquals("Ana", service.loginCliente("ana@email.com", "Senha123!")
                .orElseThrow(() -> new AssertionError("Cliente não encontrado."))
                .getNome());
    }

    @Test
    void deveCompraAtualizarEstoque() {
        BibliotecaService service = new BibliotecaService();
        Cliente cliente = service.cadastrarCliente("Joao", "joao@email.com", "Senha123!", "Rua B");
        Livro livro = service.cadastrarLivro(
                "Java 17",
                "Livro de estudo",
                "Tecnologia",
                "Programação",
                "Autor Teste",
                "Editora Teste",
                300,
                2024,
                new BigDecimal("89.90"),
                3,
                true,
                false
        );

        Pedido pedido = service.efetuarCompra(cliente, livro.getId(), 2, new Cartao(new BigDecimal("179.80"), "1234", "Joao"));

        assertEquals(StatusPedido.CONFIRMADO, pedido.getStatus());
        assertEquals(1, livro.getQuantidade());
        assertEquals(1, cliente.getHistoricoPedidos().size());
    }

    @Test
    void deveAlugarLivroDigitalEPermitirAcesso() {
        BibliotecaService service = new BibliotecaService();
        Cliente cliente = service.cadastrarCliente("Maria", "maria@email.com", "Senha123!", "Rua C");
        Livro livro = service.cadastrarLivro(
                "Livro Digital",
                "Leitura online",
                "Tecnologia",
                "Inovação",
                "Autor Digital",
                "Editora Digital",
                150,
                2025,
                new BigDecimal("19.90"),
                10,
                true,
                true
        );

        Pedido pedido = service.efetuarAluguel(cliente, livro.getId(), 1, new Cartao(new BigDecimal("19.90"), "4321", "Maria"));

        assertEquals(StatusPedido.ATIVO, pedido.getStatus());
        assertEquals(java.time.LocalDate.now().plusDays(14), pedido.getDataFimAluguel());
        assertTrue(service.acessarLivroDigital(cliente.getId(), livro.getId()));
        assertThrows(IllegalStateException.class,
                () -> service.efetuarAluguel(cliente, livro.getId(), 1,
                        new Cartao(new BigDecimal("19.90"), "4321", "Maria")));
    }

    @Test
    void deveDesativarLivroComoPrestador() {
        BibliotecaService service = new BibliotecaService();
        PrestadorServico prestador = service.cadastrarPrestador("Lira", "lira@email.com", "Senha123!", "Rua D");
        Livro livro = service.cadastrarLivro(
                "Livro para desativar",
                "História",
                "História",
                "Histórico",
                "Autor X",
                "Editora Y",
                200,
                2023,
                new BigDecimal("30.00"),
                2,
                true,
                false
        );

        assertNotNull(prestador);
        service.removerOuDesativarLivro(livro.getId(), false);
        assertFalse(livro.isDisponivel());
    }

    @Test
    void deveRegistrarAvaliacaoEConsultarHistorico() {
        BibliotecaService service = new BibliotecaService();
        Cliente cliente = service.cadastrarCliente("Pedro", "pedro@email.com", "Senha123!", "Rua E");
        Livro livro = service.cadastrarLivro(
                "Livro Avaliado",
                "Obra de avaliação",
                "Literatura",
                "Drama",
                "Autor Z",
                "Editora A",
                250,
                2022,
                new BigDecimal("55.00"),
                10,
                true,
                false
        );

        service.efetuarCompra(cliente, livro.getId(), 1, new Cartao(new BigDecimal("55.00"), "1111", "Pedro"));
        service.avaliarExperiencia(cliente.getId(), livro.getId(), 5, "Excelente leitura");

        assertEquals(1, cliente.getAvaliacoes().size());
        assertEquals(1, service.consultarHistoricoCliente(cliente.getId()).size());
    }
}
