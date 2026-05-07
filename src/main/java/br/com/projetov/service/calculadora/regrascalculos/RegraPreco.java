package br.com.projetov.service.calculadora.regrascalculos;

import br.com.projetov.models.enums.TipoVenda;

public interface RegraPreco {
    double aplicar(double precoBase, double volumeMensal, TipoVenda tipoVenda, boolean isConsignado);
}
