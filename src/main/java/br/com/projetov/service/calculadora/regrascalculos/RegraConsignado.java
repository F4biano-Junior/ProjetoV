package br.com.projetov.service.calculadora.regrascalculos;

import br.com.projetov.models.enums.TipoVenda;

public class RegraConsignado implements RegraPreco {

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