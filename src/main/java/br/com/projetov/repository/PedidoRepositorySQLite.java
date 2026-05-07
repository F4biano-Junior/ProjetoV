package br.com.projetov.repository;

import br.com.projetov.config.SqliteConfig;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;


import java.sql.*;

public class PedidoRepositorySQLite implements PedidoRepository {

    @Override
    public void salvar(PedidoModel pedido) throws SQLException {
        // SQL para cabeçalho do pedido
        String sqlPedido = "INSERT INTO pedido (cliente, entregador, data_hora) VALUES (?, ?, ?)";
        // SQL para os barris (vinculados peço 'ID' do pedido)
        String sqlBarril = "INSERT INTO barris_pedido " +
                "(pedido_id, codigo_barril, tipo_chopp, capacidade, preco_venda, consignado) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = SqliteConfig.getConnection()) {
            conn.setAutoCommit(false); // Inicia a transação (tudo ou nada)
            // 1. Salva o Pedido e recupera o ID que o SQLite gerou automaticamente
            try {
                long idPedido = inserirPedido(conn, sqlPedido, pedido);
                inserirBarris(conn, sqlBarril, idPedido, pedido);
                conn.commit();
            } catch (SQLException e) {
                // Rollback explícito e seguro
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Loga o erro de rollback sem suprimir a exceção original
                    System.err.println("Falha crítica no rollback: " + rollbackEx.getMessage());
                }
                // Relança exceção original para a Service tratar
                throw e;
            }
        }
    }

    private long inserirPedido(Connection conn, String sql, PedidoModel pedido) throws SQLException {
        try (PreparedStatement pstmtPedido = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmtPedido.setString(1, pedido.getNomeCliente());
            pstmtPedido.setString(2, pedido.getEntregador());
            pstmtPedido.setString(3, pedido.getDataHora().toString());
            pstmtPedido.executeUpdate();

            try (ResultSet rs = pstmtPedido.getGeneratedKeys()) { // ← try-with-resources
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("Falha ao obter ID gerado para pedido");
        }
    }


    // 2. Salva todos os barris do pedido
    private void inserirBarris(Connection conn, String sql, long idPedido, PedidoModel pedido) throws SQLException{
        try (PreparedStatement pstmtBarril = conn.prepareStatement(sql)) {
            for (BarrilPedido barril : pedido.getBarris()) {
                pstmtBarril.setLong(1, idPedido);
                pstmtBarril.setString(2, barril.getCodigoBarril());
                pstmtBarril.setString(3, barril.getTipo().name());
                pstmtBarril.setInt(4, barril.getCapacidade().getLitros());
                pstmtBarril.setDouble(5, barril.getPrecoVenda());
                pstmtBarril.setInt(6, barril.isConsignado() ? 1: 0);
                pstmtBarril.addBatch(); // Adiciona ao lote para salvar de uma vez
            }
            pstmtBarril.executeBatch(); // Executa o salvamento de todos os barris
        }
    }
    @Override
    public double buscarVolumeMensalCliente(String nomeCliente) {
        if (nomeCliente == null || nomeCliente.isBlank()) {
            return 0.0;
        }

        String sql = "SELECT COALESCE(SUM(b.capacidade), 0.0) AS total " +
                "FROM barris_pedido b " +
                "JOIN pedido p ON b.pedido_id = p.id " +
                "WHERE p.cliente = ? " +
                "AND p.data_hora >= datetime('now', '-30 days')";

        try (Connection conn = SqliteConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeCliente);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao calcular volume mensal do cliente '" + nomeCliente + "'", e // ← vírgula
            );
        }
        return 0.0;
    }

}
