# 🍺 Projeto Vendas de Chopp

Aplicação desktop desenvolvida em **JavaFX** para registro e gerenciamento de pedidos de venda de chopp, com persistência local em SQLite e sincronização com Google Sheets.

---

## 🎯 Objetivo

O sistema foi criado para apoiar uma operação comercial real de vendas de chopp, cobrindo os seguintes fluxos:

- Cadastro de pedidos com cliente, entregador e modalidade de venda (PDV ou Venda Direta)
- Adição de barris ao pedido com tipo de chopp, capacidade e flag de consignado
- Cálculo automático do preço por litro com base em regras comerciais
- Persistência local transacional via SQLite
- Sincronização em background com Google Sheets quando as credenciais estiverem configuradas
- Histórico de pedidos e resumo diário de vendas e litros

---

## 🤖 Contexto de Criação

Este projeto foi desenvolvido com o auxílio de **Inteligência Artificial** com um propósito claro de aprendizado: ter contato com um projeto em escala maior do que o habitual, aplicando na prática conceitos de **design patterns**, separação de responsabilidades e arquitetura em camadas.

> Não peguei 100% do projeto — ainda falta me aprofundar nos testes e no frontend. Mas consegui identificar os principais padrões aplicados (Factory, OCP, MVC) e, de modo geral, saí com um bom aprendizado. A maior vantagem pra mim foi outra: consigo olhar o código, entender o que está acontecendo e carregar isso pra outros projetos. Não preciso memorizar — preciso saber onde buscar e como aplicar. Usar IA pra construir algo maior do que faria sozinho acelerou exatamente isso.
---

## 🏗️ Arquitetura

O projeto segue uma separação em camadas clássica:

```
Controller → Service → Repository → Banco / Integrações
                 ↓
         Motor Financeiro (regras de negócio)
```

### Camadas

- **Model** — entidades do domínio (`PedidoModel`, `BarrilPedido`) e enums de negócio (`TipoChopp`, `TipoVenda`, `CapacidadeBarril`)
- **Repository** — abstração de persistência (`PedidoRepository`) com implementação SQLite (`PedidoRepositorySQLite`) e integração com Google Sheets (`SheetsRepository`)
- **Service** — orquestração de negócio (`PedidoService`) e worker de sincronização em background (`SyncWorker`)
- **Controller** — ponte entre a UI JavaFX e a camada de serviço (`PedidoController`)

---

## 🧩 Design Patterns Aplicados

| Padrão | Onde é usado |
|---|---|
| **Repository Pattern** | `PedidoRepository` define o contrato; `PedidoRepositorySQLite` implementa |
| **Strategy Pattern** | Cada regra de preço implementa a interface `RegraPreco` |
| **Chain / Pipeline** | `CalculadoraPreco` aplica uma lista ordenada de strategies |
| **Constructor Injection** | `PedidoService` recebe dependências por construtor |
| **Defensive Copy** | `CalculadoraPreco` usa `List.copyOf()` para imutabilidade interna |
| **Global Exception Handler** | `GlobalExceptionHandler` captura exceções não tratadas e exibe alerta na UI |

---

## 💰 Motor Financeiro

O coração do sistema é a `CalculadoraPreco`, que aplica uma cadeia de regras em sequência sobre o preço base do `TipoChopp`:

```
RegraConsignado → RegraDescontoPDV → RegrasPorVolume
```

### Regras

- **RegraConsignado** — barril consignado sempre cobra o preço base, sem descontos
- **RegraDescontoPDV** — aplica 20% de desconto para vendas no canal PDV
- **RegrasPorVolume** — aplica desconto fixo de R$ 1,00 quando o volume mensal do cliente atingir 1.000 litros ou mais

Uma trava de segurança impede que combinações de descontos resultem em preço negativo:

```java
return Math.max(0.0, precoFinal);
```

---

## 🗄️ Banco de Dados

Persistência local via **SQLite**, com duas tabelas principais:

- `pedido_model` — cabeçalho da venda (cliente, entregador, modalidade, status de sincronização)
- `barris_pedido` — itens do pedido vinculados ao cabeçalho via chave estrangeira com `ON DELETE CASCADE`

A coluna `sincronizado` controla a fila de sincronização com o Google Sheets: `0` = pendente, `1` = enviado.

---

## ☁️ Sincronização com Google Sheets

O `SyncWorker` roda em uma thread daemon separada da UI e tenta sincronizar pedidos pendentes a cada 30 segundos. A lógica é **offline-first**: se a sincronização falhar (sem internet, credenciais ausentes), o pedido permanece na fila e será reprocessado no próximo ciclo.

Para habilitar, coloque os arquivos de configuração em:

```
~/.projetov/credentials.json
~/.projetov/application.properties   # google.spreadsheet.id=<ID_DA_PLANILHA>
```

---

## 🧪 Testes

Suite de testes unitários cobrindo o motor financeiro sem nenhuma dependência externa (sem SQLite, sem rede):

```
.\gradlew.bat test
```

```
BUILD SUCCESSFUL — 25 testes executados, 0 falhas, 0 erros
```

### Grupos de teste

- `RegraDescontoPDVTest` — desconto de 20% para PDV e blindagem do consignado
- `RegrasPorVolumeTest` — desconto fixo por volume e casos de borda
- `RegraConsignadoTest` — preço base fixo independente de modalidade e volume
- `CalculadoraPrecoIntegracaoTest` — cadeia completa espelhando a configuração de produção
- `EdgeCasesTest` — preço zero, volume negativo e proteção contra retorno negativo
- `CalculadoraPrecoConstrutorTest` — validação de lista nula ou vazia

---

## 🛠️ Stack Tecnológica

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal (Gradle Toolchain) |
| JavaFX 21 | Interface gráfica (FXML + CSS) |
| SQLite (`sqlite-jdbc`) | Persistência local |
| Google Sheets API v4 | Sincronização com planilha |
| JUnit 5 | Testes automatizados |
| Gradle | Build, dependências e execução |
| SLF4J Simple | Logging |

---

## ▶️ Como Executar

**Pré-requisito:** Java 21 instalado.

```bash
# Windows
.\gradlew.bat run

# Linux / macOS
./gradlew run
```

---

## 📁 Estrutura Principal

```
src/
├── main/
│   ├── java/br/com/projetov/
│   │   ├── Main.java
│   │   ├── app/          MainApp.java
│   │   ├── config/       ConnectionFactory, DatabaseInitializer, GoogleSheetsConfig...
│   │   ├── controller/   PedidoController.java
│   │   ├── models/       enums/, pedido/, pessoal/, relatorio/
│   │   ├── repository/   PedidoRepository, PedidoRepositorySQLite, SheetsRepository
│   │   ├── service/      PedidoService, SheetsService, calculadora/
│   │   └── sync/         SyncWorker.java
│   └── resources/
│       ├── schema.sql
│       ├── style.css
│       └── window.fxml
└── test/
    └── ...calculadora/CalculadoraPrecoTest.java
```
