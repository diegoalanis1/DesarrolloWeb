package mx.edu.backendacademico.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

import mx.edu.backendacademico.service.AlumnoService;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlumnoTest {
    @Test void bajaConservaIdentidadSinModificarElOriginal() {
        var alumno = new Alumno(1L, " a001 ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        var baja = alumno.darDeBaja();
        // Comprueba tanto el resultado como la ausencia de efectos laterales.
        assertEquals("A001", baja.matricula());
        assertEquals(alumno.id(), baja.id());
        assertEquals(EstatusAlumno.BAJA, baja.estatus());
        assertEquals(EstatusAlumno.ACTIVO, alumno.estatus());
    }

    @Test void rechazaMatriculaVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                new Alumno(null, " ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO));
    }

    @Test void materiaRechazaCreditosNoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new Materia(null, "M1", "Análisis", 0));
    }

    @Autowired
    private AlumnoService service;

    @Test
    void dosTareasIntentanGuardarLaMismaMatricula() throws Exception {
        var matricula = ("T" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10))
                .toUpperCase(Locale.ROOT);
        var listos = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {

            var tarea1 = executor.submit(() -> {
                listos.countDown();
                try {
                    inicio.await();
                    service.crear(matricula, "Ana", "ana@u.mx");
                    return "Guardado";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (MatriculaDuplicadaException e) {
                    return "Conflicto";
                }
            });

            var tarea2 = executor.submit(() -> {
                listos.countDown();
                try {
                    inicio.await();
                    service.crear(matricula, "Luis", "luis@u.mx");
                    return "Guardado";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (MatriculaDuplicadaException e) {
                    return "Conflicto";
                }
            });

            listos.await();
            inicio.countDown();

            var resultado1 = tarea1.get();
            var resultado2 = tarea2.get();

            System.out.println(resultado1 + ", " + resultado2);

            assertEquals(1, java.util.stream.Stream.of(resultado1, resultado2)
                    .filter(r -> r.equals("Guardado")).count());
            assertEquals(1, java.util.stream.Stream.of(resultado1, resultado2)
                    .filter(r -> r.equals("Conflicto")).count());

            assertEquals(1L, service.listar().stream()
                    .filter(a -> a.matricula().equals(matricula))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }
}