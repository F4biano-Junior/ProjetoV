package br.com.projetov.service;

import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;


public class CalculadoraPreco {
    public static double calcularVenda(TipoChopp chopp, TipoVenda tipoVenda, double volumeMensal) {
        double valor = chopp.getPrecoBase(); // Pega o valor definido no Enum

        if (tipoVenda == TipoVenda.PDV) {
            // Regra: PDV paga 20% a menos que o preço base
            valor = valor * 0.80;

            // Regra Extra: Se bateu meta de volume, ganha R$ = 1,00 de desconto
            // Bônus de volume: Use uma constante ou variável para esse 1000
            // No futuro, esse valor pode vir de uma tabela de metas
            if (volumeMensal >= 1000) {
                valor -= 1.00;
            } else if (tipoVenda == TipoVenda.VENDA_DIRETA){
                // Aqui posso colocar regras específicas para venda direta no futuro
                // Por enquanto, retorna o preço base.
            }
        }
        return valor;
    }
}