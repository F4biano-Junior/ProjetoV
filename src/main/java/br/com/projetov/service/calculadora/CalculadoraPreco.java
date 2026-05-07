package br.com.projetov.service.calculadora;

import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.service.calculadora.regrascalculos.RegraDescontoPDV;
import br.com.projetov.service.calculadora.regrascalculos.RegraPreco;
import br.com.projetov.service.calculadora.regrascalculos.RegrasPorVolume;

import java.util.List;


public class CalculadoraPreco {
    private final List<RegraPreco> regras;

    public CalculadoraPreco(){
        this.regras = List.of(
                new RegraDescontoPDV(),
                new RegrasPorVolume()
        );
    }

    public double calcularVenda(TipoChopp tipoChopp, TipoVenda tipoVenda, double volumeMensal, boolean isConsignado) {
        double precoFinal = tipoChopp.getPrecoBase();
        for (RegraPreco regra : regras) {
            precoFinal = regra.aplicar(precoFinal, volumeMensal, tipoVenda, isConsignado);
        }
        return precoFinal;
    }
}