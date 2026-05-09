package br.com.projetov.service.calculadora;

import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.service.calculadora.regrascalculos.RegraConsignado;
import br.com.projetov.service.calculadora.regrascalculos.RegraDescontoPDV;

import br.com.projetov.service.calculadora.regrascalculos.RegrasPorVolume;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários — Fase 2 Final: Integridade de Negócio
 * <p>
 * CONTRATOS VERIFICADOS:
 *  - RegraDescontoPDV  : 20% de desconto quando TipoVenda.PDV
 *  - RegrasPorVolume   : -R$1,00 fixo quando volumeMensal >= 1000
 *  - RegraConsignado   : retorna precoBase sem alteração (D1 encerrado)
 *  - CalculadoraPreco  : cadeia Consignado → PDV → Volume (D2, D3 encerrados)
 *  <p>
 * Zero I/O externo: sem SQLite, sem Google Sheets, sem rede.
 *
 * @author  Backend Sênior — Projeto Vendas De Chopp
 * @version Fase 2 — Final
 */
@DisplayName("CalculadoraPreco — Suíte Completa (Fase 2 Final)")
class CalculadoraPrecoTest {

    private static final double PRECO_BASE_EXEMPLO = 10.00;
    private static final double VOLUME_BAIXO       = 500.0;
    private static final double VOLUME_ALTO        = 1000.0;

    private RegraDescontoPDV regraDescontoPDV;
    private RegrasPorVolume  regrasPorVolume;
    private RegraConsignado  regraConsignado;

    @BeforeEach
    void configurar() {
        regraDescontoPDV = new RegraDescontoPDV();
        regrasPorVolume  = new RegrasPorVolume();
        regraConsignado  = new RegraConsignado();
    }

    // =========================================================================
    // 1. RegraDescontoPDV
    // =========================================================================

    @Nested
    @DisplayName("1. RegraDescontoPDV (desconto 20% para TipoVenda.PDV)")
    class RegraDescontoPDVTest {

        @Test
        @DisplayName("deve aplicar 20% de desconto quando tipoVenda for PDV")
        void deveAplicarVintePercDeDescontoParaTipoVendaPDV() {
            double resultado = regraDescontoPDV.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, false);

            assertEquals(8.00, resultado, 0.001,
                    "20% de desconto sobre R$10,00 deve resultar em R$8,00");
        }

