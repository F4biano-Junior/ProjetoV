package br.com.projetov.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteConfig {
    private static final String URL = "jdbc:sqlite:vendas_de_chopp.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
    public static void inicializarBanco(){
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()){

            // Tabela de Pedidos
            String sqlPedido = "CREATE TABLE IF NOT EXISTS pedido (" +
                    "id INTEGER PRIMARY KEY," +
                    "cliente TEXT," +
                    "entregador TEXT," +
                    "data_hora TEXT," +
                    "sincronizado INTEGER DEFAULT 0)";

            // Tabela de Barris (Relacionada ao pedido pelo pedido_id)
            String sqlBarril = "CREATE TABLE IF NOT EXISTS barris_pedido (" +
                    "id INTEGER PRIMARY KEY," +
                    "pedido_id INTEGER," +
                    "codigo_barril TEXT," +
                    "tipo_chopp TEXT," +
                    "capacidade INTEGER," +
                    "preco_venda REAL," +
                    "FOREIGN KEY (pedido_id) REFERENCES pedido(id))";

            stmt.executeUpdate(sqlPedido);
            stmt.executeUpdate(sqlBarril);

            // Ativar o suporte a Foreign Keys no SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
