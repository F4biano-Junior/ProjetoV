package br.com.projetov.models.logistica;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;

public class BarrilPedido {
    private String codigoBarril;
    private TipoChopp tipo;
    private CapacidadeBarril capacidade;
    private double precoVendido;// O "Snapshot" do preço
    private boolean consignado;

    public BarrilPedido(String codigoBarril, TipoChopp tipo, CapacidadeBarril capacidade, double precoCalculado, boolean consignado) {
        this.codigoBarril = codigoBarril;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.precoVendido = precoCalculado;
        this.consignado = consignado;
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

    public boolean isConsignado() {
        return consignado;
    }

    //    // toString para facilitar a visualização no app
//    @Override
//    public String toString() {
//        return String.format("Barril: %s | %s %sL | R$%.2f",
//                codigoBarril, tipo, capacidade.getLitros(), precoVendido);
//    }
    // toString para o ListView — deixa claro visualmente
    @Override
    public String toString() {
        String tag = consignado ? " [CONSIGNADO]" : "";
        return String.format("%s | %s | %dL | R$ %.2f%s",
                codigoBarril, tipo.name(), capacidade.getLitros(), precoVendido, tag);
    }
}