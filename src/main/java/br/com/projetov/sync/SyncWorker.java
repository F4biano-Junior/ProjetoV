package br.com.projetov.sync;

import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.models.logistica.pedido.PedidoPendente;
import br.com.projetov.repository.PedidoRepository;
import br.com.projetov.repository.SheetsRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Worker de sincronização em background.
 *
 * <p>Roda numa única thread daemon separada da thread do JavaFX.
 * A cada {@value INTERVALO_SEGUNDOS} segundos, busca pedidos pendentes
 * no SQLite ({@code sincronizado = 0}) e tenta enviá-los ao Google Sheets.
 * Em caso de sucesso, marca o pedido como sincronizado no banco local.</p>
 *
 * <p>Em caso de falha de rede ou API, o pedido permanece com
 * {@code sincronizado = 0} e será reprocessado no próximo ciclo —
 * garantindo a resiliência offline-first.</p>
 *
 * Ciclo de vida:
 * <pre>
 *   Iniciar()   → chamado em MainApp.init()
 *   encerrar()  → chamado em MainApp.stop(), aguarda ciclo atual terminar
 * </pre>
 */
public class SyncWorker {

    private static final int INTERVALO_SEGUNDOS = 30;
    private static final int TIMEOUT_ENCERRAMENTO_SEGUNDOS = 15;

    private final PedidoRepository  localRepository;
    private final SheetsRepository  cloudRepository;
    private final ScheduledExecutorService scheduler;

    /**
     * @param localRepository fonte da verdade (SQLite)
     * @param cloudRepository destino de sync (Google Sheets)
     */
    public SyncWorker(PedidoRepository localRepository, SheetsRepository cloudRepository) {
        this.localRepository = localRepository;
        this.cloudRepository = cloudRepository;

        // Thread daemon: a JVM não espera por ela ao encerrar —
        // encerrar() garante o shutdown controlado antes do System.exit().
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "sync-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Inicia o ciclo periódico de sincronização.
     * Delay inicial de 10 segundos para não disputar recursos na inicialização da UI.
     */
    public void iniciar() {
        scheduler.scheduleWithFixedDelay(
                this::executarCiclo,
                10,                      // delay inicial
                INTERVALO_SEGUNDOS,      // intervalo entre ciclos
                TimeUnit.SECONDS
        );
        System.out.println("INFO [SyncWorker] iniciado — ciclo a cada "
                + INTERVALO_SEGUNDOS + "s.");
    }

    /**
     * Encerramento gracioso: aguarda o ciclo atual terminar antes de parar.
     * Chamado por MainApp.stop().
     */
    public void encerrar() {
        System.out.println("INFO [SyncWorker] encerrando...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(TIMEOUT_ENCERRAMENTO_SEGUNDOS, TimeUnit.SECONDS)) {
                System.err.println("AVISO [SyncWorker] timeout no encerramento — forçando parada.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("INFO [SyncWorker] encerrado.");
    }

    // -------------------------------------------------------------------------
    // Lógica interna do ciclo
    // -------------------------------------------------------------------------

    private void executarCiclo() {
        // Se o Sheets não estiver configurado, não há o que fazer.
        // Os pedidos ficam na fila até o usuário configurar as credenciais.
        if (!cloudRepository.isDisponivel()) {
            System.out.println("INFO [SyncWorker] Sheets não configurado — ciclo ignorado.");
            return;
        }

        List<PedidoPendente> pendentes = localRepository.buscarPendentesSync();

        if (pendentes.isEmpty()) return;

        System.out.printf("INFO [SyncWorker] %d pedido(s) pendente(s) encontrado(s).%n",
                pendentes.size());

        for (PedidoPendente pendente : pendentes) {
            tentarSincronizar(pendente);
        }
    }

    private void tentarSincronizar(PedidoPendente pendente) {
        try {
            enviarParaSheets(pendente.pedido());

            // Só marca como sincronizado se o envio não lançou exceção
            localRepository.marcarComoSincronizado(pendente.id());

            System.out.printf("INFO [SyncWorker] pedido id=%d sincronizado com sucesso.%n",
                    pendente.id());

        } catch (Exception e) {
            // Falha tolerada: pedido permanece sincronizado=0 e será reprocessado.
            System.err.printf("AVISO [SyncWorker] falha ao sincronizar id=%d: %s%n",
                    pendente.id(), e.getMessage());
        }
    }

    private void enviarParaSheets(PedidoModel pedido) throws Exception {
        double totalPedido = pedido.getBarris().stream()
                .mapToDouble(BarrilPedido::getSubtotal)
                .sum();

        boolean temConsignado = pedido.getBarris().stream()
                .anyMatch(BarrilPedido::isConsignado);

        List<List<Object>> linha = List.of(
                List.of(
                        pedido.getDataHora().toString(),
                        pedido.getNomeCliente(),
                        pedido.getEntregador(),
                        pedido.getTipoVenda().name(),
                        totalPedido,
                        temConsignado ? "Sim" : "Não"
                )
        );

        cloudRepository.adicionarLinha("Vendas!A2", linha);
    }
}