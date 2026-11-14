package mx.edu.backendacademico;

import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.InscripcionRepository;
import mx.edu.backendacademico.service.InscripcionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InscripcionConcurrencyTest {

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private InscripcionRepository inscripciones;

    @Test
    void dosAdmisionesSimultaneasRespetanElCupo() throws Exception {

        // Sustituir por los IDs de los registros que preparaste.
        Long materiaId = 2L;
        Long alumno1Id = 1L;
        Long alumno2Id = 2L;

        assertEquals(1, obtenerCupo(materiaId));
        assertEquals(0, inscripciones.countByMateriaId(materiaId));

        var executor = Executors.newFixedThreadPool(2);
        var listos = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);

        Callable<Long> tarea1 = () -> {
            listos.countDown();
            inicio.await();
            return inscripcionService.inscribir(alumno1Id, materiaId);
        };

        Callable<Long> tarea2 = () -> {
            listos.countDown();
            inicio.await();
            return inscripcionService.inscribir(alumno2Id, materiaId);
        };

        try {
            Future<Long> futuro1 = executor.submit(tarea1);
            Future<Long> futuro2 = executor.submit(tarea2);

            // Esperar a que ambas tareas estén preparadas.
            assertTrue(listos.await(5, TimeUnit.SECONDS));

            // Liberar ambas tareas.
            inicio.countDown();

            int exitosas = 0;
            int rechazadasPorCupo = 0;

            for (Future<Long> futuro : new Future[]{futuro1, futuro2}) {
                try {
                    assertNotNull(futuro.get(10, TimeUnit.SECONDS));
                    exitosas++;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    assertInstanceOf(OperacionAcademicaException.class, causa);

                    var excepcion = (OperacionAcademicaException) causa;

                    // Ajusta getCodigo() al nombre del getter de tu excepción.
                    assertEquals("SIN_CUPO", excepcion.getCodigo());

                    rechazadasPorCupo++;
                }
            }

            assertEquals(1, exitosas);
            assertEquals(1, rechazadasPorCupo);
            assertEquals(1, inscripciones.countByMateriaId(materiaId));

        } finally {
            executor.shutdownNow();
        }
    }

    private int obtenerCupo(Long materiaId) {
        // Sustituir por una consulta real a MateriaRepository
        // o eliminar esta comprobación si no necesitas verificarlo aquí.
        return 1;
    }
}