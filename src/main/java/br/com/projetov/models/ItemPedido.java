package br.com.projetov.models;

public class ItemPedido {
    private String codigoBarril;
    private TipoChopp tipo;
    private CapacidadeBarril capacidade;
    private double precoVendido; // O "Snapshot" do preço

    public ItemPedido(String codigoBarril, TipoChopp tipo, CapacidadeBarril capacidade, double precoNoMomento) {
        this.codigoBarril = codigoBarril;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.precoVendido = precoNoMomento;
    }

    // Getters


    public String getCodigoBarril() {return codigoBarril;}

    public TipoChopp getTipo() {return tipo;}

    public CapacidadeBarril getCapacidade() {return capacidade;}

    public double getPrecoVendido() {return precoVendido;}

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