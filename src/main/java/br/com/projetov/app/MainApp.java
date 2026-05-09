package br.com.projetov.app;


import br.com.projetov.config.GlobalExceptionHandler;
import br.com.projetov.repository.PedidoRepository;
import br.com.projetov.repository.PedidoRepositorySQLite;
import br.com.projetov.repository.SheetsRepository;
import br.com.projetov.sync.SyncWorker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.stage.Stage;

public class MainApp extends Application {

    private SyncWorker syncWorker;

    @Override
    public void init(){
        PedidoRepositorySQLite localRepository = new PedidoRepositorySQLite();
        SheetsRepository       cloudRepository = new SheetsRepository();

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
        Scene scene = new Scene(loader.load(), 760, 680);
        stage.setTitle("Sistema de Vendas de Chopp");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(620);
        stage.show();
    }

}
