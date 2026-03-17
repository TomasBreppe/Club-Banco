INSERT INTO disciplina(nombre) VALUES
                                   ('PATIN'), ('BASQUET'), ('RITMICA'), ('HANDBALL'), ('TAEKWONDO')
    ON CONFLICT DO NOTHING;

INSERT INTO parametro(clave, valor_num) VALUES
                                            ('INSCRIPCION_MONTO', 35000),
                                            ('SOCIAL_MONTO', 10000)
    ON CONFLICT (clave) DO NOTHING;
