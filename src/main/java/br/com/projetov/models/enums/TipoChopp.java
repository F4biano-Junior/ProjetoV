package br.com.projetov.models.enums;

public enum TipoChopp {
    PILSEN(10.00),
    IPA(15.00),
    HOP_LAGER(12.00),
    VIENNA(14.00);
    private final double precoPadrao;
    TipoChopp(double preco)  {this.precoPadrao = preco; }
    public double getPrecoBase() { return precoPadrao; }
}
