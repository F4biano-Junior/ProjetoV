# Documentacao Tecnica Oficial - Fase 2

Projeto: **Projeto Vendas de Chopp**  
Fase encerrada: **Fase 2 - Integridade de Dados e Negocio**  
Ultima validacao local: `.\gradlew.bat test` em 2026-05-09  
Status da validacao: **BUILD SUCCESSFUL**

---

## 1. Visao Geral do Sistema e Stack Tecnologica

O **Projeto Vendas de Chopp** e uma aplicacao desktop em JavaFX para registrar pedidos de venda de chopp, calcular precos de barris conforme regras comerciais, persistir os dados localmente em SQLite e sincronizar informacoes relevantes com Google Sheets.

O sistema foi estruturado para apoiar uma operacao comercial com os seguintes fluxos principais:

- cadastro de dados basicos do pedido, como cliente, entregador e modalidade de venda;
- adicao de barris ao pedido, com tipo de chopp, capacidade, codigo e flag de consignado;
- calculo do preco de venda por barril usando regras de negocio centralizadas;
- persistencia local transacional em SQLite;
- sincronizacao opcional com Google Sheets quando as credenciais e o identificador da planilha estiverem configurados.

### Stack atual

- **Java 21**: configurado via Gradle Toolchain.
- **JavaFX 21**: usado para a interface grafica, com os modulos `javafx.controls` e `javafx.fxml`.
- **Gradle**: gerenciador de build, dependencias e execucao de testes.
- **SQLite**: persistencia local via `org.xerial:sqlite-jdbc`.
- **Google Sheets API**: integracao com planilhas via bibliotecas oficiais do Google.
- **JUnit 5**: suite de testes automatizados do motor financeiro.
- **SLF4J Simple**: logging simples para bibliotecas que dependem de SLF4J.

O ponto de entrada configurado no Gradle e:

```groovy
application {
    mainClass = 'br.com.projetov.Main'
}
```

A classe `Main` inicializa o banco via `DatabaseInitializer` e em seguida inicia a aplicacao JavaFX (`MainApp`). A classe `MainApp` carrega o FXML principal (`window.fxml`) e registra o `GlobalExceptionHandler`, responsavel por capturar excecoes nao tratadas e apresentar um alerta amigavel ao usuario.

---

## 2. Arquitetura e Padroes de Desenho

O projeto segue uma separacao em camadas simples e adequada para o tamanho atual da aplicacao:

```text
Controller -> Service -> Repository -> Banco/Integracoes
     |            |
     |            -> Motor financeiro / regras de negocio
     |
     -> FXML / JavaFX
```

### Model

A camada de modelagem fica em `br.com.projetov.models`.

Principais modelos e enums:

- `PedidoModel`: representa o cabecalho do pedido, com cliente, entregador, tipo de venda, data/hora e lista de barris.
- `BarrilPedido`: representa cada item do pedido, armazenando codigo do barril, tipo, capacidade, preco vendido e flag de consignado.
- `TipoChopp`: enum com os tipos de chopp e seus precos base (`PILSEN`, `IPA`, `HOP_LAGER`, `VIENNA`).
- `TipoVenda`: enum com as modalidades `PDV` e `VENDA_DIRETA`.
- `CapacidadeBarril`: enum com capacidades atualmente suportadas (`L30` e `L50`).

### Repository

A camada Repository abstrai a persistencia e integracoes externas.

- `PedidoRepository`: contrato para salvar pedidos e consultar volume mensal por cliente.
- `PedidoRepositorySQLite`: implementacao SQLite. Salva pedido e barris em transacao, usando `commit` e `rollback`.
- `SheetsRepository`: encapsula leitura/escrita no Google Sheets, operando em modo offline quando a configuracao local nao esta presente.

Esse desenho facilita trocar a persistencia local no futuro, desde que o novo repositorio implemente `PedidoRepository`.

### Service

A camada Service concentra regras de negocio e orquestracao.

O `PedidoService` recebe `PedidoRepository` e `SheetsRepository` por construtor. Em producao, ele monta a cadeia padrao de regras financeiras:

```java
List<RegraPreco> cadeia = List.of(
    new RegraConsignado(),
    new RegraDescontoPDV(),
    new RegrasPorVolume()
);
this.calcular = new CalculadoraPreco(cadeia);
```

Tambem existe um segundo construtor que recebe uma `CalculadoraPreco`, permitindo injecao explicita em cenarios de teste ou composicoes futuras.

Responsabilidades atuais do `PedidoService`:

- buscar o volume mensal historico do cliente no repositorio local;
- chamar a calculadora de preco;
- adicionar o barril ao pedido com o preco calculado;
- validar que nao se salva pedido vazio;
- persistir o pedido localmente;
- tentar sincronizar os dados principais com Google Sheets.

