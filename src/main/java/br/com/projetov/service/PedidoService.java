package br.com.projetov.service;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.repository.PedidoRepository;

public class PedidoService {
//    public static void main(String[] args) {
//        PedidoModel pedidoModel = new PedidoModel("Carlos", "Roberto");
//        pedidoModel.adicionarBarril(
//                "B50-1234",
//                TipoChopp.Pilsen,
//                CapacidadeBarril.L50,
//                500
//        );
//    }
    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public void adicionarBarrilAoPedido(PedidoModel pedido, String codigo, TipoChopp tipo, CapacidadeBarril cap, TipoVenda modalidade) {
        // 1. O Service busca no banco o histórico de volume deste cliente
        double volumeMensal = repository.buscarVolumeMensalCliente(pedido.getNomeCliente()); // Ajustado parêntese

        // 2. O Service chama a calculadora (Método Static agora)
        double precoCalculado = CalculadoraPreco.calcularVenda(tipo, modalidade, volumeMensal);

        // 3. O barril é criado com o preço que a regra de negócio definiu
        pedido.adicionarBarril(codigo, tipo, cap, precoCalculado);
    }

    public void  finalizarPedido(PedidoModel pedido) throws Exception {
        //validações finais
        if (pedido.getBarris().isEmpty()){
        throw new Exception("Não é possível salvar um pedido sem barris");
        }
        repository.salvar(pedido);
    }
}
