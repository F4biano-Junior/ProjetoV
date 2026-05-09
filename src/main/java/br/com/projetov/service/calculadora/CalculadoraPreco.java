package br.com.projetov.service.calculadora;

import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.service.calculadora.regrascalculos.RegraPreco;
import java.util.List;
import java.util.Objects;


public class CalculadoraPreco {
    private final List<RegraPreco> regras;

    public CalculadoraPreco(List<RegraPreco> regras) {
        Objects.requireNonNull(regras, "A lista de regras não pode ser nula.");
        if (regras.isEmpty()) {
            throw new IllegalArgumentException(
                    "A CalculadoraPreco exige ao menos uma RegraPreco registrada.");
        }
        this.regras = List.copyOf(regras); // defensivo: imutável internamente
    }

    public double calcularVenda(TipoChopp tipoChopp,
                                TipoVenda tipoVenda,
                                double volumeMensal,
                                boolean isConsignado) {

        Objects.requireNonNull(tipoChopp, "TipoChopp não pode ser nulo.");
        Objects.requireNonNull(tipoVenda, "TipoVenda não pode ser nulo.");

        double precoFinal = tipoChopp.getPrecoBase();

        for (RegraPreco regra : regras) {
            precoFinal = regra.aplicar(precoFinal, volumeMensal, tipoVenda, isConsignado);
        }

        // Trava de segurança: a empresa não paga o cliente para levar o chopp.
        return Math.max(0.0, precoFinal);
    }
}