package br.com.projetov.models;

public enum TipoChopp {
    Pilsen(10.00),
    Ipa(15.00),
    HopLager(12.00),
    Vienna(14.00);
    private final double precoPadrao;
    TipoChopp(double p)  {this.precoPadrao = p; }
    public double getPrecoBase() { return precoPadrao; }
}
