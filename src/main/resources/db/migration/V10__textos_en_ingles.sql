-- Textos en inglés para el sitio público. Son opcionales: si un campo en inglés está vacío, el sitio muestra el español.
ALTER TABLE proyectos ADD COLUMN descripcion_en TEXT NULL, ADD COLUMN participacion_en VARCHAR(255) NULL;
ALTER TABLE casas_modelo ADD COLUMN descripcion_en TEXT NULL;
ALTER TABLE zonas_comunes ADD COLUMN nombre_en VARCHAR(150) NULL, ADD COLUMN descripcion_en TEXT NULL;
ALTER TABLE etapas ADD COLUMN nombre_en VARCHAR(100) NULL, ADD COLUMN descripcion_en TEXT NULL;
ALTER TABLE ubicaciones ADD COLUMN referencias_en TEXT NULL;
ALTER TABLE lotes ADD COLUMN descripcion_en TEXT NULL;
ALTER TABLE contenidos_institucionales ADD COLUMN titulo_en VARCHAR(200) NULL, ADD COLUMN contenido_en TEXT NULL;
-- Idioma en el que la persona usaba el sitio al escribir: sirve para responderle (y confirmarle) en ese idioma.
ALTER TABLE solicitudes_contacto ADD COLUMN idioma VARCHAR(5) NOT NULL DEFAULT 'es';
