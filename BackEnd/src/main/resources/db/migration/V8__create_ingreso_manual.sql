DO $$ BEGIN
CREATE TYPE categoria_ingreso_manual AS ENUM (
        'MANTOVANI',
        'DIEF',
        'MUTUAL',
        'EVENTOS',
        'FUTBOL_TORNEOS',
        'FUTBOL_ALQUILERES'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
CREATE TYPE medio_ingreso_manual AS ENUM (
        'EFECTIVO',
        'BANCO'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS ingreso_manual (
                                              id BIGSERIAL PRIMARY KEY,
                                              fecha DATE NOT NULL,
                                              categoria categoria_ingreso_manual NOT NULL,
                                              medio_pago medio_ingreso_manual NOT NULL,
                                              monto NUMERIC(15,2) NOT NULL,
    descripcion VARCHAR(300),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_ingreso_manual_fecha
    ON ingreso_manual(fecha);

CREATE INDEX IF NOT EXISTS idx_ingreso_manual_categoria
    ON ingreso_manual(categoria);