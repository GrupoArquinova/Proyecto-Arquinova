-- El tipo de multimedia se valida en la aplicación (enum TipoMultimedia), no en la base de datos.
-- Algunas bases locales se crearon con Hibernate, que define la columna como ENUM(...) y rechaza
-- valores nuevos como BENEFICIOS. Se deja como texto (igual que en V1); es inocuo si ya lo es.
ALTER TABLE multimedia MODIFY COLUMN tipo VARCHAR(20) NOT NULL;
