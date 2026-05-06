package br.com.projetov.models.enums;

public enum TipoVenda {
    PDV(1.10),  //10% de acréscimo para cobrir custos
    VENDA_DIRETA(1.0); //Preço padrão
    //Preço padrão

    private final double fator;
    TipoVenda(double fator) {this.fator = fator; }

    public double getFator() {return fator; }
}
