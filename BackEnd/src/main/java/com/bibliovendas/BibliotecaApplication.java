package com.bibliovendas;

import com.bibliovendas.api.BibliotecaApiServer;
import com.bibliovendas.service.BibliotecaService;

import java.io.IOException;

public final class BibliotecaApplication {
    private BibliotecaApplication() {
    }

    public static void main(String[] args) throws IOException {
        BibliotecaService service = BibliotecaService.conectarAoMySql();
        int port = Integer.parseInt(System.getenv().getOrDefault("BIBLIO_API_PORT", "8080"));
        BibliotecaApiServer api = new BibliotecaApiServer(service, port);
        api.start();
        Runtime.getRuntime().addShutdownHook(new Thread(api::close));
        System.out.println("API BiblioVendas disponível em http://127.0.0.1:" + api.getPort() + "/api");
    }
}
