package mx.edu.backendacademico.service;

// Coordina los casos de uso y conserva las reglas fuera del transporte HTTP.

import java.util.List;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.exception.ClaveIdempotenciaReutilizadaException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.repository.AlumnoRepository;
import mx.edu.backendacademico.repository.jpa.IdempotenciaSpringDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

@Service
@Transactional(readOnly = true)
public class AlumnoService {
    private final AlumnoRepository repository;

    private static final String OPERACION_CREAR_ALUMNO = "CREAR_ALUMNO";

    private final IdempotenciaSpringDataRepository idempotenciaRepository;
    private final HuellaContenido huellaContenido;
    private final ObjectMapper objectMapper;

    public AlumnoService(
            AlumnoRepository repository,
            IdempotenciaSpringDataRepository idempotenciaRepository,
            HuellaContenido huellaContenido,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.idempotenciaRepository = idempotenciaRepository;
        this.huellaContenido = huellaContenido;
        this.objectMapper = objectMapper;
    }

    public List<Alumno> listar() {
        // Devuelve una instantánea de los alumnos desde el adaptador activo.
        return repository.buscarTodos();
    }

    public Alumno buscar(Long id) {
        // La ausencia se interpreta como un error de aplicación explícito.
        return repository.buscarPorId(id).orElseThrow(() -> new AlumnoNoEncontradoException(id));
    }

    @Transactional
    public ResultadoCreacionAlumno crearConIdempotencia(
            String clave,
            String matricula,
            String nombre,
            String correo) {

        if (clave == null || clave.isBlank() || clave.length() > 100) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia debe tener entre 1 y 100 caracteres");
        }

        // Normaliza los datos antes de calcular la huella.
        Alumno candidato = new Alumno(
                null, matricula, nombre, correo, EstatusAlumno.ACTIVO);

        String huella = huellaContenido.calcular(candidato);

        int insertados = idempotenciaRepository.insertarSiNoExiste(
                OPERACION_CREAR_ALUMNO, clave, huella);

        if (insertados == 0) {
            var existente = idempotenciaRepository
                    .findByOperacionAndClave(OPERACION_CREAR_ALUMNO, clave)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el registro de idempotencia"));

            if (!existente.getHuella().equals(huella)) {
                throw new ClaveIdempotenciaReutilizadaException(clave);
            }

            if (existente.getStatus() == null
                    || existente.getRespuesta() == null
                    || existente.getLocation() == null) {
                throw new IllegalStateException(
                        "El registro de idempotencia no tiene una respuesta completa");
            }

            try {
                AlumnoResponse respuesta = objectMapper.readValue(
                        existente.getRespuesta(), AlumnoResponse.class);

                return new ResultadoCreacionAlumno(
                        respuesta,
                        existente.getStatus(),
                        existente.getLocation());

            } catch (JacksonException  ex) {
                throw new IllegalStateException(
                        "No fue posible recuperar la respuesta original", ex);
            }
        }

        // La clave es nueva: crear el alumno.
        if (repository.existePorMatricula(candidato.matricula())) {
            throw new MatriculaDuplicadaException(candidato.matricula());
        }

        Alumno creado = repository.guardar(candidato);
        AlumnoResponse respuesta = AlumnoMapper.toResponse(creado);

        String location = "/api/v1/alumnos/" + creado.id();

        try {
            String respuestaJson = objectMapper.writeValueAsString(respuesta);

            int actualizados = idempotenciaRepository.guardarRespuesta(
                    OPERACION_CREAR_ALUMNO,
                    clave,
                    201,
                    respuestaJson,
                    location);

            if (actualizados != 1) {
                throw new IllegalStateException(
                        "No fue posible guardar la respuesta de idempotencia");
            }

        } catch (JacksonException  ex) {
            throw new IllegalStateException(
                    "No fue posible serializar la respuesta del alumno", ex);
        }

        return new ResultadoCreacionAlumno(respuesta, 201, location);
    }

    @Transactional
    public Alumno crear(String matricula, String nombre, String correo) {
        // Esta comprobación mejora el mensaje; el almacén vuelve a proteger unicidad.
        if (repository.existePorMatricula(matricula)) {
            throw new MatriculaDuplicadaException(matricula);
        }
        return repository.guardar(new Alumno(null, matricula, nombre, correo, EstatusAlumno.ACTIVO));
    }

    @Transactional
    public Alumno actualizar(Long id, String nombre, String correo) {
        // Recupera el estado actual y conserva identidad, matrícula y estatus.
        var actual = buscar(id);
        return repository.guardar(new Alumno(id, actual.matricula(), nombre, correo, actual.estatus()));
    }

    @Transactional
    public void eliminar(Long id) {
        // Si no existe, comunica ausencia antes de solicitar la eliminación.
        buscar(id);
        repository.eliminarPorId(id);
    }
}