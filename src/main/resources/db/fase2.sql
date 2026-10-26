-- =============================================================================
-- Fase 2 - Portal do cliente com Mercado Pago
-- Rodar UMA VEZ no MySQL (banco: autechbd)
-- =============================================================================

-- 1) Adiciona coluna para rastrear o payment_id do Mercado Pago
--    Necessario para idempotencia do webhook e evitar criar PIX duplicado
ALTER TABLE FATURAS
  ADD COLUMN MP_PAYMENT_ID VARCHAR(64) NULL;

CREATE INDEX idx_faturas_mp_payment_id ON FATURAS (MP_PAYMENT_ID);
