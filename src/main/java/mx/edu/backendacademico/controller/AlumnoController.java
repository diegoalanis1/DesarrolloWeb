package mx.edu.backendacademico.controller;

// Traduce HTTP a llamadas de aplicación y construye respuestas mediante DTO.

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

import mx.edu.backendacademico.config.IdentidadAlumno;
import mx.edu.backendacademico.dto.ActualizarAlumnoRequest;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.service.AlumnoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/alumnos")
public class AlumnoController {
    private final AlumnoService service;
    private final IdentidadAlumno identidadAlumno;
    public AlumnoController(AlumnoService service, IdentidadAlumno identidadAlumno) {this.service = service;this.identidadAlumno = identidadAlumno;}
    // GET consulta sin solicitar cambios de estado de negocio.
    @GetMapping
    public List<AlumnoResponse> listar() { return service.listar().stream().map(AlumnoMapper::toResponse).toList(); }

    // GET consulta sin solicitar cambios de estado de negocio.
    @GetMapping("/{id}")
    public AlumnoResponse buscar(@PathVariable Long id) { return AlumnoMapper.toResponse(service.buscar(id)); }

    // Consulta el expediente asociado al usuario autenticado.
    @GetMapping("/mi-expediente")
    public AlumnoResponse miExpediente(Authentication authentication) {

        String matricula = identidadAlumno.obtenerMatricula(
                authentication.getName()
        );

        if (matricula == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var alumno = service.listar().stream()
                .filter(a -> a.matricula().equals(matricula))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el alumno asociado al usuario"
                ));

        return AlumnoMapper.toResponse(alumno);
    }

    // Consulta un expediente específico con autorización por objeto.
    @GetMapping("/{id}/expediente")
    public AlumnoResponse consultarExpediente(
            @PathVariable Long id,
            Authentication authentication) {

        var alumno = service.buscar(id);

        boolean esAdministrador = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdministrador) {
            String matricula = identidadAlumno.obtenerMatricula(
                    authentication.getName()
            );

            if (matricula == null || !matricula.equals(alumno.matricula())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        return AlumnoMapper.toResponse(alumno);
    }

    // El servicio asigna identidad; ResponseEntity publica 201 y Location.
    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@RequestBody @Valid CrearAlumnoRequest request) {
        var creado = service.crear(request.matricula(), request.nombre(), request.correo());
        return ResponseEntity.created(URI.create("/api/v1/alumnos/" + creado.id())).body(AlumnoMapper.toResponse(creado));
    }

    // PUT sustituye todos los campos de la representación editable.
    @PutMapping("/{id}")
    public AlumnoResponse actualizar(@PathVariable Long id, @RequestBody @Valid ActualizarAlumnoRequest request) {
        return AlumnoMapper.toResponse(service.actualizar(id, request.nombre(), request.correo()));
    }

    // DELETE exitoso termina con 204, que no permite cuerpo de respuesta.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}