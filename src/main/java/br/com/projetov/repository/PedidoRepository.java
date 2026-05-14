package br.com.projetov.repository;

import br.com.projetov.models.pedido.PedidoModel;
import br.com.projetov.models.pedido.PedidoHistorico;
import br.com.projetov.models.pedido.PedidoPendente;
import br.com.projetov.models.relatorio.ResumoHoje;

import java.sql.SQLException;
import java.util.List;

public interface PedidoRepository {
    void salvar(PedidoModel pedido) throws SQLException;
    double buscarVolumeMensalCliente(String nomeCliente);
    List<PedidoPendente> buscarPendentesSync();

    List<PedidoHistorico> buscarHistoricoPedidos();

    ResumoHoje buscarResumoHoje();

    void marcarComoSincronizado(long id) throws SQLException;
}