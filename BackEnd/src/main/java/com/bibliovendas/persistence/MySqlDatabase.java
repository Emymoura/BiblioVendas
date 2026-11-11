package com.bibliovendas.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class MySqlDatabase {
    private final String url;
    private final String usuario;
    private final String senha;

    public MySqlDatabase() {
        this(
                valorConfigurado("BIBLIO_DB_URL", "jdbc:mysql://localhost:3306/bibliovendas?serverTimezone=UTC"),
                valorConfigurado("BIBLIO_DB_USER", "root"),
                valorConfigurado("BIBLIO_DB_PASSWORD", "")
        );
    }

    public MySqlDatabase(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    public void inicializarSchema() {
        try (InputStream input = MySqlDatabase.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("O arquivo schema.sql não foi encontrado no classpath.");
            }

            String schema;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                schema = reader.lines().collect(Collectors.joining("\n"));
            }

            try (Connection connection = conectar(); Statement statement = connection.createStatement()) {
                for (String comando : schema.split(";")) {
                    if (!comando.trim().isEmpty()) {
                        statement.execute(comando);
                    }
                }
            }
        } catch (SQLException | IOException ex) {
            throw new PersistenceException(
                    "Não foi possível inicializar o banco MySQL. Verifique a conexão e execute BackEnd/database/schema.sql.",
                    ex
            );
        }
    }

    private static String valorConfigurado(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null ? padrao : valor;
    }
}
