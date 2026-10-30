-- =============================================================================
-- Fase 4 - Permissao para publicar arquivos no servidor de downloads
-- Rodar UMA VEZ no MySQL (banco: autechbd)
-- =============================================================================

-- 1) Quem pode enviar/substituir arquivos em downloads.autech.net.br (vai no token do login)
ALTER TABLE USUARIOS
  ADD COLUMN PODE_PUBLICAR BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Liberar para os colaboradores que publicam (trocar pelos logins certos)
-- UPDATE USUARIOS SET PODE_PUBLICAR = TRUE WHERE LOGIN IN ('login1', 'login2');
