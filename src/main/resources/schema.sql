-- 1. Tabela principal que representa o PedidoModel (O Cabeçalho da Venda)
CREATE TABLE IF NOT EXISTS pedido_model (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      cliente TEXT NOT NULL,
                                      entregador TEXT NOT NULL,
                                      data_hora DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela filha que representa os itens BarrilPedido (Os Produtos da Venda)
CREATE TABLE IF NOT EXISTS barris_pedido (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             pedido_id INTEGER NOT NULL,
                                             codigo_barril TEXT NOT NULL,
                                             tipo_chopp TEXT NOT NULL,
                                             capacidade INTEGER NOT NULL,
                                             preco_venda REAL NOT NULL,
                                             consignado INTEGER DEFAULT 0, -- 0 = Não, 1 = Sim

    -- Chave estrangeira ligando o barril ao pedido correspondente
                                             FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE
);