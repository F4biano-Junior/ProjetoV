CREATE TABLE IF NOT EXISTS pedidos (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       cliente_identificador TEXT NOT NULL, -- CPF/CNPJ do record
                                       entregador_id INTEGER NOT NULL,
                                       cod_barril TEXT NOT NULL,
                                       tipo_chopp TEXT NOT NULL, -- Ex: 'PILSEN'
                                       tipo_venda TEXT NOT NULL, -- Ex: 'PDV', 'VENDA_DIRETA'
                                       volume_litros REAL NOT NULL,
                                       preco_final REAL NOT NULL,
                                       is_consignado INTEGER DEFAULT 0, -- 0 = Não, 1 = Sim
                                       data_venda DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       sincronizado INTEGER DEFAULT 0 -- 0 = Pendente, 1 = Enviado pro Google Sheets
);