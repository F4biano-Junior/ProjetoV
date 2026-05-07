package br.com.projetov.service.calculadora.regrascalculos;

import br.com.projetov.models.enums.TipoVenda;

public class RegrasPorVolume implements RegraPreco{
    @Override
    public double aplicar(double precoBase, double volumeMensal, TipoVenda tipoVenda, boolean isConsignado) {
        if (volumeMensal >= 1000){
            return precoBase - 1.00;
        }
        return precoBase;
    }
}
