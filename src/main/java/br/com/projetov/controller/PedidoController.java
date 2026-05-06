package br.com.projetov.controller;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.repository.PedidoRepository;
import br.com.projetov.service.PedidoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class PedidoController implements Initializable {

    // ── Campos do formulário ───────────────────────────────────────────────
    @FXML private TextField txtCliente;
    @FXML private TextField txtEntregador;
    @FXML private TextField txtCodigoBarril;
    @FXML private ComboBox<TipoChopp>      cmbTipoChopp;
    @FXML private ComboBox<CapacidadeBarril> cmbCapacidade;
    @FXML private ComboBox<TipoVenda>      cmbTipoVenda;
    @FXML private ListView<String>         listViewBarris;
    @FXML private Label                    lblTotal;
    @FXML private Button                   btnFinalizar;

    // ── Estado interno ─────────────────────────────────────────────────────
    private PedidoModel pedidoAtual;
    private final ObservableList<String> itensListView = FXCollections.observableArrayList();

    // ── Camada de serviço (injeção via construtor, como seu Service espera) ─
    private final PedidoService pedidoService;

    public PedidoController() {
        // PedidoService recebe o repositório via construtor — respeitando
        // a arquitetura que você já definiu no PedidoService.java
        this.pedidoService = new PedidoService(new PedidoRepository());
    }

    // ── Inicialização do FXML ──────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Popula os ComboBoxes com os valores dos Enums
        cmbTipoChopp.setItems(FXCollections.observableArrayList(TipoChopp.values()));
        cmbCapacidade.setItems(FXCollections.observableArrayList(CapacidadeBarril.values()));
        cmbTipoVenda.setItems(FXCollections.observableArrayList(TipoVenda.values()));

        // Conecta a lista observável ao ListView — atualizações automáticas na UI
        listViewBarris.setItems(itensListView);

        // Prepara o primeiro pedido em branco
        iniciarNovoPedido();
    }

    // ── Handler: Adicionar Barril ──────────────────────────────────────────
    @FXML
    private void handleAdicionarBarril() {
        // 1. Valida os campos do barril antes de qualquer operação
        if (!validarCamposBarril()) return;
        // ← ADICIONAR ESTE BLOCO: sincroniza o nome do cliente no model
        //    antes de qualquer consulta ao banco
        String nomeCliente = txtCliente.getText().trim();
        if (!nomeCliente.isBlank()) {
            pedidoAtual.setNomeCliente(nomeCliente); // ← precisa do setter (ver abaixo)
        }
        String codigo           = txtCodigoBarril.getText().trim();
        TipoChopp tipo          = cmbTipoChopp.getValue();
        CapacidadeBarril cap    = cmbCapacidade.getValue();
        TipoVenda modalidade    = cmbTipoVenda.getValue();

        try {
            // 2. Delega ao Service — ele busca o volume mensal e calcula o preço
            pedidoService.adicionarBarrilAoPedido(pedidoAtual, codigo, tipo, cap, modalidade);

            // 3. Atualiza a ListView com o toString() do barril recém-adicionado
            //    Pega o último item inserido na lista do model
            String descricaoBarril = pedidoAtual.getBarris()
                    .get(pedidoAtual.getBarris().size() - 1)
                    .toString();
            itensListView.add(descricaoBarril);

            // 4. Recalcula e exibe o total
            atualizarTotal();

            // 5. Limpa apenas o campo de código para agilizar o próximo lançamento
            txtCodigoBarril.clear();

        } catch (RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao Adicionar Barril",
                    "Não foi possível calcular o preço do barril.",
                    "Verifique a conexão com o banco de dados.\n\nDetalhe técnico: " + e.getMessage()
            );
        }
    }

    // ── Handler: Finalizar Pedido ──────────────────────────────────────────
    @FXML
    private void handleFinalizarPedido() {
        // 1. Valida os dados obrigatórios do cabeçalho do pedido
        if (!validarCamposPedido()) return;

        // 2. Confirmação antes de gravar — evita finalizações acidentais
        boolean confirmado = mostrarConfirmacao(
                "Finalizar Pedido",
                "Confirmar gravação do pedido para " + pedidoAtual.getNomeCliente() + "?",
                "Serão salvos " + pedidoAtual.getBarris().size() + " barril(is) no banco de dados."
        );
        if (!confirmado) return;

        try {
            // 3. PedidoService valida se há barris e persiste via repositório
            pedidoService.finalizarPedido(pedidoAtual);

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pedido Finalizado",
                    "Pedido salvo com sucesso!",
                    "Cliente: " + pedidoAtual.getNomeCliente() +
                            "\nBarris: " + pedidoAtual.getBarris().size()
            );

            // 4. Reseta a tela para um novo pedido
            handleLimpar();

        } catch (Exception e) {
            // Captura tanto IllegalArgumentException (pedido sem barris)
            // quanto RuntimeException (falha de banco vinda do Repository)
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao Finalizar Pedido",
                    "Não foi possível salvar o pedido.",
                    e.getMessage()
            );
        }
    }

    // ── Handler: Limpar tela ───────────────────────────────────────────────
    @FXML
    private void handleLimpar() {
        txtCliente.clear();
        txtEntregador.clear();
        txtCodigoBarril.clear();
        cmbTipoChopp.setValue(null);
        cmbCapacidade.setValue(null);
        cmbTipoVenda.setValue(null);
        itensListView.clear();
        lblTotal.setText("R$ 0,00");
        iniciarNovoPedido();
    }

    // ── Helpers privados ───────────────────────────────────────────────────

    /**
     * Cria um PedidoModel novo. Chamado no initialize() e após cada finalização.
     * O nomeCliente e entregador são atualizados dinamicamente no momento
     * da finalização — o PedidoModel é apenas o container dos barris por enquanto.
     */
    private void iniciarNovoPedido() {
        // Pedido começa com strings vazias; os campos reais são capturados
        // no momento de finalizar, garantindo que o usuário preencha antes de salvar
        pedidoAtual = new PedidoModel("", "");
    }

    /** Recalcula e atualiza o Label de total com base nos barris do model. */
    private void atualizarTotal() {
        double total = pedidoAtual.getBarris().stream()
                .mapToDouble(b -> b.getSubtotal())
                .sum();
        lblTotal.setText(String.format("R$ %.2f", total));
    }

    /**
     * Valida campos do cabeçalho do pedido.
     * Também sincroniza o PedidoModel com os campos de texto neste momento,
     * pois o model é o único lugar que o Service lê.
     */
    private boolean validarCamposPedido() {
        if (txtCliente.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Nome do cliente não preenchido", "Informe o cliente antes de finalizar.");
            txtCliente.requestFocus();
            return false;
        }
        if (txtEntregador.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Nome do entregador não preenchido", "Informe o entregador antes de finalizar.");
            txtEntregador.requestFocus();
            return false;
        }
        if (pedidoAtual.getBarris().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pedido vazio",
                    "Nenhum barril adicionado", "Adicione pelo menos um barril antes de finalizar.");
            return false;
        }

        // Recria o PedidoModel com os dados corretos do formulário,
        // preservando os barris já adicionados
        PedidoModel pedidoFinal = new PedidoModel(
                txtCliente.getText().trim(),
                txtEntregador.getText().trim()
        );
        pedidoAtual.getBarris().forEach(b ->
                pedidoFinal.adicionarBarril(
                        b.getCodigoBarril(),
                        b.getTipo(),
                        b.getCapacidade(),
                        b.getPrecoVenda()   // preço já calculado — não recalcula
                )
        );
        pedidoAtual = pedidoFinal;
        return true;
    }

    /** Valida os campos do formulário de barril antes de adicionar. */
    private boolean validarCamposBarril() {
        if (txtCodigoBarril.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Código do barril vazio", "Informe o código do barril.");
            txtCodigoBarril.requestFocus();
            return false;
        }
        if (cmbTipoChopp.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Tipo de chopp não selecionado", "Selecione o tipo do chopp.");
            return false;
        }
        if (cmbCapacidade.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Capacidade não selecionada", "Selecione a capacidade do barril.");
            return false;
        }
        if (cmbTipoVenda.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatório",
                    "Modalidade não selecionada", "Selecione PDV ou Venda Direta.");
            return false;
        }
        return true;
    }

    /** Fábrica centralizada para todos os Alerts do Controller. */
    private void mostrarAlerta(Alert.AlertType tipo, String titulo,
                               String cabecalho, String conteudo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    /** Diálogo de confirmação — retorna true se o usuário clicou em OK. */
    private boolean mostrarConfirmacao(String titulo, String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        return alert.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .isPresent();
    }
}