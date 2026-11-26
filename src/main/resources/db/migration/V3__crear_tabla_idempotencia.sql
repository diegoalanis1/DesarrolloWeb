CREATE TABLE idempotencia (
      id BIGSERIAL PRIMARY KEY,
      operacion VARCHAR(100) NOT NULL,
      clave VARCHAR(100) NOT NULL,
      huella VARCHAR(64) NOT NULL,
      status INTEGER,
      respuesta JSONB,
      location VARCHAR(255),
      creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT uk_idempotencia_operacion_clave
          UNIQUE (operacion, clave)
);