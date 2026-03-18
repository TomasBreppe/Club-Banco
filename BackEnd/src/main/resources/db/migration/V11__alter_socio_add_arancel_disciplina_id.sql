ALTER TABLE socio
    ADD COLUMN IF NOT EXISTS arancel_disciplina_id BIGINT REFERENCES arancel_disciplina(id);

CREATE INDEX IF NOT EXISTS idx_socio_arancel_disciplina_id
    ON socio(arancel_disciplina_id);