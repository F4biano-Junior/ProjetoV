package br.com.projetov.repository;

import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.models.logistica.pedido.PedidoPendente;

import java.sql.SQLException;
import java.util.List;

public interface PedidoRepository {
    void salvar(PedidoModel pedido) throws SQLException;
    double buscarVolumeMensalCliente(String nomeCliente);

    List<PedidoPendente> buscarPendentesSync();

    void marcarComoSincronizado(long id) throws SQLException;
}