package br.com.projetov.controller;

import br.com.projetov.models.enums.CapacidadeBarril;
import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.models.logistica.BarrilPedido;
import br.com.projetov.models.logistica.pedido.PedidoModel;
import br.com.projetov.repository.PedidoRepository;
import br.com.projetov.repository.PedidoRepositorySQLite;
import br.com.projetov.repository.SheetsRepository;
import br.com.projetov.service.PedidoService;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.text.NumberFormat;

import java.util.Locale;
import java.util.ResourceBundle;

public class PedidoController implements Initializable {

    @FXML
    private TextField txtCliente;
    @FXML
    private TextField txtEntregador;
    @FXML
    private TextField txtCodigoBarril;
    @FXML
    private ComboBox<TipoChopp> cmbTipoChopp;
    @FXML
    private ComboBox<CapacidadeBarril> cmbCapacidade;
    @FXML
    private ComboBox<TipoVenda> cmbTipoVenda;
    @FXML
    private TableView<BarrilPedido> tableBarris;
    @FXML
    private TableColumn<BarrilPedido, String> colCodigo;
    @FXML
    private TableColumn<BarrilPedido, String> colTipo;
    @FXML
    private TableColumn<BarrilPedido, String> colLitros;
    @FXML
    private TableColumn<BarrilPedido, String> colPrecoLitro;
    @FXML
    private TableColumn<BarrilPedido, String> colValorBarril;
    @FXML
    private TableColumn<BarrilPedido, String> colStatus;
    @FXML
    private Label lblResumoPedido;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnFinalizar;
    @FXML
    private CheckBox chkConsignado;
    @FXML
    private Circle statusSyncIndicator;
    @FXML
    private Label lblSyncStatus;

    private PedidoModel pedidoAtual;
    private final ObservableList<BarrilPedido> itensPedido = FXCollections.observableArrayList();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private final PedidoService pedidoService;

    public PedidoController() {
        try {
            PedidoRepository repositoryLocal = new PedidoRepositorySQLite();
            SheetsRepository repositoryNuvem = new SheetsRepository();


            this.pedidoService = new PedidoService(repositoryLocal, repositoryNuvem);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao inicializar dependencias do Controller: " + e.getMessage(), e);
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbTipoChopp.setItems(FXCollections.observableArrayList(TipoChopp.values()));
        cmbCapacidade.setItems(FXCollections.observableArrayList(CapacidadeBarril.values()));
        cmbTipoVenda.setItems(FXCollections.observableArrayList(TipoVenda.values()));

        configurarTabelaBarris();
        tableBarris.setItems(itensPedido);
        btnFinalizar.disableProperty().bind(Bindings.isEmpty(itensPedido));

        iniciarNovoPedido();
    }

    @FXML
    private void handleAdicionarBarril() {
        if (!validarCamposBarril()) {
            return;
        }

        String nomeCliente = txtCliente.getText().trim();
        if (!nomeCliente.isBlank()) {
            pedidoAtual.setNomeCliente(nomeCliente);
        }

        String codigo = txtCodigoBarril.getText().trim();
        TipoChopp tipo = cmbTipoChopp.getValue();
        CapacidadeBarril cap = cmbCapacidade.getValue();
        boolean consignado = chkConsignado.isSelected();

        try {
            pedidoService.adicionarBarrilAoPedido(pedidoAtual, codigo, tipo, cap, consignado);

            BarrilPedido barrilAdicionado = pedidoAtual.getBarris()
                    .getLast();
            itensPedido.add(barrilAdicionado);

            atualizarResumoPedido();
            limparCamposBarril();
        } catch (RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao Adicionar Barril",
                    "Nao foi possivel calcular o preco do barril.",
                    "Verifique a conexao com o banco de dados.\n\nDetalhe tecnico: " + e.getMessage()
            );
        }
    }

    @FXML
    private void handleRemoverBarril() {
        BarrilPedido barrilSelecionado = tableBarris.getSelectionModel().getSelectedItem();
        if (barrilSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Nenhum item selecionado",
                    "Selecione um barril na tabela para remover.", "");
            return;
        }