### Controller

O `PedidoController` atua como ponte entre a UI JavaFX e a camada de Service.

O controller foi limpo das principais regras de negocio de preco: ele nao instancia nem chama diretamente a `CalculadoraPreco` para calcular valores de venda. A adicao de barril delega para:

```java
pedidoService.adicionarBarrilAoPedido(pedidoAtual, codigo, tipo, cap, consignado);
```

Ainda existem responsabilidades de apresentacao dentro do controller, como:

- validacao de campos obrigatorios da tela;
- exibicao de alertas;
- atualizacao de resumo visual;
- formatacao monetaria;
- montagem final do `PedidoModel` antes da finalizacao.

Essas responsabilidades sao aceitaveis para a fase atual, mas ha oportunidades futuras de refinamento: criar um `PedidoViewModel`, mover agregados como total/litros para o dominio ou service, e usar uma factory/contexto para evitar que o controller conheca diretamente implementacoes de repositorio.

### Padroes de desenho identificados

- **Repository Pattern**: `PedidoRepository` define o contrato e `PedidoRepositorySQLite` implementa persistencia local.
- **Strategy Pattern**: cada regra de preco implementa `RegraPreco`.
- **Chain/Pipeline de Regras**: `CalculadoraPreco` aplica uma lista ordenada de strategies para chegar ao preco final.
- **Constructor Injection**: `PedidoService` recebe dependencias por construtor, especialmente repositorios e calculadora.
- **Defensive Copy / Imutabilidade Interna**: `CalculadoraPreco` usa `List.copyOf(regras)` para proteger sua cadeia interna.
- **Global Exception Handler**: `GlobalExceptionHandler` configura `Thread.setDefaultUncaughtExceptionHandler` e redireciona alertas para a thread JavaFX com `Platform.runLater`.

---

## 3. O Motor Financeiro: Regras de Negocio

O motor financeiro fica em:

```text
src/main/java/br/com/projetov/service/calculadora
```

Ele e composto por:

- `CalculadoraPreco`
- `RegraPreco`
- `RegraConsignado`
- `RegraDescontoPDV`
- `RegrasPorVolume`

### Interface RegraPreco

`RegraPreco` define o contrato Strategy:

```java
double aplicar(double precoBase, double volumeMensal, TipoVenda tipoVenda, boolean isConsignado);
```

Cada regra recebe o preco corrente e decide se altera ou nao o valor.

### CalculadoraPreco

A `CalculadoraPreco` recebe uma lista ordenada de regras. O calculo parte do preco base do `TipoChopp`:

```java
double precoFinal = tipoChopp.getPrecoBase();
```

Depois aplica cada regra em sequencia:

```java
for (RegraPreco regra : regras) {
    precoFinal = regra.aplicar(precoFinal, volumeMensal, tipoVenda, isConsignado);
}
```

Ao final, aplica uma trava de seguranca:

```java
return Math.max(0.0, precoFinal);
```

Essa protecao impede que descontos em cadeia gerem preco negativo.

### RegraDescontoPDV

Regra:

- se `isConsignado == true`, nao aplica desconto;
- se `tipoVenda == TipoVenda.PDV`, aplica 20% de desconto;
- caso contrario, devolve o preco sem alteracao.

Formula:

```text
precoFinal = precoBase * 0.80
```

Exemplo:

```text
PILSEN base R$10,00 em PDV -> R$8,00
```

### RegrasPorVolume

Regra:

- se `isConsignado == true`, nao aplica desconto;
- se `volumeMensal >= 1000`, aplica desconto fixo de R$1,00;
- caso contrario, devolve o preco sem alteracao.

Formula:

```text
precoFinal = precoBase - 1.00
```

Exemplo:

```text
PILSEN base R$10,00 com volume >= 1000 L -> R$9,00
```

### RegraConsignado

Regra de negocio definida:

- barril consignado sempre cobra o preco base;
- barril consignado nao recebe desconto PDV;
- barril consignado nao recebe desconto por volume.

No estado atual do codigo, o escudo de consignado e garantido por duas protecoes combinadas:

1. `RegraConsignado` devolve o preco base quando `isConsignado == true`.
2. `RegraDescontoPDV` e `RegrasPorVolume` tambem verificam `isConsignado` e retornam o preco sem desconto.

Assim, mesmo que o pedido seja `PDV` e o cliente tenha volume mensal alto, um barril consignado permanece com o preco base.

Exemplo:

```text
PILSEN base R$10,00
TipoVenda.PDV
volumeMensal >= 1000
isConsignado = true

Resultado: R$10,00
```

### Ordem da cadeia de producao

