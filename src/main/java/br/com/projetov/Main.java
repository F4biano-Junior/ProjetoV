package br.com.projetov;

import br.com.projetov.config.SqliteConfig;
import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.repository.PedidoRepository;
import br.com.projetov.service.PedidoService;

public class Main {
    public static void main(String[] args) {
        SqliteConfig.inicializarBanco();
        System.out.println("O Sistema Vendas Chopp iniciou com sucesso!");

            SqliteConfig.inicializarBanco();
            PedidoRepository repo = new PedidoRepository();
            PedidoService service = new PedidoService(repo);

            // Criamos o pedido
            PedidoModel pedido = new PedidoModel("Carlos Bar", "Roberto Entregador");

            // Adicionamos o barril SEM digitar o preço. A Service vai calcular sozinha!
            service.adicionarBarrilAoPedido(pedido, "B-500", TipoChopp.Pilsen, CapacidadeBarril.L50, TipoVenda.PDV);

            // Finalizamos e salvamos no SQLite
            try {
                service.finalizarPedido(pedido);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}