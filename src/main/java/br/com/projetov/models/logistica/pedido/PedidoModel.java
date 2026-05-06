package br.com.projetov.models.logistica.pedido;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.logistica.BarrilPedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PedidoModel {
    private String nomeCliente;
    private String entregador;
    private LocalDateTime dataHora;
    private List<BarrilPedido> barris;

    public PedidoModel(String nomeCliente, String entregador) {
        this.nomeCliente = nomeCliente;
        this.entregador = entregador;
        this.dataHora = LocalDateTime.now();
        this.barris = new ArrayList<>();
    }

    /**
     * Adiciona um barril dinamicamente.
     * Perfeito para o App Móvel onde o entregador adiciona vários itens.
     */
    public void adicionarBarril(String codigo, TipoChopp tipo, CapacidadeBarril cap, double volumeHistoricoCliente) {
        /* Futuramente criar um método que calcule quanto o cliente comprou no último mes e calcular um possível desconto */
        this.barris.add(new BarrilPedido(codigo, tipo, cap, volumeHistoricoCliente));
    }

    /**
     * Dica: Adicione este para o caso do entregador errar o código!
     */
    public void removerBarril(String codigo) {
        barris.removeIf(item -> item.getCodigoBarril().equals(codigo));
    }

    // Getters necessários para o SheetsRepository
    public List<BarrilPedido> getBarris() { return barris; }
    public String getNomeCliente() { return nomeCliente; }
    public String getEntregador() { return entregador; }
    public LocalDateTime getDataHora() { return dataHora; }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }
}