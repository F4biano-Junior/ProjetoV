package br.com.projetov.service;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.repository.PedidoRepository;

import br.com.projetov.repository.SheetsRepository;
import br.com.projetov.service.calculadora.CalculadoraPreco;

import java.util.List;

public class PedidoService {
    private final PedidoRepository localRepository;
    private final SheetsRepository cloudRepository;
    private final CalculadoraPreco calcular;



    public PedidoService(PedidoRepository localRepository,
                         SheetsRepository cloudRepository,
                         CalculadoraPreco calcular) {
        this.localRepository = localRepository;
        this.cloudRepository = cloudRepository;
        this.calcular = calcular;
    }

        private void sincronizarComGoogleSheets(PedidoModel pedido) {
            try {
                // Como sua Model não tem um getTotal(), calculamos aqui para a planilha
                double totalPedido = pedido.getBarris().stream()
                        .mapToDouble(BarrilPedido::getSubtotal)
                        .sum();

                // Verificamos se existe algum barril consignado no pedido
                boolean temConsignado = pedido.getBarris().stream()
                        .anyMatch(BarrilPedido::isConsignado);

                List<List<Object>> linha = List.of(
                        List.of(
                                pedido.getDataHora().toString(),
                                pedido.getNomeCliente(),
                                pedido.getEntregador(),
                                pedido.getTipoVenda().name(),
                                totalPedido,
                                temConsignado ? "Sim" : "Não"
                        )
                );

                cloudRepository.adicionarLinha("Vendas!A2", linha);

            } catch (Exception e) {
                System.err.println("Erro na sincronização Google Sheets: " + e.getMessage());
            }
        }

    public void adicionarBarrilAoPedido(PedidoModel pedido, String codigo,
                                        TipoChopp tipo, CapacidadeBarril cap,
                                        boolean consignado) {
        try {
            // 1. O Service busca no banco o histórico de volume deste cliente
            double volumeMensal = localRepository.buscarVolumeMensalCliente(pedido.getNomeCliente()); // Ajustado parêntese

            // 2. O Service chama a calculadora
            double precoCalculado = calcular.calcularVenda(tipo, pedido.getTipoVenda(), volumeMensal, consignado);

            // 3. O barril é criado com o preço que a regra de negócio definiu
            pedido.adicionarBarril(codigo, tipo, cap, precoCalculado, consignado);
        } catch (RuntimeException e) {
            throw new RuntimeException("Não foi possível calcular o preço: erro ao consultar histórico.", e);
        }
    }
    public void  finalizarPedido(PedidoModel pedido) throws Exception {
        //validações finais
        if (pedido.getBarris().isEmpty()){
        throw new Exception("Não é possível salvar um pedido sem barris");
        }
        localRepository.salvar(pedido);
        sincronizarComGoogleSheets(pedido);
    }
}
