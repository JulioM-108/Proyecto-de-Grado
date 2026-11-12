package Prototipo.Proyecto_Grado.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "Estudiante" de Supabase (nombre entre comillas en la BD).
 * Columnas: id (uuid PK), fecha_registro (timestamptz), nombre (varchar), correo_institucional (varchar unique),
 * contraseña (varchar).
 * No declara asociaciones: participacion y profesor_estudiante la referencian por estudiante_id.
 */
@Entity
@Table(name = "\"Estudiante\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_registro", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 150)
    private String correo;

    /**
     * Hash BCrypt de la contraseña (se genera al registrarse; AuthService no acepta texto plano para estudiantes).
     * La columna se llama "contraseña" (con ñ, escrita como escape Unicode). Se excluye del JSON.
     */
    @JsonIgnore
    @Column(name = "contrase\u00f1a", nullable = false)
    private String contrasena;

    /** Asigna la fecha de registro al insertar si no se proporcionó. */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
