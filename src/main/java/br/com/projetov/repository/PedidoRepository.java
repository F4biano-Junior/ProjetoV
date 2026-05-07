package br.com.projetov.repository;

import br.com.projetov.models.logistica.pedido.PedidoModel;
import java.sql.SQLException;

public interface PedidoRepository {
    // Qualquer banco de dados no futuro terá que saber salvar e buscar volume
    void salvar(PedidoModel pedido) throws SQLException;
    double buscarVolumeMensalCliente(String nomeCliente);
}