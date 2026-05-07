package br.com.projetov.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void inicializar() {
        // Lê o arquivo schema.sql da pasta resources
        try (InputStream is = DatabaseInitializer.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new RuntimeException("Arquivo schema.sql não encontrado na pasta resources.");
            }

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] comandos = sql.split(";");

            try (Connection conn = ConnectionFactory.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Passa por cada pedaço do script e executa um de cada vez
                for (String comando : comandos) {
                    if (!comando.trim().isEmpty()) { // Ignora espaços em branco no final do arquivo
                        stmt.execute(comando);
                    }
                }
                System.out.println("Banco de dados verificado/atualizado com sucesso.");
            }

        } catch (Exception e) {
            throw new RuntimeException("Falha ao inicializar o banco de dados", e);
        }
    }
}