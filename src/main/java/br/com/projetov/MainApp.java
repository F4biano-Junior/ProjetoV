package br.com.projetov;

import br.com.projetov.config.SqliteConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Sistema de Distribuição de Chopp ativo!");
        Scene scene = new Scene(new StackPane(label), 400, 200);
        stage.setScene(scene);
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