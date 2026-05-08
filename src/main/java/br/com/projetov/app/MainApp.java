package br.com.projetov.app;


import br.com.projetov.config.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.stage.Stage;

public class MainApp extends Application {

//    @Override
//    public void init() throws Exception {
//        //Executando antes da janela abrir
//        SqliteConfig.inicializarBanco();
//    }

    @Override
    public void start(Stage stage) throws Exception {
        GlobalExceptionHandler.initialize();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/window.fxml")
        );
        Scene scene = new Scene(loader.load(), 760, 680);
        stage.setTitle("Sistema de Vendas de Chopp");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(620);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
    @Override
    public void stop() {
        // Garante que o processo Java encerre totalmente ao fechar a janela
        javafx.application.Platform.exit();
        System.exit(0);

    }
}
