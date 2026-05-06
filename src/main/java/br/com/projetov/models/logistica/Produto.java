package br.com.projetov.models.logistica;

import br.com.projetov.models.enums.TipoChopp;

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
