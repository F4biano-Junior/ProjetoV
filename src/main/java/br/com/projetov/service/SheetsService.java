package br.com.projetov.service;

import br.com.projetov.models.logistica.Produto;
import br.com.projetov.repository.SheetsRepository;


import java.util.List;

public class SheetsService {
    private final SheetsRepository repository;


    public SheetsService() throws Exception {
        this.repository = new SheetsRepository();
    }
    public  void atualizarProduto(int linha, Produto produto) throws Exception{

        String range = "A" + linha + ":C" + linha;

        List<List<Object>> valores = List.of(
                List.of(
                        produto.getNome(),
                        produto.getTipo()

                )
        );
        repository.atualizarCelula(range, valores);
    }
}