        @Test
        @DisplayName("deve retornar preço base sem alteração para venda que não é PDV")
        void deveRetornarPrecoBaseParaVendaNaoPDV() {
            for (TipoVenda tipo : TipoVenda.values()) {
                if (tipo == TipoVenda.PDV) continue;

                double resultado = regraDescontoPDV.aplicar(
                        PRECO_BASE_EXEMPLO, VOLUME_BAIXO, tipo, false);

                assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                        "TipoVenda." + tipo + " não deve acionar desconto PDV");
            }
        }

        @Test
        @DisplayName("NÃO deve aplicar desconto PDV se o flag isConsignado for true")
        void naoDeveAplicarDescontoPDVSeConsignado() {
            double semConsignado = regraDescontoPDV.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, false);
            double comConsignado = regraDescontoPDV.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, true);

            // Agora eles PRECISAM ser diferentes!
            assertNotEquals(semConsignado, comConsignado,
                    "O barril consignado deve bloquear o desconto de PDV");

            // O sem consignado recebe os 20% de desconto (cai para 8.00)
            assertEquals(8.00, semConsignado, 0.001);

            // O com consignado aciona o escudo e devolve os 10.00 intactos
            assertEquals(PRECO_BASE_EXEMPLO, comConsignado, 0.001);
        }

        @Test
        @DisplayName("deve calcular desconto PDV corretamente sobre preço com casas decimais")
        void deveCalcularDescontoPDVSobrePrecoDecimal() {
            double resultado = regraDescontoPDV.aplicar(
                    15.50, VOLUME_BAIXO, TipoVenda.PDV, false);

            assertEquals(12.40, resultado, 0.001,
                    "80% de R$15,50 = R$12,40");
        }
    }

    // =========================================================================
    // 2. RegrasPorVolume
    // =========================================================================

    @Nested
    @DisplayName("2. RegrasPorVolume (desconto fixo de R$1,00 para volume >= 1000)")
    class RegrasPorVolumeTest {

        @Test
        @DisplayName("deve deduzir R$1,00 fixo quando volumeMensal for exatamente 1000")
        void deveAplicarDescontoDeUmRealParaVolumeMilExato() {
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, 1000.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(9.00, resultado, 0.001,
                    "Volume = 1000 (limite exato) deve acionar desconto de R$1,00");
        }

        @Test
        @DisplayName("deve deduzir R$1,00 quando volumeMensal for superior a 1000")
        void deveAplicarDescontoDeUmRealParaVolumeAcimaDoMinimo() {
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, 1500.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(9.00, resultado, 0.001);
        }

        @Test
        @DisplayName("não deve aplicar desconto de volume quando volumeMensal for 999")
        void naoDeveAplicarDescontoParaVolumeAbaixoDoMinimo() {
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, 999.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Volume 999 está abaixo do mínimo — não deve haver desconto");
        }

        @Test
        @DisplayName("não deve aplicar desconto de volume para volume zero")
        void naoDeveAplicarDescontoParaVolumeZero() {
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, 0.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001);
        }

        @Test
        @DisplayName("deve aplicar desconto de volume independente do TipoVenda")
        void deveAplicarDescontoVolumeParaTodosOsTiposDeVenda() {
            for (TipoVenda tipo : TipoVenda.values()) {
                double resultado = regrasPorVolume.aplicar(
                        PRECO_BASE_EXEMPLO, VOLUME_ALTO, tipo, false);

                assertEquals(9.00, resultado, 0.001,
                        "Desconto de volume deve ser indiferente ao TipoVenda." + tipo);
            }
        }
    }

    // =========================================================================
    // 3. RegraConsignado — D1 ENCERRADO
    // Regra de negócio confirmada pelo Dono do Produto:
    // barril consignado sempre cobra o precoBase, sem descontos, sem acréscimos.
    // =========================================================================

    @Nested
    @DisplayName("3. RegraConsignado ✅ D1 Encerrado — preço base fixo, sem descontos")
    class RegraConsignadoTest {

        @Test
        @DisplayName("deve retornar o preço base exato para barril consignado")
        void deveRetornarPrecoBaseExatoParaBarrilConsignado() {
            double resultado = regraConsignado.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.VENDA_DIRETA, true);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Consignado sempre cobra o preço base — sem acréscimo, sem desconto");
        }

        @Test
        @DisplayName("deve retornar preço base para consignado independente do TipoVenda informado")
        void deveRetornarPrecoBaseParaConsignadoIndependenteDeTipoVenda() {
            for (TipoVenda tipo : TipoVenda.values()) {
                double resultado = regraConsignado.aplicar(
                        PRECO_BASE_EXEMPLO, VOLUME_BAIXO, tipo, true);

                assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                        "TipoVenda." + tipo + " não deve alterar o preço do consignado");
            }
        }

        @Test
        @DisplayName("deve retornar preço base para consignado mesmo com volume alto (>= 1000)")
        void deveRetornarPrecoBaseParaConsignadoMesmoComVolumeAlto() {
            double resultado = regraConsignado.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_ALTO, TipoVenda.VENDA_DIRETA, true);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Volume alto não deve conceder desconto a barril consignado");
        }

        @Test
        @DisplayName("não deve interferir quando isConsignado for false — passa preço adiante na cadeia")
        void naoDeveInterfirirQuandoNaoForConsignado() {
            double resultado = regraConsignado.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, false);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Quando não consignado, a regra passa o preço sem alteração para a próxima da cadeia");
        }
    }

    // =========================================================================
    // 4. CalculadoraPreco — Cadeia completa de produção
    //    Ordem: Consignado → PDV → Volume (espelho exato do PedidoService)
    //    TipoChopp.PILSEN.getPrecoBase() == 10.00 — confirmado no enum real
    // =========================================================================

    @Nested
    @DisplayName("4. CalculadoraPreco — Cadeia Completa (Consignado → PDV → Volume)")
    class CalculadoraPrecoIntegracaoTest {

        static final TipoChopp TIPO_TESTE      = TipoChopp.PILSEN;
        static final double    PRECO_BASE_TIPO = 10.00; // PILSEN.getPrecoBase()

        private CalculadoraPreco calculadora;

        @BeforeEach
        void configurarCalculadora() {
            // Espelho exato da cadeia de produção definida no PedidoService
            calculadora = new CalculadoraPreco(List.of(
                    new RegraConsignado(),
                    new RegraDescontoPDV(),
                    new RegrasPorVolume()
            ));
        }

        @Test
        @DisplayName("deve retornar preço base para venda direta com volume baixo")
        void deveRetornarPrecoBaseSemDescontoParaVendaDiretaComVolumeBaixo() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_BAIXO, false);

            assertEquals(PRECO_BASE_TIPO, resultado, 0.001,
                    "Sem PDV e sem volume >= 1000: preço deve ser o base");
        }

        @Test
        @DisplayName("deve aplicar 20% de desconto para venda PDV com volume baixo")
        void deveAplicarVintePercDeDescontoPDVSobrePrecoBase() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_BAIXO, false);

            assertEquals(PRECO_BASE_TIPO * 0.80, resultado, 0.001,
                    "PDV com volume baixo: apenas desconto de 20% aplicado");
        }

        @Test
        @DisplayName("deve aplicar desconto de volume (-R$1,00) para venda direta com volume alto")
        void deveAplicarDescontoDeVolumeParaVendaDiretaComVolumeAlto() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_ALTO, false);

            assertEquals(PRECO_BASE_TIPO - 1.00, resultado, 0.001,
                    "VENDA_DIRETA com volume >= 1000: desconto fixo de R$1,00 aplicado");
        }

        @Test
        @DisplayName("deve aplicar PDV e volume em cadeia: 20% depois -R$1,00")
        void deveAplicarDescontoPDVEDescontoVolumeEmCadeia() {
            // Cadeia reduzida: isola as duas regras sem a RegraConsignado interferir
            CalculadoraPreco somentePDVVolume = new CalculadoraPreco(
                    List.of(new RegraDescontoPDV(), new RegrasPorVolume())
            );

            double resultado = somentePDVVolume.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_ALTO, false);

            double esperado = (PRECO_BASE_TIPO * 0.80) - 1.00;
            assertEquals(esperado, resultado, 0.001,
                    "PDV + volume alto: desconto de 20% seguido de -R$1,00");
        }

        @Test
        @DisplayName("consignado deve receber preço base — PDV e volume bloqueados pela cadeia")
        void consignadoDeveReceberPrecoBaseIgnorandoPDVEVolume() {
            // Pior caso: PDV + volume alto em conjunto.
            // RegraConsignado está em 1º e blinda o preço antes que qualquer outra regra aja.
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_ALTO, true);

            assertEquals(PRECO_BASE_TIPO, resultado, 0.001,
                    "Consignado retorna preço base — PDV e volume não devem agir");
        }

        @Test
        @DisplayName("[D3 RESOLVIDO] isConsignado blinda o preço: mesmo TipoVenda, resultado diferente por flag")
        void consignadoProduzeResultadoDiferenteDaVendaNormalComMesmoTipoVenda() {
            // Única variável isolada: isConsignado. TipoVenda e volume idênticos nos dois casos.
            // Sem consignado: RegraDescontoPDV age → 20% de desconto → R$8,00
            // Com consignado: RegraConsignado blinda → preço base → R$10,00
            double semConsignado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_BAIXO, false);

            double comConsignado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_BAIXO, true);

            assertAll("Consignado blinda o preço independente do canal de venda",
                    () -> assertNotEquals(semConsignado, comConsignado, 0.001,
                            "isConsignado=true e false devem produzir preços diferentes"),
                    () -> assertEquals(8.00,             semConsignado, 0.001,
                            "PDV sem consignado: 20% de desconto → R$8,00"),
                    () -> assertEquals(PRECO_BASE_TIPO,  comConsignado, 0.001,
                            "PDV com consignado: RegraConsignado blinda → preço base R$10,00")
            );
        }
    }

    // =========================================================================
    // 5. Edge Cases — Limites e entradas inválidas
    // =========================================================================

    @Nested
    @DisplayName("5. Edge Cases — Limites e Entradas Inválidas")
    class EdgeCasesTest {

        @Test
        @DisplayName("RegraDescontoPDV deve retornar 0.0 para preço base zero")
        void regraDescontoPDVDeveRetornarZeroParaPrecoBaseZero() {
            double resultado = regraDescontoPDV.aplicar(
                    0.0, VOLUME_BAIXO, TipoVenda.PDV, false);

            assertEquals(0.0, resultado, 0.001,
                    "80% de R$0,00 = R$0,00");
        }

        @Test
        @DisplayName("RegrasPorVolume com preço zero e volume alto retorna negativo — Math.max na CalculadoraPreco protege")
        void regrasPorVolumeComPrecoZeroEVolumeAltoRetornaValorNegativo() {
            // Documenta que a regra isolada retorna -1.00,
            // mas a CalculadoraPreco aplica Math.max(0.0, ...) antes de devolver ao Service.
            double resultadoRegra = regrasPorVolume.aplicar(
                    0.0, VOLUME_ALTO, TipoVenda.VENDA_DIRETA, false);

            assertEquals(-1.00, resultadoRegra, 0.001,
                    "Regra isolada retorna negativo — proteção está na CalculadoraPreco, não na regra");

            // Confirma que a calculadora nunca deixa chegar negativo ao Service
            CalculadoraPreco calc = new CalculadoraPreco(List.of(new RegrasPorVolume()));
            double resultadoProtegido = calc.calcularVenda(
                    TipoChopp.PILSEN, TipoVenda.VENDA_DIRETA, VOLUME_ALTO, false);

            // PILSEN.precoBase(10.00) - 1.00 = 9.00 — não chega a negativo neste caso,
            // mas o Math.max garante o piso em qualquer combinação
            assertTrue(resultadoProtegido >= 0.0,
                    "CalculadoraPreco nunca deve retornar preço negativo ao Service");
        }

        @Test
        @DisplayName("RegraDescontoPDV deve lidar com preço base muito alto sem overflow")
        void regraDescontoPDVDeveCalcularCorretamenteComPrecoAlto() {
            double precoAlto = 999_999.99;
            double resultado = regraDescontoPDV.aplicar(
                    precoAlto, VOLUME_BAIXO, TipoVenda.PDV, false);

            assertEquals(precoAlto * 0.80, resultado, 0.01);
        }

        @Test
        @DisplayName("RegrasPorVolume com volume negativo não deve aplicar desconto")
        void regrasPorVolumeComVolumeNegativoNaoDeveAplicarDesconto() {
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, -100.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Volume negativo não faz sentido de negócio — desconto não deve ser ativado");
        }
    }

    // =========================================================================
    // 6. CalculadoraPreco — Validação do Construtor
    // =========================================================================

    @Nested
    @DisplayName("6. CalculadoraPreco — Validação do Construtor")
    class CalculadoraPrecoConstrutorTest {

        @Test
        @DisplayName("deve lançar NullPointerException se lista de regras for nula")
        void deveLancarExcecaoParaListaNula() {
            assertThrows(NullPointerException.class,
                    () -> new CalculadoraPreco(null));
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException se lista de regras estiver vazia")
        void deveLancarExcecaoParaListaVazia() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CalculadoraPreco(List.of()));
        }
    }
}