A cadeia registrada no `PedidoService` e:

```text
RegraConsignado -> RegraDescontoPDV -> RegrasPorVolume
```

Para venda normal, PDV e volume podem atuar em sequencia:

```text
PILSEN R$10,00
PDV: R$10,00 * 0.80 = R$8,00
Volume >= 1000: R$8,00 - R$1,00 = R$7,00
```

Para consignado, os descontos sao bloqueados:

```text
PILSEN R$10,00
Consignado: R$10,00
PDV: nao altera
Volume: nao altera
Resultado: R$10,00
```

---

## 4. Estrutura de Base de Dados: Persistencia Local

O schema local esta em:

```text
src/main/resources/schema.sql
```

O banco SQLite fisico utilizado pela aplicacao e:

```text
vendas_de_chopp.db
```

conectado via:

```text
jdbc:sqlite:vendas_de_chopp.db
```

### Tabela pedido_model

Tabela principal que representa o cabecalho da venda.

Colunas atuais no `schema.sql`:

| Coluna | Tipo | Observacao |
|---|---:|---|
| `id` | INTEGER | chave primaria autoincremental |
| `cliente` | TEXT | nome do cliente, obrigatorio |
| `entregador` | TEXT | nome do entregador, obrigatorio |
| `tipo_venda` | TEXT | persiste `TipoVenda.name()`, como `PDV` ou `VENDA_DIRETA` |
| `sincronizado` | INTEGER | controle de sincronizacao com Sheets; `0` = pendente, `1` = enviado |
| `data_hora` | DATETIME | default `CURRENT_TIMESTAMP` |

Nota de integridade: as colunas `tipo_venda` e `sincronizado` ja estao presentes no `schema.sql`. No estado atual do repositorio, o `PedidoRepositorySQLite` insere explicitamente `tipo_venda`; a coluna `sincronizado` usa o default `0`, deixando novos pedidos marcados como pendentes para a futura fila de sincronizacao.

### Tabela barris_pedido

Tabela filha que representa os itens do pedido.

| Coluna | Tipo | Observacao |
|---|---:|---|
| `id` | INTEGER | chave primaria autoincremental |
| `pedido_id` | INTEGER | chave estrangeira para `pedido_model(id)` |
| `codigo_barril` | TEXT | codigo operacional do barril |
| `tipo_chopp` | TEXT | persiste `TipoChopp.name()` |
| `capacidade` | INTEGER | litros do barril, vindo de `CapacidadeBarril.getLitros()` |
| `preco_venda` | REAL | preco por litro calculado pelo motor financeiro |
| `consignado` | INTEGER | `0` para nao, `1` para sim |

A relacao com `pedido_model` usa:

```sql
FOREIGN KEY (pedido_id) REFERENCES pedido_model(id) ON DELETE CASCADE
```

### DatabaseInitializer

`DatabaseInitializer` e responsavel por preparar o banco no inicio da aplicacao.

Fluxo:

1. le `/schema.sql` a partir de `src/main/resources`;
2. interpreta o arquivo como UTF-8;
3. divide os comandos por `;`;
4. abre uma conexao via `ConnectionFactory`;
5. executa cada comando nao vazio.

Como o schema usa `CREATE TABLE IF NOT EXISTS`, a inicializacao pode ser executada repetidamente sem recriar tabelas existentes.

### ConnectionFactory

`ConnectionFactory` centraliza a criacao de conexoes SQLite.

Responsabilidades:

- manter a URL JDBC do banco local;
- abrir novas conexoes via `DriverManager.getConnection`;
- encapsular `SQLException` em `RuntimeException` com mensagem amigavel.

### PedidoRepositorySQLite

`PedidoRepositorySQLite` implementa persistencia transacional:

- desativa `autoCommit`;
- insere o cabecalho do pedido;
- recupera o ID gerado;
- insere os barris em batch;
- confirma com `commit`;
- em caso de erro, tenta `rollback`.

Tambem implementa `buscarVolumeMensalCliente`, somando a capacidade dos barris vendidos ao cliente nos ultimos 30 dias. Esse volume e usado pelo `PedidoService` para ativar ou nao a regra de volume.

---

## 5. Qualidade e Cobertura de Testes

A suite automatizada atual esta em:

```text
src/test/java/br/com/projetov/service/calculadora/CalculadoraPrecoTest.java
```

Ela cobre o motor financeiro sem I/O externo: nao acessa SQLite, Google Sheets nem rede.

Resultado validado localmente:

```text
.\gradlew.bat test
BUILD SUCCESSFUL
25 testes executados
0 falhas
0 erros
```

### Grupos de teste

A suite esta organizada em classes aninhadas:

