CREATE TABLE IF NOT EXISTS socio_disciplina (
    id BIGSERIAL PRIMARY KEY,
    socio_id BIGINT NOT NULL REFERENCES socio(id),
    disciplina_id BIGINT NOT NULL REFERENCES disciplina(id),
    arancel_disciplina_id BIGINT REFERENCES arancel_disciplina(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    vigencia_hasta DATE,
    inscripcion_pagada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_alta TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_socio_disciplina_socio
    ON socio_disciplina(socio_id);

CREATE INDEX IF NOT EXISTS idx_socio_disciplina_disciplina
    ON socio_disciplina(disciplina_id);

CREATE INDEX IF NOT EXISTS idx_socio_disciplina_arancel
    ON socio_disciplina(arancel_disciplina_id);

CREATE INDEX IF NOT EXISTS idx_socio_disciplina_activo
    ON socio_disciplina(activo);