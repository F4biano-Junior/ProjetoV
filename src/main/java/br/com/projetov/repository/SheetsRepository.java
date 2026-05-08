package br.com.projetov.repository;

import br.com.projetov.config.GoogleSheetsConfig;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class SheetsRepository {
    private final String spreadsheetId;
    private final Sheets sheetsServices;

    public SheetsRepository() {
        String configuredSpreadsheetId = null;
        Sheets configuredSheetsService = null;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                configuredSpreadsheetId = prop.getProperty("google.spreadsheet.id");
            }

            if (configuredSpreadsheetId != null && !configuredSpreadsheetId.isBlank()) {
                configuredSheetsService = GoogleSheetsConfig.getSheetsService();
            }
        } catch (Exception e) {
            System.err.println("Sincronizacao Google Sheets indisponivel. Modo offline ativo: " + e.getMessage());
        }

        this.spreadsheetId = configuredSpreadsheetId;
        this.sheetsServices = configuredSheetsService;
    }

    public void adicionarLinha(String range, List<List<Object>> valores) throws Exception {
        if (!isSyncDisponivel()) {
            return;
        }

        ValueRange body = new ValueRange().setValues(valores);

        sheetsServices.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public List<List<Object>> lerDados(String range) throws Exception {
        if (!isSyncDisponivel()) {
            return List.of();
        }

        ValueRange response = sheetsServices.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        return response.getValues();
    }

    private boolean isSyncDisponivel() {
        return spreadsheetId != null && !spreadsheetId.isBlank() && sheetsServices != null;
    }
}
