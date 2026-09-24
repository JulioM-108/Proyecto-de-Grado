package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "Profesor" de Supabase.
 * Columnas: id (uuid PK), created_at (timestamptz), nombre (varchar), correo (varchar unique), contraseña (varchar).
 */
@Entity
@Table(name = "\"Profesor\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_registro", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "\"correo_institucional:\"", nullable = false, unique = true, length = 150)
    private String correo;

    /** Facultad a la que pertenece el docente (con mayúscula exacta según Supabase). */
    @Column(name = "\"Facultad\"", length = 150)
    private String facultad;

    /** Indicador de si el docente tiene activo el Doble Factor de Autenticación (2FA). */
    @Column(name = "dos_factor_activo")
    private Boolean dosFactorActivo;

    /** Clave secreta Base32 para cálculo TOTP (Google/Microsoft Authenticator). */
    @Column(name = "secreto_2fa", length = 64)
    private String secreto2fa;

    /** Contraseña en texto plano o hash (columna con nombre especial). */
    @Column(name = "contrase\u00f1a", nullable = false)
    private String contrasena;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
