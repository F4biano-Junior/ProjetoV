package br.com.projetov.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteConfig {

    private static final String URL = "jdbc:sqlite:vendas_de_chopp.db";

    private static final int VERSAO_ATUAL = 2; // Incremente aqui para futuras mudanças

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void inicializarBanco() {
        try (Connection conn = getConnection()) {
            // Ativa suporte a chaves estrangeiras
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            // 1. Cria tabela base se não existir
            criarTabelas(conn);

            // 2. Verifica a versão e aplica migrações
            int versaoBanco = getVersaoBanco(conn);

            if (versaoBanco < 2) {
                migrarParaV2(conn);
            }

            // Aqui você adicionaria if (versaoBanco < 3) no futuro

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao inicializar ou migrar o banco de dados.", e);
        }
    }

    private static void criarTabelas(Connection conn) throws SQLException {
        // SQL usando Text Blocks (Java 15+) ou strings normais se estiver em versão antiga
        String sqlPedido = "CREATE TABLE IF NOT EXISTS pedido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cliente TEXT NOT NULL," +
                "entregador TEXT NOT NULL," +
                "data_hora TEXT NOT NULL," +
                "sincronizado INTEGER DEFAULT 0)";

        // Note que o Schema "Ideal" (v2) já inclui a coluna consignado para novos bancos
        String sqlBarril = "CREATE TABLE IF NOT EXISTS barris_pedido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pedido_id INTEGER NOT NULL," +
                "codigo_barril TEXT NOT NULL," +
                "tipo_chopp TEXT NOT NULL," +
                "capacidade INTEGER NOT NULL," +
                "preco_venda REAL NOT NULL," +
                "consignado INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY (pedido_id) REFERENCES pedido(id))";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlPedido);
            stmt.execute(sqlBarril);
        }
    }

    private static int getVersaoBanco(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void setVersaoBanco(Connection conn, int versao) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + versao);
        }
    }

    // --- MIGRATIONS ---

    private static void migrarParaV2(Connection conn) throws SQLException {
        System.out.println("Migrando banco para versão 2...");
        try (Statement stmt = conn.createStatement()) {
            // Tenta adicionar a coluna. No SQLite, se a coluna já existir,
            // o comando falhará, por isso usamos um try-catch interno ou
            // confiamos na versão do PRAGMA.
            try {
                stmt.execute("ALTER TABLE barris_pedido ADD COLUMN consignado INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                // Se o erro for que a coluna já existe, apenas ignoramos
                if (!e.getMessage().contains("duplicate column name")) {
                    throw e;
                }
            }
        }
        setVersaoBanco(conn, 2);
        System.out.println("Banco atualizado para versão 2 com sucesso.");
    }
}