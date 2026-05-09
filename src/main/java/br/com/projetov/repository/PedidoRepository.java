package br.com.projetov.repository;

import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.models.logistica.pedido.PedidoHistorico;
import br.com.projetov.models.logistica.pedido.PedidoPendente;
import br.com.projetov.models.logistica.pedido.ResumoHoje;

import java.sql.SQLException;
import java.util.List;

public interface PedidoRepository {
    void salvar(PedidoModel pedido) throws SQLException;
    double buscarVolumeMensalCliente(String nomeCliente);

    List<PedidoHistorico> buscarHistoricoPedidos();

    ResumoHoje buscarResumoHoje();

    List<PedidoPendente> buscarPendentesSync();

    void marcarComoSincronizado(long id) throws SQLException;
}
