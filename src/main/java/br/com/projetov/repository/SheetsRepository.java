package br.com.projetov.repository;

import br.com.projetov.config.GoogleSheetsConfig;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;


public class SheetsRepository {
    private String spreadsheetId;
    private final Sheets sheetsServices;


    public SheetsRepository() throws Exception {
        Properties prop = new Properties();
        try(InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")){
            if (input == null) {
                throw new RuntimeException("Desculpe, não consegui encontrar o application.properties");
            }
            prop.load(input);
            this.spreadsheetId = prop.getProperty("google.spreadsheet.id");
        }
        this.sheetsServices = GoogleSheetsConfig.getSheetsService();
    }

    public void adicionarLinha(String range, List<List<Object>> valores) throws Exception {

        ValueRange body = new ValueRange().setValues(valores);

        sheetsServices.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")

                .execute();
    }

    public List<List<Object>> lerDados(String range) throws Exception {

        ValueRange response = sheetsServices.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        return response.getValues();

    }
}