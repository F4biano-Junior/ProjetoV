package br.com.projetov.models.enums;

public enum CapacidadeBarril {
    L30(30),
    L50(50);

    private final int litros;
    CapacidadeBarril(int litros) { this.litros = litros; }
    public int getLitros() { return litros; }
}
