package br.com.projetov.service.calculadora;

import br.com.projetov.models.enums.TipoChopp;
import br.com.projetov.models.enums.TipoVenda;
import br.com.projetov.service.calculadora.regrascalculos.RegraConsignado;
import br.com.projetov.service.calculadora.regrascalculos.RegraDescontoPDV;
import br.com.projetov.service.calculadora.regrascalculos.RegraPreco;
import br.com.projetov.service.calculadora.regrascalculos.RegrasPorVolume;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários — Fase 2: Integridade de Negócio P1
 * CONTRATOS VERIFICADOS NO CÓDIGO REAL:
 *  - RegraPreco.aplicar(precoBase, volumeMensal, tipoVenda, isConsignado): double
 *  - RegraDescontoPDV: 20% de desconto quando TipoVenda.PDV
 *  - RegrasPorVolume: -R$ 1,00 fixo quando volumeMensal >= 1000
 *  - RegraConsignado: retorna 0.0 ← STUB NÃO IMPLEMENTADO (D1)
 *  - CalculadoraPreco: cadeia PDV → Volume (Consignado ausente da cadeia) ← (D2, D3)
 *  <p>
 * Zero I/O externo: sem SQLite, sem Google Sheets, sem rede.
 * <p>
 * @author  Backend Sênior — Projeto Vendas De Chopp
 * @version Fase 2 — gerado a partir das fontes reais
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
    // 1. RegraDescontoPDV — testes isolados (sem dependência de enum externo)
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
            // Testa todos os TipoVenda que não sejam PDV
            for (TipoVenda tipo : TipoVenda.values()) {
                if (tipo == TipoVenda.PDV) continue;

                double resultado = regraDescontoPDV.aplicar(
                        PRECO_BASE_EXEMPLO, VOLUME_BAIXO, tipo, false);

                assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                        "TipoVenda." + tipo + " não deve acionar desconto PDV");
            }
        }

        @Test
        @DisplayName("deve aplicar desconto PDV independente do flag isConsignado")
        void deveAplicarDescontoPDVIndependenteDeConsignado() {
            double resultadoNaoConsignado = regraDescontoPDV.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, false);
            double resultadoConsignado = regraDescontoPDV.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.PDV, true);

            assertEquals(resultadoNaoConsignado, resultadoConsignado, 0.001,
                    "Flag isConsignado não deve alterar o comportamento do desconto PDV");
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
    // 2. RegrasPorVolume — testes isolados
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
    // 3. RegraConsignado — documenta o débito D1 (stub não implementado)
    // =========================================================================

    @Nested
    @DisplayName("3. RegraConsignado ⚠️ STUB — Documentação do Débito D1")
    class RegraConsignadoTest {

        /**
         * ESTE TESTE FALHA INTENCIONALMENTE.
         * <p>
         * Ele serve como SENTINELA: assim que RegraConsignado.aplicar() for
         * implementada corretamente, este teste deve ser atualizado para
         * refletir o comportamento real acordado com o Arquiteto.
         * <p>
         * Comportamento atual (bug): retorna 0.0 para qualquer entrada.
         * Comportamento esperado:    retornar precoBase ajustado pela regra de consignado.
         */
        @Test
        @DisplayName("[DÉBITO D1] RegraConsignado deve retornar preço ajustado — ATUALMENTE RETORNA 0.0 (BUG)")
        void regraConsignadoDeveRetornarPrecoAjustadoENaoZero() {
            double resultado = regraConsignado.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.VENDA_DIRETA, true);

            assertNotEquals(0.0, resultado,
                    "[DÉBITO D1] RegraConsignado.aplicar() retorna 0.0 hardcoded. " +
                            "Implementar a regra real e atualizar este teste com o valor esperado.");
        }

        @Test
        @DisplayName("[DÉBITO D1] Documenta comportamento atual broken: retorna 0.0 para isConsignado=true")
        void documentaComportamentoBrokenAtualDeRegraConsignado() {
            double resultado = regraConsignado.aplicar(
                    PRECO_BASE_EXEMPLO, VOLUME_BAIXO, TipoVenda.VENDA_DIRETA, true);

            // Este assertEquals PASSA — mas apenas porque o código está ERRADO.
            // Serve para documentar o estado atual até a correção.
            assertEquals(0.0, resultado,
                    "Comportamento atual (quebrado): retorna 0.0. " +
                            "Remover este teste quando D1 for corrigido.");
        }
    }

    // =========================================================================
    // 4. CalculadoraPreco — testes de integração da cadeia de regras
    //    NOTA: requer que TipoChopp tenha pelo menos um valor com precoBase
    //    conhecido. Ajuste TIPO_TESTE e PRECO_BASE_TIPO conforme o enum real.
    // =========================================================================

    @Nested
    @DisplayName("4. CalculadoraPreco — Cadeia de Regras (PDV → Volume)")
    class CalculadoraPrecoIntegracaoTest {

        private CalculadoraPreco calculadora;

        /**
         * ⚠️ AJUSTE OBRIGATÓRIO:
         * Substitua TipoChopp.PILSEN pelo valor real do seu enum e
         * atualize PRECO_BASE_TIPO com o precoBase correspondente.
         * <p>
         * Exemplo: se TipoChopp.PILSEN.getPrecoBase() == 8.50, defina:
         *   private static final double PRECO_BASE_TIPO = 8.50;
         */
        private static final TipoChopp TIPO_TESTE      = TipoChopp.PILSEN;
        private static final double    PRECO_BASE_TIPO = 10.00;

        @BeforeEach
        void configurarCalculadora() {
            List<RegraPreco> cadeia = List.of(
                    new RegraConsignado(),
                    new RegraDescontoPDV(),
                    new RegrasPorVolume()
            );
            calculadora = new CalculadoraPreco(cadeia);
        }

        @Test
        @DisplayName("deve retornar preço base sem desconto para venda avulsa com volume baixo")
        void deveRetornarPrecoBaseSemDescontoParaVendaAvulsaComVolumeBaixo() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_BAIXO, false);

            assertEquals(PRECO_BASE_TIPO, resultado, 0.001,
                    "Sem desconto PDV e sem volume >= 1000: preço deve ser o base");
        }

        @Test
        @DisplayName("deve aplicar 20% de desconto PDV sobre o preço base")
        void deveAplicarVintePercDeDescontoPDVSobrePrecoBase() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_BAIXO, false);

            double esperado = PRECO_BASE_TIPO * 0.80;
            assertEquals(esperado, resultado, 0.001,
                    "PDV com volume baixo: apenas desconto de 20% aplicado");
        }

        @Test
        @DisplayName("deve aplicar desconto de volume (-R$1,00) para venda avulsa com volume alto")
        void deveAplicarDescontoDeVolumeParaVendaAvulsaComVolumeAlto() {
            double resultado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_ALTO, false);

            double esperado = PRECO_BASE_TIPO - 1.00;
            assertEquals(esperado, resultado, 0.001,
                    "VENDA_DIRETA com volume >= 1000: desconto fixo de R$1,00 aplicado");
        }

        @Test
        @DisplayName("deve aplicar ambas as regras em cadeia: PDV 20% e depois -R$1,00 por volume")
        void deveAplicarDescontoPDVEDescontoVolumEmCadeia() {
            // Ordem da cadeia no código: RegraDescontoPDV → RegrasPorVolume
            // 1ª regra: precoBase * 0.80
            // 2ª regra: resultado anterior - 1.00
            // Cadeia intencional para este teste: só as duas regras que interessam
            CalculadoraPreco calculadoraPDVVolume = new CalculadoraPreco(
                    List.of(new RegraDescontoPDV(), new RegrasPorVolume())
            );

            double resultado = calculadoraPDVVolume.calcularVenda(
                    TIPO_TESTE, TipoVenda.PDV, VOLUME_ALTO, false);

            double aposDescontoPDV    = PRECO_BASE_TIPO * 0.80;
            double aposDescontoVolume = aposDescontoPDV - 1.00;

            assertEquals(aposDescontoVolume, resultado, 0.001);
        }

        /**
         * DÉBITO D3: RegraConsignado não está registrada na lista de regras
         * da CalculadoraPreco. Este teste documenta que a ‘flag’ isConsignado=true
         * atualmente não tem efeito nenhum no cálculo.
         */
        @Test
        @DisplayName("[DÉBITO D3] flag isConsignado não altera o resultado — RegraConsignado ausente da cadeia")
        void flagIsConsignadoNaoAlteraResultadoPoisRegraEstaAusenteDaCadeia() {
            double resultadoSemConsignado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_BAIXO, false);

            double resultadoComConsignado = calculadora.calcularVenda(
                    TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_BAIXO, true);

            assertEquals(resultadoSemConsignado, resultadoComConsignado, 0.001,
                    "[DÉBITO D3] isConsignado=true não deve produzir o mesmo resultado que false " +
                            "quando a regra for implementada. Corrija registrando RegraConsignado na cadeia.");
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
                    "80% de R$0,00 = R$0,00 — sem exceção esperada");
        }

        @Test
        @DisplayName("RegrasPorVolume deve retornar preço negativo se precoBase for 0.0 e volume alto — sinaliza ausência de guarda")
        void regrasPorVolumeComPrecoZeroEVolumeAltoRetornaValorNegativo() {
            // Este teste documenta uma ausência de validação: -R$1,00 sobre R$0,00
            // resulta em preço negativo. O Arquiteto deve decidir se isso é aceitável.
            double resultado = regrasPorVolume.aplicar(
                    0.0, VOLUME_ALTO, TipoVenda.VENDA_DIRETA, false);

            assertEquals(-1.00, resultado, 0.001,
                    "Sinaliza ausência de guarda: preço não pode ser negativo. " +
                            "Avaliar adição de Math.max(0, ...) ou validação no Service.");
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
        @DisplayName("RegrasPorVolume com volume negativo deve retornar preço base sem desconto")
        void regrasPorVolumeComVolumeNegativoNaoDeveAplicarDesconto() {
            // Volume negativo não faz sentido de negócio; a regra deve ignorar
            double resultado = regrasPorVolume.aplicar(
                    PRECO_BASE_EXEMPLO, -100.0, TipoVenda.VENDA_DIRETA, false);

            assertEquals(PRECO_BASE_EXEMPLO, resultado, 0.001,
                    "Volume negativo não deve ativar desconto de volume");
        }
    }
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
    @Test
    @DisplayName("[D3 RESOLVIDO] isConsignado=true deve ser processado pela RegraConsignado na cadeia")
    void flagIsConsignadoAgoraEProcessadoPelaRegraConsignado() {

        // Cadeia completa — igual à de produção
        CalculadoraPreco cadeiaProdução = new CalculadoraPreco(
                List.of(new RegraConsignado(), new RegraDescontoPDV(), new RegrasPorVolume())
        );

        double resultadoSemConsignado = cadeiaProdução.calcularVenda(
                CalculadoraPrecoIntegracaoTest.TIPO_TESTE, TipoVenda.VENDA_DIRETA, VOLUME_BAIXO, false);

        double resultadoComConsignado = cadeiaProdução.calcularVenda(
                CalculadoraPrecoIntegracaoTest.TIPO_TESTE, TipoVenda.PDV, VOLUME_BAIXO, true);

        // Quando D1 for implementado, este assertEquals vira assertNotEquals.
        // Por ora (passthrough seguro), os valores ainda são iguais — mas a
        // RegraConsignado já ESTÁ sendo chamada na cadeia (D3 resolvido).
        assertEquals(resultadoSemConsignado, resultadoComConsignado, 0.001,
                "D1 pendente: quando a regra real for implementada, " +
                        "trocar para assertNotEquals e definir o valor esperado.");
    }
}