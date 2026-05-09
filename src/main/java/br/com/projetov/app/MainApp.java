package br.com.projetov.app;


import br.com.projetov.config.GlobalExceptionHandler;

import br.com.projetov.controller.PedidoController;
import br.com.projetov.repository.PedidoRepositorySQLite;
import br.com.projetov.repository.SheetsRepository;
import br.com.projetov.sync.SyncWorker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private SyncWorker syncWorker;
    private SheetsRepository cloudRepository;

    @Override
    public void init(){
        PedidoRepositorySQLite localRepository = new PedidoRepositorySQLite();
        cloudRepository = new SheetsRepository();

        syncWorker = new SyncWorker(localRepository, cloudRepository);
        syncWorker.iniciar();
    }

    @Override
    public void stop(){
        if (syncWorker != null){
            syncWorker.encerrar();
        }
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        GlobalExceptionHandler.initialize();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/window.fxml")
        );
        Scene scene = new Scene(loader.load(), 900, 720);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css"),
                "style.css nao encontrado"
        ).toExternalForm());

        PedidoController controller = loader.getController();
        controller.atualizarStatusSincronizacao(
                cloudRepository != null && cloudRepository.isDisponivel()
        );

        stage.setTitle("Sistema de Vendas de Chopp");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(620);
        stage.show();
    }

}
