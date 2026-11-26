package mx.edu.backendacademico.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import mx.edu.backendacademico.domain.Alumno;
import org.springframework.stereotype.Component;

@Component
public class HuellaContenido {

    public String calcular(Alumno alumno) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            actualizar(digest, alumno.matricula());
            actualizar(digest, alumno.nombre());
            actualizar(digest, alumno.correo());

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private void actualizar(MessageDigest digest, String valor) {
        byte[] bytes = valor.getBytes(StandardCharsets.UTF_8);

        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(bytes.length)
                .array());

        digest.update(bytes);
    }
}