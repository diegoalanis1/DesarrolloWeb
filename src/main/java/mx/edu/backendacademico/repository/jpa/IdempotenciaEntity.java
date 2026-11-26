package mx.edu.backendacademico.repository.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "idempotencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotencia_operacion_clave",
                columnNames = {"operacion", "clave"}
        )
)
public class IdempotenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String operacion;

    @Column(nullable = false, length = 100)
    private String clave;

    @Column(nullable = false, length = 64)
    private String huella;

    private Integer status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String respuesta;

    @Column(length = 255)
    private String location;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    protected IdempotenciaEntity() {
    }

    public IdempotenciaEntity(
            String operacion,
            String clave,
            String huella) {
        this.operacion = operacion;
        this.clave = clave;
        this.huella = huella;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getClave() {
        return clave;
    }

    public String getHuella() {
        return huella;
    }

    public Integer getStatus() {
        return status;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public String getLocation() {
        return location;
    }

    public void guardarRespuesta(
            Integer status,
            String respuesta,
            String location) {
        this.status = status;
        this.respuesta = respuesta;
        this.location = location;
    }
}