package br.com.projetov.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    // Caminho do arquivo SQLite
    private static final String URL = "jdbc:sqlite:vendas_de_chopp.db";

    // Método que devolve uma conexão nova
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar no banco de dados SQLite: " + e.getMessage(), e);
        }
    }
}
