package br.com.projetov.repository;

import br.com.projetov.config.ConnectionFactory;
import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoHistorico;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.models.logistica.pedido.PedidoPendente;
import br.com.projetov.models.logistica.pedido.ResumoHoje;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<PedidoHistorico> buscarHistoricoPedidos() {
        String sqlPedidos = "SELECT id, cliente, entregador, tipo_venda, data_hora, sincronizado " +
                "FROM pedido_model ORDER BY data_hora DESC, id DESC";
        String sqlBarris = "SELECT codigo_barril, tipo_chopp, capacidade, " +
                "       preco_venda, consignado " +
                "FROM barris_pedido WHERE pedido_id = ?";

        List<PedidoHistorico> historico = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsPedidos = stmt.executeQuery(sqlPedidos)) {

            while (rsPedidos.next()) {
                long id = rsPedidos.getLong("id");
                List<BarrilPedido> barris = carregarBarris(conn, sqlBarris, id);

                historico.add(new PedidoHistorico(
                        id,
                        rsPedidos.getString("cliente"),
                        rsPedidos.getString("entregador"),
                        TipoVenda.valueOf(rsPedidos.getString("tipo_venda")),
                        parseDataHora(rsPedidos.getString("data_hora")),
                        rsPedidos.getInt("sincronizado") == 1,
                        barris
                ));
            }

        } catch (SQLException | RuntimeException e) {
            System.err.println("Erro ao buscar historico de pedidos: " + e.getMessage());
        }

        return historico;
    }

    @Override
    public ResumoHoje buscarResumoHoje() {
        String sql = "SELECT COALESCE(SUM(b.capacidade), 0) AS litros, " +
                "COALESCE(SUM(b.capacidade * b.preco_venda), 0.0) AS vendas " +
                "FROM pedido_model p " +
                "JOIN barris_pedido b ON b.pedido_id = p.id " +
                "WHERE date(replace(p.data_hora, 'T', ' ')) = date('now', 'localtime')";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new ResumoHoje(
                        rs.getInt("litros"),
                        rs.getDouble("vendas")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar resumo do dia: " + e.getMessage());
        }

        return new ResumoHoje(0, 0.0);
    }

    @Override
    public List<PedidoPendente> buscarPendentesSync(){
        String sqlPedidos = "SELECT id, cliente, entregador, tipo_venda, data_hora " +
                "FROM pedido_model WHERE sincronizado = 0";
        String sqlBarris = "SELECT codigo_barril, tipo_chopp, capacidade, " +
                "       preco_venda, consignado " +
                "FROM barris_pedido WHERE pedido_id = ?";

        List<PedidoPendente> pendentes = new ArrayList<>();

        try (Connection conn        = ConnectionFactory.getConnection();
             Statement  stmt        = conn.createStatement();
             ResultSet  rsPedidos   = stmt.executeQuery(sqlPedidos)) {

            while (rsPedidos.next()) {
                long      id       = rsPedidos.getLong("id");
                PedidoModel pedido = reconstruirPedido(rsPedidos);

                carregarBarris(conn, sqlBarris, id, pedido);
                pendentes.add(new PedidoPendente(id, pedido));
            }

        } catch (SQLException e) {
            // Tolerante a falhas: retorna o que foi carregado até o erro.
            // O SyncWorker tentará novamente no próximo ciclo.
            System.err.println("Erro ao buscar pendentes de sync: " + e.getMessage());
        }

        return pendentes;
    }

    @Override
    public void marcarComoSincronizado(long id) throws SQLException {
        String sql = "UPDATE pedido_model SET sincronizado = 1 WHERE id = ?";

        try (Connection conn       = ConnectionFactory.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int linhasAfetadas = pstmt.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException(
                        "marcarComoSincronizado: nenhuma linha atualizada para id=" + id);
            }
        }
    }
    private PedidoModel reconstruirPedido(ResultSet rs) throws SQLException {
        String nomeCliente = rs.getString("cliente");
        String entregador  = rs.getString("entregador");

        TipoVenda tipoVenda;
        try {
            tipoVenda = TipoVenda.valueOf(rs.getString("tipo_venda"));
        } catch (IllegalArgumentException e) {
            throw new SQLException(
                    "Valor inválido para TipoVenda: '" + rs.getString("tipo_venda") + "'", e);
        }

        return new PedidoModel(nomeCliente, entregador, tipoVenda);
    }

    private void carregarBarris(Connection conn, String sql, long pedidoId, PedidoModel pedido)
            throws SQLException {

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pedidoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pedido.adicionarBarril(
                            rs.getString("codigo_barril"),
                            TipoChopp.valueOf(rs.getString("tipo_chopp")),
                            CapacidadeBarril.fromLitros(rs.getInt("capacidade")),
                            rs.getDouble("preco_venda"),
                            rs.getInt("consignado") == 1
                    );
                }
            }
        }
    }

    private List<BarrilPedido> carregarBarris(Connection conn, String sql, long pedidoId)
            throws SQLException {

        List<BarrilPedido> barris = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pedidoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    barris.add(new BarrilPedido(
                            rs.getString("codigo_barril"),
                            TipoChopp.valueOf(rs.getString("tipo_chopp")),
                            CapacidadeBarril.fromLitros(rs.getInt("capacidade")),
                            rs.getDouble("preco_venda"),
                            rs.getInt("consignado") == 1
                    ));
                }
            }
        }

        return barris;
    }

    private LocalDateTime parseDataHora(String valor) {
        return LocalDateTime.parse(valor.replace(" ", "T"));
    }
}
