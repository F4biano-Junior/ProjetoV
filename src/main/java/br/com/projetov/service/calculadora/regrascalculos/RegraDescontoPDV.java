package br.com.projetov.service.calculadora.regrascalculos;

import br.com.projetov.models.enums.TipoVenda;

public class RegraDescontoPDV implements RegraPreco {
    @Override
    public double aplicar(double precoBase, double volumeMensal, TipoVenda tipoVenda, boolean isConsignado) {
        if (isConsignado) return precoBase;
        if (tipoVenda == TipoVenda.PDV) {
            return precoBase * 0.80; // 20% de desconto
        }
        return precoBase;
    }
}
