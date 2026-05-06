package br.com.projetov.repository;

import br.com.projetov.config.GoogleSheetsConfig;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.List;


public class SheetsRepository {
    private static final String SPREADSHEET_ID = "118xm_qewCyxhzgoNNDUUgzeI0RgNHn0R49Lsa9USgbQ";

    private final Sheets sheetsServices;

    public SheetsRepository() throws Exception {
        this.sheetsServices = GoogleSheetsConfig.getSheetsService();
    }

    public void atualizarCelula(String range, List<List<Object>> valores) throws Exception {

        ValueRange body = new ValueRange().setValues(valores);

        sheetsServices.spreadsheets().values()
                .update(SPREADSHEET_ID, range, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public List<List<Object>> lerDados(String range) throws Exception {

        ValueRange response = sheetsServices.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        return response.getValues();

    }
}