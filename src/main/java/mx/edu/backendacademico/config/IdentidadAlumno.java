package mx.edu.backendacademico.config;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IdentidadAlumno {

    private final Map<String, String> matriculas = Map.of(
            "alumno", "A001",
            "alumno2", "B002"
    );

    public String obtenerMatricula(String usuario) {
        return matriculas.get(usuario);
    }
}