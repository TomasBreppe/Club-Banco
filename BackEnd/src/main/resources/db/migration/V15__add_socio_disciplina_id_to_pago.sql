ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS socio_disciplina_id BIGINT REFERENCES socio_disciplina(id);

CREATE INDEX IF NOT EXISTS idx_pago_socio_disciplina_id
    ON pago(socio_disciplina_id);

UPDATE pago p
SET socio_disciplina_id = sd.id
FROM socio_disciplina sd
WHERE p.socio_id = sd.socio_id
  AND p.disciplina_id = sd.disciplina_id
  AND p.socio_disciplina_id IS NULL;