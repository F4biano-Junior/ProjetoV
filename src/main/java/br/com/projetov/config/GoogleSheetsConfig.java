package br.com.projetov.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class GoogleSheetsConfig {
    private static final String APPLICATION_NAME = "Projeto Vendas";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Sheets getSheetsService() throws Exception {
        // 1. Resolve o caminho para a pasta oculta do usuário do SO
        String userHome = System.getProperty("user.home");
        File credenciais = new File(userHome + File.separator + ".projetov" + File.separator + "credentials.json");

        // 2. Validação Amigável: Avisa claramente se o arquivo não estiver lá
        if (!credenciais.exists()) {
            throw new IllegalStateException("FALHA DE SEGURANÇA: Arquivo de credenciais não encontrado. " +
                    "Por favor, coloque o 'credentials.json' na pasta: " + credenciais.getAbsolutePath());
        }

        GoogleCredentials autenticacaoGoogle;

        // 3. Try-with-resources: Garante que o arquivo seja fechado, evitando Memory Leak
        try (FileInputStream fis = new FileInputStream(credenciais)) {
            autenticacaoGoogle = GoogleCredentials.fromStream(fis)
                    .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
        }

        // 4. Log de Sucesso seguro (sem expor o conteúdo do arquivo)
        System.out.println("INFO: Credenciais do Google Sheets carregadas com sucesso de: " + credenciais.getAbsolutePath());

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(autenticacaoGoogle);

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}