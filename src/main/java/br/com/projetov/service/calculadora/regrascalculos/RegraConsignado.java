package br.com.projetov.service.calculadora.regrascalculos;

import br.com.projetov.models.enums.TipoVenda;

/**
 * Regra de precificação para barris consignados.
 *
 * <p>Regra de negócio: o barril consignado é sempre cobrado pelo preço base
 * do tipo de chopp (equivalente à Venda Direta), sem acréscimos nem descontos.</p>
 *
 * <p>A posição desta regra na cadeia (primeira) garante o early return:
 * nenhuma regra subsequente (PDV, Volume) age sobre um barril consignado.</p>
 *
 * Débitos encerrados nesta classe:
 *  [D1] Implementação confirmada pelo Dono do Produto — retorna precoBase.
 *  [D3] Registrada na cadeia do PedidoService.
 */
public class RegraConsignado implements RegraPreco {

    /**
     * Aplica a regra de consignado.
     *
     * <p>Se o barril for consignado, retorna o {@code precoBase} sem alteração,
     * interrompendo efetivamente a cadeia para este item.
     * Se não for consignado, passa adiante sem interferir.</p>
     *
     * @param precoBase    preço base do tipo de chopp
     * @param volumeMensal volume mensal do cliente em litros (não utilizado aqui)
     * @param tipoVenda    canal de venda (não utilizado aqui)
     * @param isConsignado {@code true} se o barril for consignado
     * @return precoBase inalterado se consignado; precoBase para passagem à próxima regra caso contrário
     */
    @Override
    public double aplicar(double precoBase,
                          double volumeMensal,
                          TipoVenda tipoVenda,
                          boolean isConsignado) {
        if (isConsignado) {
            return precoBase; // preço fixo: sem desconto, sem acréscimo
        }
        return precoBase;
    }
}