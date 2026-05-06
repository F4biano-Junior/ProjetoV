package br.com.projetov.model;

public enum TipoChopp {
    Pilsen(10.00),
    Ipa(15.00),
    HopLager(12.00),
    Vienna(14.00);
    private final double precoBase;

    TipoChopp(double precoBase) {
        this.precoBase = precoBase;
    }

    public double getPrecoBase() {
        return precoBase;
    }
}
