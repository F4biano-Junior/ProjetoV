package br.com.projetov.model;

public class ItemPedido {
    private String codigoBarril;
    private br.com.projetov.model.TipoChopp tipo;
    private br.com.projetov.model.CapacidadeBarril capacidade;
    private double precoVendido; // O "Snapshot" do preço

    public ItemPedido(String codigoBarril, TipoChopp tipo, CapacidadeBarril capacidade, double precoNoMomento) {
        this.codigoBarril = codigoBarril;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.precoVendido = precoNoMomento;
    }

    // Getters
    public double calcularSubtotal() {
        // Exemplo: se o preço for por litro
        return capacidade.getLitros() * precoVendido;
    }

    // toString para facilitar a visualização no app
    @Override
    public String toString() {
        return String.format("Barril: %s | %s %sL | R$%.2f",
                codigoBarril, tipo, capacidade.getLitros(), precoVendido);
    }
}