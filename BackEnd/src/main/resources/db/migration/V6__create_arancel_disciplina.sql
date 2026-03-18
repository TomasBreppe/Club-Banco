CREATE TABLE IF NOT EXISTS arancel_disciplina (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  disciplina_id BIGINT NOT NULL REFERENCES disciplina(id),
    categoria VARCHAR(80) NOT NULL,
    monto_social NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_deportivo NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_preparacion_fisica NUMERIC(12,2) NOT NULL DEFAULT 0,
    vigente_desde DATE NOT NULL DEFAULT CURRENT_DATE,
    activa BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE INDEX IF NOT EXISTS idx_arancel_disciplina_disciplina
    ON arancel_disciplina(disciplina_id);

CREATE INDEX IF NOT EXISTS idx_arancel_disciplina_vigente
    ON arancel_disciplina(vigente_desde);