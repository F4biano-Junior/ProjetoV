package br.com.projetov.models.enums;

public enum CapacidadeBarril {
    L30(30),
    L50(50);

    private final int litros;
    CapacidadeBarril(int litros) { this.litros = litros; }

    public int getLitros() { return litros; }

    public static  CapacidadeBarril fromLitros(int litros){
        for (CapacidadeBarril c : values()){
            if (c.litros == litros ) return c;
        }
        throw new IllegalArgumentException(
                "Capacidade De Barril desconhecida para " + litros + " litros."
        );
    }

}
