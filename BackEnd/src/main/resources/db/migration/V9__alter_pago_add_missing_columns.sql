ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS disciplina_id BIGINT REFERENCES disciplina(id);

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS arancel_disciplina_id BIGINT REFERENCES arancel_disciplina(id);

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(80);

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS monto_preparacion_fisica NUMERIC(12,2) NOT NULL DEFAULT 0;

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS observacion VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_pago_disciplina_id
    ON pago(disciplina_id);

CREATE INDEX IF NOT EXISTS idx_pago_arancel_disciplina_id
    ON pago(arancel_disciplina_id);