package br.com.projetov.models.logistica;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;

public class BarrilPedido {
    private String codigoBarril;
    private TipoChopp tipo;
    private CapacidadeBarril capacidade;
    private double precoVendido; // O "Snapshot" do preço

    public BarrilPedido(String codigoBarril, TipoChopp tipo, CapacidadeBarril capacidade, double volumeHistoricoCliente) {
        this.codigoBarril = codigoBarril;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.precoVendido = getPrecoVenda();
    }

    // Getters
    public String getCodigoBarril() {return codigoBarril;}

    public TipoChopp getTipo() {return tipo;}

    public CapacidadeBarril getCapacidade() {return capacidade;}

    public double getPrecoVenda() {return precoVendido;}
        //Valor total
    public double getSubtotal() {
        // Preço for por litro
        return capacidade.getLitros() * precoVendido;
    }

    // toString para facilitar a visualização no app
    @Override
    public String toString() {
        return String.format("Barril: %s | %s %sL | R$%.2f",
                codigoBarril, tipo, capacidade.getLitros(), precoVendido);
    }
}