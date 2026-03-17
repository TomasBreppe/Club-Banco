ALTER TABLE socio
ALTER COLUMN genero TYPE varchar(20)
USING genero::text;