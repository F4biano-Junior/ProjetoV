package br.com.projetov;

import br.com.projetov.app.MainApp;
import br.com.projetov.config.DatabaseInitializer;


public class Main {
    public static void main(String[] args) {

        try {
            DatabaseInitializer.inicializar();
            System.out.println("Banco de dados verificado com sucesso.");
        } catch (Exception e) {
            System.err.println("Erro fatal: Não foi possível preparar o banco de dados.");
            e.printStackTrace();
        }
        // comando que inicia a MainApp.java
        MainApp.launch(MainApp.class, args);
        }
}