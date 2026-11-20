package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.repository.jpa.AlumnoEntity;
import mx.edu.backendacademico.repository.jpa.AlumnoSpringDataRepository;

@SpringBootTest
@Transactional
class AlumnoUnicidadTest {

    @Autowired
    private AlumnoSpringDataRepository alumnos;

    @Test
    void postgresRechazaMatriculaDuplicada() {
        String matricula = "UN" + UUID.randomUUID().toString().substring(0, 10);

        var primero = new AlumnoEntity(new Alumno(null, matricula, "Ada", "ada-" + matricula + "@u.mx", EstatusAlumno.ACTIVO));

        var segundo = new AlumnoEntity(new Alumno(null, matricula, "Alan", "alan-" + matricula + "@u.mx", EstatusAlumno.ACTIVO));

        // El primer registro debe guardarse.
        alumnos.saveAndFlush(primero);

        // PostgreSQL debe rechazar el segundo por matrícula duplicada.
        assertThrows(DataIntegrityViolationException.class,
                () -> alumnos.saveAndFlush(segundo));
    }
}