package br.com.projetov.repository;

import br.com.projetov.config.ConnectionFactory;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;

import java.sql.*;

public class PedidoRepositorySQLite implements PedidoRepository {

    @Override
    public void salvar(PedidoModel pedido) throws SQLException {
        // ALTERADO: tipo_venda incluído na query e nos parâmetros
        String sqlPedido = "INSERT INTO pedido_model (cliente, entregador, tipo_venda, data_hora) " +
                "VALUES (?, ?, ?, ?)";
        String sqlBarril = "INSERT INTO barris_pedido " +
                "(pedido_id, codigo_barril, tipo_chopp, capacidade, preco_venda, consignado) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long idPedido = inserirPedido(conn, sqlPedido, pedido);
                inserirBarris(conn, sqlBarril, idPedido, pedido);
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Falha crítica no rollback: " + rollbackEx.getMessage());
                }
                throw e;
            }
        }
    }

    private long inserirPedido(Connection conn, String sql, PedidoModel pedido) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, pedido.getNomeCliente());
            pstmt.setString(2, pedido.getEntregador());
            pstmt.setString(3, pedido.getTipoVenda().name()); // NOVO: enum → String
            pstmt.setString(4, pedido.getDataHora().toString());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("Falha ao obter ID gerado para pedido");
        }
    }

    private void inserirBarris(Connection conn, String sql, long idPedido, PedidoModel pedido)
            throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (BarrilPedido barril : pedido.getBarris()) {
                pstmt.setLong(1,   idPedido);
                pstmt.setString(2, barril.getCodigoBarril());
                pstmt.setString(3, barril.getTipo().name());
                pstmt.setInt(4,    barril.getCapacidade().getLitros()); // mantido: INTEGER
                pstmt.setDouble(5, barril.getPrecoVenda());
                pstmt.setInt(6,    barril.isConsignado() ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public double buscarVolumeMensalCliente(String nomeCliente) {
        if (nomeCliente == null || nomeCliente.isBlank()) {
            return 0.0;
        }
        // Sem alteração — volume usa capacidade (litros) que já era INTEGER
        String sql = "SELECT COALESCE(SUM(b.capacidade), 0.0) AS total " +
                "FROM barris_pedido b " +
                "JOIN pedido_model p ON b.pedido_id = p.id " +
                "WHERE p.cliente = ? " +
                "AND p.data_hora >= datetime('now', '-30 days')";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeCliente);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Aviso: Histórico não localizado para '" + nomeCliente +
                    "'. Assumindo como primeira compra. Erro: " + e.getMessage());
        }
        return 0.0;
    }
}