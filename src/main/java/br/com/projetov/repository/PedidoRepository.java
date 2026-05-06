package br.com.projetov.repository;

import br.com.projetov.config.SqliteConfig;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;


import java.sql.*;

public class PedidoRepository {
    public void salvar(PedidoModel pedido) {
        // SQL para cabeçalho do pedido
        String sqlPedido = "INSERT INTO pedido (cliente, entregador, data_hora) VALUES (?, ?, ?)";
        // SQL para os barris (vinculados peço ID do pedido)
        String sqlBarril = "INSERT INTO barris_pedido (pedido_id, codigo_barril, tipo_chopp, capacidade, preco_venda) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = SqliteConfig.getConnection()) {
            conn.setAutoCommit(false); // Inicia a transação (tudo ou nada)
            // 1. Salva o Pedido e recupera o ID que o SQLite gerou automaticamente
            try (PreparedStatement pstmtPedido = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                pstmtPedido.setString(1, pedido.getNomeCliente());
                pstmtPedido.setString(2, pedido.getEntregador());
                pstmtPedido.setString(3, pedido.getDataHora().toString());
                pstmtPedido.executeUpdate();
                // Recupera o ID gerado (necessário para os barris saberem a quem pertencem)
                ResultSet rs = pstmtPedido.getGeneratedKeys();
                if (rs.next()) {
                    long idGeradoParaPedido = rs.getLong(1);

                    // 2. Salva todos os barris do pedido
                    // 2. Salva todos os barris do pedido
                    try (PreparedStatement pstmtBarril = conn.prepareStatement(sqlBarril)) {
                        for (BarrilPedido barril : pedido.getBarris()) {
                            pstmtBarril.setLong(1, idGeradoParaPedido);
                            pstmtBarril.setString(2, barril.getCodigoBarril());
                            pstmtBarril.setString(3, barril.getTipo().name());
                            pstmtBarril.setInt(4, barril.getCapacidade().getLitros());
                            pstmtBarril.setDouble(5, barril.getPrecoVenda());
                            pstmtBarril.addBatch(); // Adiciona ao lote para salvar de uma vez
                        }
                        pstmtBarril.executeBatch(); // Executa o salvamento de todos os barris
                    }
                }
                conn.commit(); // Se chegou aqui sem erro, grava definitivamente
                System.out.println("Pedido e barris salvos no SQLite!");
            } catch (SQLException e) {
                conn.rollback(); // Se der erro, desfaz o que foi feito no banco
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar no banco de dados: " + e.getMessage());
        }

    }

    public double buscarVolumeMensalCliente(String nomeCliente) {
        // SQL que soma os litros dos barris vendidos para o cliente nos últimos 30 dias
        String sql = "SELECT SUM(b.capacidade) as total " +
                "FROM barris_pedido b " +
                "JOIN pedido p ON b.pedido_id = p.id " +
                "WHERE p.cliente = ? " +
                "AND p.data_hora >= date('now', '-30 days')";

        try (Connection conn = SqliteConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeCliente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao calcular volume mensal: " + e.getMessage());
        }
        return 0.0; // Se não encontrar nada, o volume é zero
    }
}
