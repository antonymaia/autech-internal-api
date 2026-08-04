-- =============================================================================
-- Fase 1 - Sistema de faturas
-- Rodar UMA VEZ no MySQL (banco: autechbd)
-- =============================================================================

-- 1) Corrigir Produto.id_produto de qualquer tipo texto para BIGINT AUTO_INCREMENT
--    (só existia um registro de teste, então drop/recreate é seguro)
DROP TABLE IF EXISTS PRODUTO;

CREATE TABLE PRODUTO (
    ID_PRODUTO BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome       VARCHAR(255) NOT NULL,
    descricao  VARCHAR(500) NULL,
    valor      DECIMAL(18,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2) Produtos contratados por cada assinatura (N:N com quantidade)
CREATE TABLE ASSINATURA_PRODUTO (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    assinatura_id  BIGINT NOT NULL,
    produto_id     BIGINT NOT NULL,
    quantidade     INT NOT NULL DEFAULT 1,
    valor          DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_assprod_assinatura FOREIGN KEY (assinatura_id) REFERENCES ASSINATURA(ID_ASSINATURA)
        ON DELETE CASCADE,
    CONSTRAINT fk_assprod_produto FOREIGN KEY (produto_id) REFERENCES PRODUTO(ID_PRODUTO)
        ON DELETE RESTRICT,
    UNIQUE KEY uk_assinatura_produto (assinatura_id, produto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 3) Itens da fatura (snapshot dos produtos no momento da emissão)
CREATE TABLE FATURA_ITEM (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    fatura_id       BIGINT NOT NULL,
    produto_id      BIGINT NULL,
    descricao       VARCHAR(255) NOT NULL,
    valor_unitario  DECIMAL(18,2) NOT NULL,
    quantidade      INT NOT NULL,
    subtotal        DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_faturaitem_fatura FOREIGN KEY (fatura_id) REFERENCES FATURAS(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_faturaitem_produto FOREIGN KEY (produto_id) REFERENCES PRODUTO(ID_PRODUTO)
        ON DELETE SET NULL,
    INDEX idx_faturaitem_fatura (fatura_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
