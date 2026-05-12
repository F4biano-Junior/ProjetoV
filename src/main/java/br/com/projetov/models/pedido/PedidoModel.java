package br.com.projetov.models.pedido;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PedidoModel {
    private String nomeCliente;
    private String entregador;
    private TipoVenda tipoVenda;
    private LocalDateTime dataHora;
    private List<BarrilPedido> barris;

    public PedidoModel(String nomeCliente, String entregador, TipoVenda tipoVenda) {
        this.nomeCliente = nomeCliente;
        this.entregador = entregador;
        this.tipoVenda = tipoVenda;
        this.dataHora = LocalDateTime.now();
        this.barris = new ArrayList<>();
    }

    public TipoVenda getTipoVenda() {return tipoVenda;}

    public void setTipoVenda(TipoVenda tipoVenda) {this.tipoVenda = tipoVenda;}

    /**
     * Adiciona um barril dinamicamente.
     * Perfeito para o App Móvel onde o entregador adiciona vários itens.
     */
    public void adicionarBarril(String codigo, TipoChopp tipo, CapacidadeBarril cap, double precoCalculado,
                                boolean consignado) {
        this.barris.add(new BarrilPedido(codigo, tipo, cap, precoCalculado, consignado));
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