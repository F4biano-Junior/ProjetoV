package br.com.projetov.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private String nomeCliente;
    private LocalDateTime dataHora;
    private List<ItemPedido> itens;
    private String entregador;

    public Pedido(String nomeCliente, String entregador) {
        this.nomeCliente = nomeCliente;
        this.entregador = entregador;
        this.dataHora = LocalDateTime.now();
        this.itens = new ArrayList<>();
    }

    // Metodo dinâmico que o App Móvel vai usar
    public void adicionarBarril(String codigo, TipoChopp tipo, CapacidadeBarril cap, double precoAtual) {
        this.itens.add(new ItemPedido(codigo, tipo, cap, precoAtual));
    }

    public double calcularTotalPedido() {
        return itens.stream().mapToDouble(ItemPedido::calcularSubtotal).sum();
    }

    // Getters...
}