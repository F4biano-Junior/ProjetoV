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
        File propsFile = new File(userHome + File.separator +
                ".projetov" + File.separator + "application.properties");

        System.out.println("DEBUG [1]: Procurando arquivo em -> " + propsFile.getAbsolutePath());
        System.out.println("DEBUG [2]: Arquivo existe? -> " + propsFile.exists());

        if (propsFile.exists()) {
            try (FileInputStream input = new FileInputStream(propsFile)) {
                Properties prop = new Properties();
                prop.load(input);
                configuredSpreadsheetId = prop.getProperty("google.spreadsheet.id");

                System.out.println("DEBUG [3]: ID lido do arquivo -> " + configuredSpreadsheetId);

                if (configuredSpreadsheetId != null && !configuredSpreadsheetId.isBlank()) {
                    System.out.println("DEBUG [4]: ID é válido! Tentando criar o Google Sheets Service...");
                    configuredSheetsService = GoogleSheetsConfig.getSheetsService();
                    System.out.println("DEBUG [5]: Serviço Google foi criado com sucesso? -> " + true);
                } else {
                    System.out.println("DEBUG [4]: ID da planilha está nulo ou em branco no arquivo!");
                }
            } catch (Exception e) {
                System.err.println("ERRO no Google: " + e.getMessage());
            }
        }

        this.spreadsheetId  = configuredSpreadsheetId;
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

    private boolean isSyncDispositive() {
        return spreadsheetId == null || spreadsheetId.isBlank() || sheetsServices == null;
    }
}