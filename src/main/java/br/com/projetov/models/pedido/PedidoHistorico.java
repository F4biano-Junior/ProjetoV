package br.com.projetov.models.pedido;

import br.com.projetov.models.enums.TipoVenda;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoHistorico(
        long id,
        String nomeCliente,
        String entregador,
        TipoVenda tipoVenda,
        LocalDateTime dataHora,
        boolean sincronizado,
        List<BarrilPedido> barris
) {
    public PedidoHistorico {
        barris = List.copyOf(barris);
    }

    public int quantidadeBarris() {
        return barris.size();
    }

    public int litrosTotal() {
        return barris.stream()
                .mapToInt(barril -> barril.getCapacidade().getLitros())
                .sum();
    }

    public double totalPedido() {
        return barris.stream()
                .mapToDouble(BarrilPedido::getSubtotal)
                .sum();
    }
}
