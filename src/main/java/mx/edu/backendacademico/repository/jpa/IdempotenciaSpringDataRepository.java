package mx.edu.backendacademico.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotenciaSpringDataRepository
        extends JpaRepository<IdempotenciaEntity, Long> {

    Optional<IdempotenciaEntity> findByOperacionAndClave(
            String operacion, String clave);

    @Modifying
    @Query(value = """
            INSERT INTO idempotencia (operacion, clave, huella)
            VALUES (:operacion, :clave, :huella)
            ON CONFLICT (operacion, clave) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(
            @Param("operacion") String operacion,
            @Param("clave") String clave,
            @Param("huella") String huella
    );

    @Modifying
    @Query(value = """
            UPDATE idempotencia
            SET status = :status,
                respuesta = CAST(:respuesta AS jsonb),
                location = :location
            WHERE operacion = :operacion
              AND clave = :clave
            """, nativeQuery = true)
    int guardarRespuesta(
            @Param("operacion") String operacion,
            @Param("clave") String clave,
            @Param("status") int status,
            @Param("respuesta") String respuesta,
            @Param("location") String location
    );
}