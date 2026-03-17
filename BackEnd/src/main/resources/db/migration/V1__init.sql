-- ENUMS
DO $$ BEGIN
CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'SOCIO');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
CREATE TYPE genero AS ENUM ('MASCULINO', 'FEMENINO', 'OTRO');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
CREATE TYPE estado_suscripcion AS ENUM ('PENDING', 'AUTHORIZED', 'PAUSED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
CREATE TYPE medio_pago AS ENUM ('MERCADO_PAGO', 'EFECTIVO', 'TRANSFERENCIA');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- DISCIPLINAS
CREATE TABLE IF NOT EXISTS disciplina (
                                          id BIGSERIAL PRIMARY KEY,
                                          nombre VARCHAR(30) NOT NULL UNIQUE,
    activa BOOLEAN NOT NULL DEFAULT TRUE
    );

-- PARAMETROS
CREATE TABLE IF NOT EXISTS parametro (
                                         clave VARCHAR(50) PRIMARY KEY,
    valor_num NUMERIC(12,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- CUOTAS por disciplina
CREATE TABLE IF NOT EXISTS cuota_disciplina (
                                                id BIGSERIAL PRIMARY KEY,
                                                disciplina_id BIGINT NOT NULL REFERENCES disciplina(id),
    monto_total NUMERIC(12,2) NOT NULL,
    vigente_desde DATE NOT NULL DEFAULT CURRENT_DATE,
    activa BOOLEAN NOT NULL DEFAULT TRUE
    );

-- SOCIO
CREATE TABLE IF NOT EXISTS socio (
                                     id BIGSERIAL PRIMARY KEY,
                                     dni VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80) NOT NULL,
    genero genero NOT NULL,
    telefono VARCHAR(30),
    celular VARCHAR(30) NOT NULL,
    disciplina_id BIGINT NOT NULL REFERENCES disciplina(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    vigencia_hasta DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- HISTORIAL CAMBIO DISCIPLINA
CREATE TABLE IF NOT EXISTS cambio_disciplina (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 socio_id BIGINT NOT NULL REFERENCES socio(id),
    origen_id BIGINT NOT NULL REFERENCES disciplina(id),
    destino_id BIGINT NOT NULL REFERENCES disciplina(id),
    fecha TIMESTAMP NOT NULL DEFAULT now(),
    motivo VARCHAR(200)
    );

-- USUARIO
CREATE TABLE IF NOT EXISTS usuario (
                                       id BIGSERIAL PRIMARY KEY,
                                       email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol rol_usuario NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- USUARIO_SOCIO (responsable -> varios socios)
CREATE TABLE IF NOT EXISTS usuario_socio (
                                             usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    socio_id BIGINT NOT NULL REFERENCES socio(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, socio_id)
    );

-- PAGOS
CREATE TABLE IF NOT EXISTS pago (
                                    id BIGSERIAL PRIMARY KEY,
                                    socio_id BIGINT NOT NULL REFERENCES socio(id),
    concepto VARCHAR(40) NOT NULL,
    periodo VARCHAR(7),
    monto_total NUMERIC(12,2) NOT NULL,
    monto_social NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_disciplina NUMERIC(12,2) NOT NULL DEFAULT 0,
    medio medio_pago NOT NULL,
    fecha_pago TIMESTAMP NOT NULL DEFAULT now(),
    mp_payment_id VARCHAR(50),
    mp_status VARCHAR(30),
    raw_payload JSONB
    );

-- SUSCRIPCION MP
CREATE TABLE IF NOT EXISTS suscripcion_mp (
                                              id BIGSERIAL PRIMARY KEY,
                                              socio_id BIGINT NOT NULL UNIQUE REFERENCES socio(id) ON DELETE CASCADE,
    preapproval_id VARCHAR(80) UNIQUE,
    status estado_suscripcion NOT NULL DEFAULT 'PENDING',
    monto_mensual NUMERIC(12,2) NOT NULL,
    next_payment_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_socio_disciplina ON socio(disciplina_id);
CREATE INDEX IF NOT EXISTS idx_socio_vigencia ON socio(vigencia_hasta);
CREATE INDEX IF NOT EXISTS idx_pago_socio_fecha ON pago(socio_id, fecha_pago);