        pedidoAtual.removerBarril(barrilSelecionado.getCodigoBarril());
        itensPedido.remove(barrilSelecionado);
        atualizarResumoPedido();
    }

    @FXML
    private void handleFinalizarPedido() {
        if (!validarCamposPedido()) {
            return;
        }

        boolean confirmado = mostrarConfirmacao(
                "Confirmar gravacao do pedido para " + pedidoAtual.getNomeCliente() + "?",
                "Serao salvos " + pedidoAtual.getBarris().size() + " barril(is) no banco de dados."
        );
        if (!confirmado) {
            return;
        }

        try {
            pedidoService.finalizarPedido(pedidoAtual);

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pedido Finalizado",
                    "Pedido salvo com sucesso!",
                    "Cliente: " + pedidoAtual.getNomeCliente() +
                            "\nBarris: " + pedidoAtual.getBarris().size() +
                            "\nTotal: " + formatarMoeda(calcularTotalPedido())
            );

            handleLimpar();
        } catch (Exception e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao Finalizar Pedido",
                    "Nao foi possivel salvar o pedido.",
                    e.getMessage()
            );
        }
    }

    @FXML
    private void handleLimpar() {
        txtCliente.clear();
        txtEntregador.clear();
        txtCodigoBarril.clear();
        cmbTipoChopp.setValue(null);
        cmbCapacidade.setValue(null);
        cmbTipoVenda.setValue(null);
        itensPedido.clear();
        chkConsignado.setSelected(false);
        iniciarNovoPedido();
    }

    private void configurarTabelaBarris() {
        colCodigo.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getCodigoBarril()));
        colTipo.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getTipo().name()));
        colLitros.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getCapacidade().getLitros() + " L"));
        colPrecoLitro.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatarMoeda(cell.getValue().getPrecoVenda())));
        colValorBarril.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatarMoeda(cell.getValue().getSubtotal())));
        colStatus.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().isConsignado() ? "Consignado" : "Venda"));
    }

    private void iniciarNovoPedido() {
        pedidoAtual = new PedidoModel("", "", null);
        atualizarResumoPedido();
    }

    public void atualizarStatusSincronizacao(boolean sincronizacaoAtiva) {
        lblSyncStatus.setText(sincronizacaoAtiva ? "Sincronização Ativa" : "Aguardando conexão...");

        statusSyncIndicator.getStyleClass().removeAll("status-dot-online", "status-dot-offline");
        statusSyncIndicator.getStyleClass().add(
                sincronizacaoAtiva ? "status-dot-online" : "status-dot-offline"
        );
    }

    private void limparCamposBarril() {
        txtCodigoBarril.clear();
        cmbTipoChopp.setValue(null);
        cmbCapacidade.setValue(null);
        chkConsignado.setSelected(false);
        txtCodigoBarril.requestFocus();
    }

    private void atualizarResumoPedido() {
        int quantidadeBarris = pedidoAtual.getBarris().size();
        int litrosPedido = pedidoAtual.getBarris().stream()
                .mapToInt(b -> b.getCapacidade().getLitros())
                .sum();

        lblResumoPedido.setText(String.format("%d barril(is) | %d L", quantidadeBarris, litrosPedido));
        lblTotal.setText(formatarMoeda(calcularTotalPedido()));
    }

    private double calcularTotalPedido() {
        return pedidoAtual.getBarris().stream()
                .mapToDouble(BarrilPedido::getSubtotal)
                .sum();
    }

    private boolean validarCamposPedido() {
        if (txtCliente.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Nome do cliente nao preenchido", "Informe o cliente antes de finalizar.");
            txtCliente.requestFocus();
            return false;
        }
        if (txtEntregador.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Nome do entregador nao preenchido", "Informe o entregador antes de finalizar.");
            txtEntregador.requestFocus();
            return false;
        }
        if (cmbTipoVenda.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Modalidade nao selecionada", "Selecione PDV ou Venda Direta.");
            return false;
        }

        if (pedidoAtual.getBarris().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pedido vazio",
                    "Nenhum barril adicionado", "Adicione pelo menos um barril antes de finalizar.");
            return false;
        }

        PedidoModel pedidoFinal = new PedidoModel(
                txtCliente.getText().trim(),
                txtEntregador.getText().trim(),
                cmbTipoVenda.getValue()
        );
        pedidoAtual.getBarris().forEach(b ->
                pedidoFinal.adicionarBarril(
                        b.getCodigoBarril(),
                        b.getTipo(),
                        b.getCapacidade(),
                        b.getPrecoVenda(),
                        b.isConsignado()
                )
        );
        pedidoAtual = pedidoFinal;
        return true;
    }

    private boolean validarCamposBarril() {
        if (cmbTipoVenda.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Modalidade nao definida",
                    "Selecione a modalidade do pedido antes de adicionar barris.",
                    "PDV ou Venda Direta.");
            return false;
        }
        pedidoAtual.setTipoVenda(cmbTipoVenda.getValue());

        if (txtCodigoBarril.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Codigo do barril vazio", "Informe o codigo do barril.");
            txtCodigoBarril.requestFocus();
            return false;
        }
        if (cmbTipoChopp.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Tipo de chopp nao selecionado", "Selecione o tipo do chopp.");
            return false;
        }
        if (cmbCapacidade.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo obrigatorio",
                    "Capacidade nao selecionada", "Selecione a capacidade do barril.");
            return false;
        }
        return true;
    }

    private String formatarMoeda(double valor) {
        return moeda.format(valor);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo,
                               String cabecalho, String conteudo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    private boolean mostrarConfirmacao(String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Finalizar Pedido");
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        return alert.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .isPresent();
    }
}
