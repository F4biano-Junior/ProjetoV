package br.com.projetov.models;

public enum CapacidadeBarril {
    L30(30),
    L50(50);

    private final int litros;
    CapacidadeBarril(int l) { this.litros = l; }
    public int getLitros() { return litros; }
}
