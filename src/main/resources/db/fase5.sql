-- =============================================================================
-- Fase 5 - Registro das execucoes das tarefas agendadas
-- Rodar UMA VEZ no MySQL (banco: autechbd), ANTES de subir a versao nova da API
-- =============================================================================

-- 1) Quem pode consultar as execucoes e o log de cobranca pelo site (vai no token do login)
ALTER TABLE USUARIOS
  ADD COLUMN PODE_VER_EXECUCOES BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Liberar para os colaboradores que acompanham (trocar pelos logins certos)
-- UPDATE USUARIOS SET PODE_VER_EXECUCOES = TRUE WHERE LOGIN IN ('login1', 'login2');

-- 3) Uma linha por execucao da tarefa, com os totais
CREATE TABLE EXECUCAO_TAREFA (
  ID BIGINT NOT NULL AUTO_INCREMENT,
  TAREFA VARCHAR(50) NOT NULL,
  STATUS VARCHAR(20) NOT NULL,
  INICIO DATETIME NOT NULL,
  FIM DATETIME NULL,
  FECHADAS INT NOT NULL DEFAULT 0,
  PROXIMAS_GERADAS INT NOT NULL DEFAULT 0,
  IGNORADOS INT NOT NULL DEFAULT 0,
  ERROS INT NOT NULL DEFAULT 0,
  PRIMARY KEY (ID)
);

CREATE INDEX idx_execucao_tarefa_inicio ON EXECUCAO_TAREFA (INICIO);

-- 4) Uma linha por cliente com erro ou ignorado. CNPJ_CPF sem FK: o historico continua
--    valendo se o cliente for excluido, e fica NULL quando a falha nao e de um cliente.
CREATE TABLE OCORRENCIA_EXECUCAO (
  ID BIGINT NOT NULL AUTO_INCREMENT,
  EXECUCAO_ID BIGINT NOT NULL,
  TIPO VARCHAR(20) NOT NULL,
  CNPJ_CPF VARCHAR(20) NULL,
  MOTIVO VARCHAR(50) NOT NULL,
  MENSAGEM VARCHAR(500) NULL,
  DATA_HORA DATETIME NOT NULL,
  PRIMARY KEY (ID),
  CONSTRAINT fk_ocorrencia_execucao FOREIGN KEY (EXECUCAO_ID)
    REFERENCES EXECUCAO_TAREFA (ID) ON DELETE CASCADE
);
