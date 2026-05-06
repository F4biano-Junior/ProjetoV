package br.com.projetov;

import br.com.projetov.config.SqliteConfig;


public class Main {
    public static void main(String[] args) {
        SqliteConfig.inicializarBanco();
        System.out.println("O Sistema Vendas Chopp iniciou com sucesso!");

        // comando que inicia a MainApp.java
        MainApp.launch(MainApp.class, args);
        }
}