DO $$ BEGIN
CREATE TYPE gasto_categoria AS ENUM (
        'IMPUESTOS',
        'DISCIPLINAS',
        'MANTENIMIENTO',
        'LIMPIEZA',
        'EVENTOS',
        'HONORARIOS',
        'OTROS'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
CREATE TYPE medio_pago_gasto AS ENUM (
        'EFECTIVO',
        'BANCO'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS gasto (
                                     id BIGSERIAL PRIMARY KEY,
                                     fecha DATE NOT NULL,
                                     categoria gasto_categoria NOT NULL,
                                     concepto VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300),
    monto NUMERIC(12,2) NOT NULL,
    medio_pago medio_pago_gasto,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_gasto_fecha ON gasto(fecha);
CREATE INDEX IF NOT EXISTS idx_gasto_categoria ON gasto(categoria);