INSERT INTO socio_disciplina (
    socio_id,
    disciplina_id,
    arancel_disciplina_id,
    activo,
    vigencia_hasta,
    inscripcion_pagada,
    fecha_alta
)
SELECT
    s.id,
    s.disciplina_id,
    s.arancel_disciplina_id,
    s.activo,
    s.vigencia_hasta,
    s.inscripcion_pagada,
    COALESCE(s.created_at, now())
FROM socio s
WHERE s.disciplina_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM socio_disciplina sd
      WHERE sd.socio_id = s.id
        AND sd.disciplina_id = s.disciplina_id
  );