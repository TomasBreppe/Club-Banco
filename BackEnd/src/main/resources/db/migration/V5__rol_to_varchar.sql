ALTER TABLE usuario
ALTER COLUMN rol TYPE varchar(20)
USING rol::text;