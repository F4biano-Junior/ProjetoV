package br.com.projetov.repository;

import br.com.projetov.config.GoogleSheetsConfig;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

public class SheetsRepository {

    private final String spreadsheetId;
    private final Sheets sheetsServices;

    public SheetsRepository() {
        String configuredSpreadsheetId = null;
        Sheets configuredSheetsService = null;

        String userHome = System.getProperty("user.home");
        File propsFile = new File(userHome + File.separator + ".projetov" + File.separator + "application.properties");

        if (propsFile.exists()) {
            try (FileInputStream input = new FileInputStream(propsFile)) {
                Properties prop = new Properties();
                prop.load(input);
                configuredSpreadsheetId = prop.getProperty("google.spreadsheet.id");

                if (configuredSpreadsheetId != null && !configuredSpreadsheetId.isBlank()) {
                    configuredSheetsService = GoogleSheetsConfig.getSheetsService();
                }
            } catch (Exception e) {
                System.err.println("ERRO: Sincronização Google Sheets indisponível. Falha ao inicializar: " + e.getMessage());
            }
        } else {
            System.err.println("AVISO: Arquivo application.properties não encontrado em " + propsFile.getAbsolutePath() + ". Modo offline ativo.");
        }

        this.spreadsheetId = configuredSpreadsheetId;
        this.sheetsServices = configuredSheetsService;
    }

    public boolean isDisponivel() {
        return !isSyncDispositive();
    }

    public void adicionarLinha(String range, List<List<Object>> valores) throws Exception {
        if (isSyncDispositive()) {
            return;
        }
        ValueRange body = new ValueRange().setValues(valores);
        sheetsServices.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public List<List<Object>> lerDado(String range) throws Exception {
        if (isSyncDispositive()) {
            return List.of();
        }
        ValueRange response = sheetsServices.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    private boolean isSyncDispositive() {
        return spreadsheetId == null || spreadsheetId.isBlank() || sheetsServices == null;
    }
}