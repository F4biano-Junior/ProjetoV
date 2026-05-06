package br.com.projetov.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private String nomeCliente;
    private String entregador;
    private LocalDateTime dataHora;
    private List<ItemPedido> itens;

    public Pedido(String nomeCliente, String entregador) {
        this.nomeCliente = nomeCliente;
        this.entregador = entregador;
        this.dataHora = LocalDateTime.now();
        this.itens = new ArrayList<>();
    }

    /**
     * Adiciona um barril dinamicamente.
     * Perfeito para o App Móvel onde o entregador adiciona vários itens.
     */
    public void adicionarBarril(String codigo, TipoChopp tipo, CapacidadeBarril cap, double precoAtual) {
        this.itens.add(new ItemPedido(codigo, tipo, cap, precoAtual));
    }

    /**
     * Dica: Adicione este para o caso do entregador errar o código no pátio!
     */
    public void removerBarril(String codigo) {
        itens.removeIf(item -> item.getCodigoBarril().equals(codigo));
    }

    // Getters necessários para o SheetsRepository
    public List<ItemPedido> getItens() { return itens; }
    public String getNomeCliente() { return nomeCliente; }
    public String getEntregador() { return entregador; }
    public LocalDateTime getDataHora() { return dataHora; }
}