package mx.edu.backendacademico.service;

import mx.edu.backendacademico.dto.AlumnoResponse;

public record ResultadoCreacionAlumno(
        AlumnoResponse alumno,
        int status,
        String location
) {
}