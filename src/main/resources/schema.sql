-- 1. Tabela principal que representa o PedidoModel (O Cabeçalho da Venda)
CREATE TABLE IF NOT EXISTS pedido_model (
                                            id          INTEGER  PRIMARY KEY AUTOINCREMENT,
                                            cliente     TEXT     NOT NULL,
                                            entregador  TEXT     NOT NULL,
                                            tipo_venda  TEXT     NOT NULL,  -- NOVO: persiste TipoVenda.name() ex: "PDV", "AVULSO"
                                            data_hora   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela filha que representa os itens BarrilPedido (Os Produtos da Venda)
CREATE TABLE IF NOT EXISTS barris_pedido (
                                             id            INTEGER PRIMARY KEY AUTOINCREMENT,
                                             pedido_id     INTEGER NOT NULL,
                                             codigo_barril TEXT    NOT NULL,
                                             tipo_chopp    TEXT    NOT NULL,
                                             capacidade    INTEGER NOT NULL,  -- litros (int), mantido conforme getLitros()
                                             preco_venda   REAL    NOT NULL,
                                             consignado    INTEGER DEFAULT 0, -- 0 = Não, 1 = Sim

                                             FOREIGN KEY (pedido_id) REFERENCES pedido_model(id) ON DELETE CASCADE
);