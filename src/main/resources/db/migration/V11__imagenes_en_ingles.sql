-- Versión en inglés de las imágenes que llevan texto dentro (diapositivas de bienvenida, mapas, planos, plano de urbanismo).
-- Son opcionales: si no hay versión en inglés, el sitio en inglés muestra la imagen original.
ALTER TABLE multimedia ADD COLUMN url_en VARCHAR(1500) NULL;
ALTER TABLE ubicaciones ADD COLUMN urbanismo_url_en VARCHAR(1000) NULL;
