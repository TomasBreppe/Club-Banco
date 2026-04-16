ALTER TABLE socio
ADD COLUMN tiene_beca boolean NOT NULL DEFAULT false;

ALTER TABLE socio
ADD COLUMN porcentaje_beca_social numeric(5,2) NOT NULL DEFAULT 0;

ALTER TABLE socio
ADD COLUMN porcentaje_beca_deportiva numeric(5,2) NOT NULL DEFAULT 0;

ALTER TABLE socio
ADD COLUMN porcentaje_beca_preparacion_fisica numeric(5,2) NOT NULL DEFAULT 0;

ALTER TABLE socio
ADD COLUMN observacion_beca varchar(255);