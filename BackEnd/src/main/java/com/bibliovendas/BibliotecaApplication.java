package com.bibliovendas;

import com.bibliovendas.api.BibliotecaApiServer;
import com.bibliovendas.service.BibliotecaService;

import java.io.IOException;

public final class BibliotecaApplication {
    private BibliotecaApplication() {
    }

    public static void main(String[] args) throws IOException {
        BibliotecaService service = BibliotecaService.conectarAoMySql();
        configurarAdministradorInicial(service);
        int port = Integer.parseInt(System.getenv().getOrDefault("BIBLIO_API_PORT", "8080"));
        BibliotecaApiServer api = new BibliotecaApiServer(service, port);
        api.start();
        Runtime.getRuntime().addShutdownHook(new Thread(api::close));
        System.out.println("API BiblioVendas disponível em http://127.0.0.1:" + api.getPort() + "/api");
    }

    private static void configurarAdministradorInicial(BibliotecaService service) {
        String email = System.getenv("BIBLIO_ADMIN_EMAIL");
        String senha = System.getenv("BIBLIO_ADMIN_PASSWORD");
        if (email == null && senha == null) {
            return;
        }
        if (email == null || email.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Configure BIBLIO_ADMIN_EMAIL e BIBLIO_ADMIN_PASSWORD para provisionar o administrador.");
        }
        service.garantirAdministrador("Administrador", email.trim(), senha);
    }
}
