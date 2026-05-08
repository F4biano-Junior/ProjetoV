package br.com.projetov.config;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class GlobalExceptionHandler {

    private GlobalExceptionHandler() {
    }

    public static void initialize() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.printf("Erro inesperado na thread '%s':%n", thread.getName());
            throwable.printStackTrace(System.err);

            if (Platform.isFxApplicationThread()) {
                showErrorAlert(throwable);
            } else {
                Platform.runLater(() -> showErrorAlert(throwable));
            }
        });
    }

    private static void showErrorAlert(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Inesperado");
        alert.setHeaderText("Ops! Algo deu errado na aplicação.");
        alert.setContentText(
                "Tente realizar a operação novamente. Se o problema continuar, contate o suporte."
        );

        TextArea stackTraceArea = new TextArea(getStackTrace(throwable));
        stackTraceArea.setEditable(false);
        stackTraceArea.setWrapText(false);
        stackTraceArea.setMaxWidth(Double.MAX_VALUE);
        stackTraceArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(stackTraceArea);
        alert.showAndWait();
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
