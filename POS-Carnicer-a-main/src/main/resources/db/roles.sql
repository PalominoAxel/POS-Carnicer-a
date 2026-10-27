-- Rol de aplicacion con privilegios minimos, usado por application.properties (db.user/db.password).
-- La app NUNCA debe conectarse como "postgres" (superusuario): si quedara comprometida, un atacante
-- tendria control total del servidor de BD y no solo de la base dontoribio_pos.
--
-- Ejecutar como superusuario (postgres) contra la base "dontoribio_pos" despues de correr schema.sql.
-- Cambiar la contrasena de ejemplo antes de usarla en cualquier entorno.

DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'pos_app') THEN
      CREATE ROLE pos_app WITH LOGIN PASSWORD 'CAMBIAR_ESTA_CONTRASENA' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
   END IF;
END
$$;

GRANT CONNECT ON DATABASE dontoribio_pos TO pos_app;
GRANT USAGE ON SCHEMA public TO pos_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO pos_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO pos_app;

-- Para que las tablas/secuencias creadas mas adelante (por el superusuario) tambien queden accesibles.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pos_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO pos_app;

-- Evita que cualquier otro rol/usuario sin permisos explicitos pueda crear objetos en el esquema.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
