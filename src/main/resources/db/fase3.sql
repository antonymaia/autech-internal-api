-- =============================================================================
-- Fase 3 - Migracao Payments API -> Orders API (Checkout Transparente)
-- Rodar UMA VEZ no MySQL (banco: autechbd)
-- =============================================================================

-- 1) Adiciona coluna MP_ORDER_ID para rastrear a Order criada no Mercado Pago
--    (mantem MP_PAYMENT_ID por compatibilidade com registros antigos)
ALTER TABLE FATURAS
  ADD COLUMN MP_ORDER_ID VARCHAR(64) NULL;

CREATE INDEX idx_faturas_mp_order_id ON FATURAS (MP_ORDER_ID);
