package br.com.projetov.service;

import br.com.projetov.models.TipoChopp;


public class TabelaPreco {
    public double calcularVenda(TipoChopp chopp, TipoPreco tipoCliente, double volumeMensal) {
        double valor = chopp.getPrecoBase(); // Pega o valor definido no Enum

        if (tipoCliente == TipoPreco.PDV) {
            // Regra: PDV paga 20% a menos que o preço base
            valor = valor * 0.80;

            // Regra Extra: Se bateu meta de volume, ganha + R$ 1.00 de desconto
            if (volumeMensal >= 1000) {
                valor -= 1.00;
            }
        }

        return valor;
    }
}
