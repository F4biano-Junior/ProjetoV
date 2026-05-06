package br.com.projetov.models;

public class Produto {
    private  String nome;
    private TipoChopp tipo;

    public Produto(String nome, TipoChopp tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public TipoChopp getTipo() {
        return tipo;
    }

}
