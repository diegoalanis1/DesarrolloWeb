package mx.edu.backendacademico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AlumnoIdempotenciaIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private MvcResult crearSolicitud(
            String clave,
            String matricula,
            String nombre,
            String correo) throws Exception {

        String contenido = """
                {
                    "matricula": "%s",
                    "nombre": "%s",
                    "correo": "%s"
                }
                """.formatted(matricula, nombre, correo);

        return mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .with(csrf())
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contenido))
                .andReturn();
    }

    private String sufijoUnico() {
        return UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
    }

    private long obtenerId(MvcResult resultado) throws UnsupportedEncodingException {
        String cuerpo = resultado.getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(cuerpo);

        if (!matcher.find()) {
            throw new AssertionError(
                    "No se encontró el ID en la respuesta: " + cuerpo);
        }

        return Long.parseLong(matcher.group(1));
    }

    @Test
    void solicitudesSimultaneasConMismaClaveCreanUnSoloAlumno()
            throws Exception {

        String sufijo = sufijoUnico();
        String clave = "prueba-" + sufijo;
        String matricula = "IDEMP" + sufijo;

        CountDownLatch iniciar = new CountDownLatch(1);

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(2)) {

            Future<MvcResult> futuro1 = executor.submit(() -> {
                iniciar.await();
                return crearSolicitud(
                        clave, matricula, "Ada Lovelace", "ada@u.mx");
            });

            Future<MvcResult> futuro2 = executor.submit(() -> {
                iniciar.await();
                return crearSolicitud(
                        clave, matricula, "Ada Lovelace", "ada@u.mx");
            });

            iniciar.countDown();

            MvcResult resultado1 = futuro1.get(15, TimeUnit.SECONDS);
            MvcResult resultado2 = futuro2.get(15, TimeUnit.SECONDS);

            assertEquals(
                    resultado1.getResponse().getContentAsString(),
                    resultado2.getResponse().getContentAsString());

            assertEquals(201, resultado1.getResponse().getStatus());
            assertEquals(201, resultado2.getResponse().getStatus());

            assertEquals(
                    obtenerId(resultado1),
                    obtenerId(resultado2));

            assertEquals(
                    resultado1.getResponse().getHeader("Location"),
                    resultado2.getResponse().getHeader("Location"));

            Integer cantidad = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM alumnos WHERE matricula = ?",
                    Integer.class,
                    matricula);

            assertEquals(1, cantidad);
        }
    }

    @Test
    void mismaClaveConContenidoDiferenteDevuelve409()
            throws Exception {

        String sufijo = sufijoUnico();
        String clave = "prueba-" + sufijo;

        String matricula1 = "IDEMP" + sufijo;
        String matricula2 = "OTRO" + sufijo;

        MvcResult primero = crearSolicitud(
                clave, matricula1, "Ada Lovelace", "ada@u.mx");

        MvcResult segundo = crearSolicitud(
                clave, matricula2, "Alan Turing", "alan@u.mx");

        assertEquals(201, primero.getResponse().getStatus());
        assertEquals(409, segundo.getResponse().getStatus());

        Integer cantidad = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM alumnos
                WHERE matricula IN (?, ?)
                """,
                Integer.class,
                matricula1,
                matricula2);

        assertEquals(1, cantidad);
    }

    @Test
    void clavesDiferentesConMismaMatriculaDevuelve409() throws Exception {
        String sufijo = sufijoUnico();
        String matricula = "IDEMP" + sufijo;

        MvcResult primero = crearSolicitud(
                "clave-1-" + sufijo,
                matricula,
                "Ada Lovelace",
                "ada@u.mx"
        );

        MvcResult segundo = crearSolicitud(
                "clave-2-" + sufijo,
                matricula,
                "Ada Lovelace",
                "ada@u.mx"
        );

        assertEquals(201, primero.getResponse().getStatus());
        assertEquals(409, segundo.getResponse().getStatus());

        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alumnos WHERE matricula = ?",
                Integer.class,
                matricula
        );

        assertEquals(1, cantidad);
    }
}