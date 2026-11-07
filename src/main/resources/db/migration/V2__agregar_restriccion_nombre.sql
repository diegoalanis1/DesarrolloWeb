ALTER TABLE alumnos
    ADD CONSTRAINT chk_alumnos_nombre_no_vacio
        CHECK (nombre IS NOT NULL AND LENGTH(TRIM(nombre)) > 0);