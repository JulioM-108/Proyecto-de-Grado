package Prototipo.Proyecto_Grado.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "Profesor" de Supabase (nombre entre comillas en la BD).
 * Representa al docente que crea las sesiones y consulta informes y métricas.
 * Columnas: id (uuid PK), fecha_registro (timestamptz), nombre (varchar), "correo_institucional:" (varchar unique),
 * "Facultad" (varchar), dos_factor_activo (bool), secreto_2fa (varchar), contraseña (varchar).
 * No declara asociaciones: "Sesion" y profesor_estudiante la referencian por profesor_id.
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

    /** El nombre de la columna incluye dos puntos al final ("correo_institucional:"); por eso va entre comillas. */
    @Column(name = "\"correo_institucional:\"", nullable = false, unique = true, length = 150)
    private String correo;

    /** Facultad a la que pertenece el docente (con mayúscula exacta según Supabase). */
    @Column(name = "\"Facultad\"", length = 150)
    private String facultad;

    /** true si el docente tiene activa la autenticación de doble factor (2FA por TOTP). */
    @Column(name = "dos_factor_activo")
    private Boolean dosFactorActivo;

    /** Clave secreta Base32 para cálculo TOTP (Google/Microsoft Authenticator). */
    @JsonIgnore
    @Column(name = "secreto_2fa", length = 64)
    private String secreto2fa;

    /**
     * Contraseña del docente: hash BCrypt o, para cuentas creadas a mano en Supabase, texto plano
     * (AuthService admite ambos). La columna se llama "contraseña" (con ñ, escrita como escape Unicode).
     * Se excluye del JSON.
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