- `RegraDescontoPDVTest`: valida desconto de 20% para `TipoVenda.PDV`, ausencia de desconto em venda direta e blindagem de consignado.
- `RegrasPorVolumeTest`: valida desconto fixo de R$1,00 para volume igual ou superior a 1000 litros, ausencia de desconto abaixo do limite, volume zero e aplicabilidade por tipo de venda.
- `RegraConsignadoTest`: garante preco base fixo para barril consignado, independente de tipo de venda e volume.
- `CalculadoraPrecoIntegracaoTest`: valida a cadeia completa `Consignado -> PDV -> Volume` com o mesmo arranjo usado em producao no `PedidoService`.
- `EdgeCasesTest`: cobre preco zero, volume negativo, preco alto e protecao contra retorno negativo.
- `CalculadoraPrecoConstrutorTest`: valida que a calculadora nao aceita lista de regras nula ou vazia.

### Protecoes relevantes

A protecao mais importante do motor e:

```java
return Math.max(0.0, precoFinal);
```

Isso garante que o Service nunca receba preco negativo, mesmo que regras futuras ou combinacoes extremas produzam valores abaixo de zero.

A suite tambem documenta explicitamente o comportamento de uma regra isolada que pode produzir valor negativo (`RegrasPorVolume` sobre preco zero) e confirma que a responsabilidade de piso minimo esta centralizada na `CalculadoraPreco`.

### Observacao sobre contagem

O relatorio do Gradle confirma **25 casos de teste JUnit** executados com sucesso. Dentro desses testes ha multiplas assercoes, incluindo `assertEquals`, `assertNotEquals`, `assertTrue`, `assertThrows` e `assertAll`.

---

## 6. Estrutura de Ficheiros/Diretorios

Arvore principal do projeto:

```text
ProjetoV/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── ProjetoV_Documentacao.docx
├── vendas_de_chopp.db
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/
    │   │   └── br/
    │   │       └── com/
    │   │           └── projetov/
    │   │               ├── Main.java
    │   │               ├── app/
    │   │               │   └── MainApp.java
    │   │               ├── config/
    │   │               │   ├── ConnectionFactory.java
    │   │               │   ├── DatabaseInitializer.java
    │   │               │   ├── GlobalExceptionHandler.java
    │   │               │   └── GoogleSheetsConfig.java
    │   │               ├── controller/
    │   │               │   └── PedidoController.java
    │   │               ├── models/
    │   │               │   ├── enums/
    │   │               │   │   ├── CapacidadeBarril.java
    │   │               │   │   ├── TipoChopp.java
    │   │               │   │   └── TipoVenda.java
    │   │               │   ├── logistica/
    │   │               │   │   ├── BarrilPedido.java
    │   │               │   │   ├── Produto.java
    │   │               │   │   └── pedido/
    │   │               │   │       └── PedidoModel.java
    │   │               │   └── pessoal/
    │   │               │       ├── Cliente.java
    │   │               │       └── Entregador.java
    │   │               ├── repository/
    │   │               │   ├── PedidoRepository.java
    │   │               │   ├── PedidoRepositorySQLite.java
    │   │               │   └── SheetsRepository.java
    │   │               └── service/
    │   │                   ├── PedidoService.java
    │   │                   ├── SheetsService.java
    │   │                   └── calculadora/
    │   │                       ├── CalculadoraPreco.java
    │   │                       └── regrascalculos/
    │   │                           ├── RegraConsignado.java
    │   │                           ├── RegraDescontoPDV.java
    │   │                           ├── RegraPreco.java
    │   │                           └── RegrasPorVolume.java
    │   └── resources/
    │       ├── schema.sql
    │       └── window.fxml
    └── test/
        └── java/
            └── br/
                └── com/
                    └── projetov/
                        └── service/
                            └── calculadora/
                                └── CalculadoraPrecoTest.java
```

---

## Estado final da Fase 2

A Fase 2 encerra com os principais objetivos de integridade de negocio encaminhados:

- motor financeiro isolado e testado;
- regras comerciais implementadas como strategies;
- preco consignado blindado contra descontos;
- desconto PDV e desconto por volume validados;
- persistencia local transacional em SQLite;
- `tipo_venda` persistido no cabecalho do pedido;
- `sincronizado` disponivel no schema local para apoiar a fila de sincronizacao da Fase 3;
- controller sem calculo direto de preco;
- suite JUnit validada com sucesso;
- handler global de excecoes presente para melhorar resiliencia da aplicacao JavaFX.

O principal ponto de alinhamento antes da Fase 3 passa a ser a implementacao do fluxo que consome essa coluna: gravar pedidos como pendentes, processar a fila em background, sincronizar com Google Sheets e marcar `sincronizado = 1` somente apos sucesso confirmado.
