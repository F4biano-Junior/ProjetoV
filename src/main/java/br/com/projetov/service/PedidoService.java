package br.com.projetov.service;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.pedido.BarrilPedido;
import br.com.projetov.models.pedido.PedidoHistorico;
import br.com.projetov.models.pedido.PedidoModel;
import br.com.projetov.models.relatorio.ResumoHoje;
import br.com.projetov.repository.PedidoRepository;

import br.com.projetov.repository.SheetsRepository;
import br.com.projetov.service.calculadora.CalculadoraPreco;
import br.com.projetov.service.calculadora.regrascalculos.RegraConsignado;
import br.com.projetov.service.calculadora.regrascalculos.RegraDescontoPDV;
import br.com.projetov.service.calculadora.regrascalculos.RegraPreco;
import br.com.projetov.service.calculadora.regrascalculos.RegrasPorVolume;

import java.util.List;

public class PedidoService {
    private final PedidoRepository localRepository;
    private final CalculadoraPreco calcular;



    public PedidoService(PedidoRepository localRepository,
                         SheetsRepository cloudRepository) {
        this.localRepository = localRepository;

        List<RegraPreco> cadeia = List.of(
                new RegraConsignado(),
                new RegraDescontoPDV(),
                new RegrasPorVolume()
        );
        this.calcular = new CalculadoraPreco(cadeia);
    }
    /** Construtor para testes: permite injetar calculadora configurada externamente. */
    public PedidoService(PedidoRepository localRepository,
                         SheetsRepository cloudRepository,
                         CalculadoraPreco calcular) {
        this.localRepository = localRepository;
        this.calcular        = calcular;
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

        if (pedido.getBarris().isEmpty()){
        throw new Exception("Não é possível salvar um pedido sem barris");
        }
        localRepository.salvar(pedido);
    }

    public List<PedidoHistorico> listarHistoricoPedidos() {
        return localRepository.buscarHistoricoPedidos();
    }

    public ResumoHoje buscarResumoHoje() {
        return localRepository.buscarResumoHoje();
    }
}
