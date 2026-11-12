package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla profesor_estudiante.
 * Tabla intermedia de la relación muchos a muchos entre {@link Profesor} y {@link Estudiante}
 * (dos ManyToOne, con unicidad del par profesor_id y estudiante_id).
 * Permite que un estudiante permanezca en el directorio de alumnos de un docente aunque sea
 * desvinculado de una sesión específica o no tenga clases activas actualmente.
 */
@Entity
@Table(name = "profesor_estudiante", uniqueConstraints = {
        @UniqueConstraint(name = "uq_profesor_estudiante", columnNames = {"profesor_id", "estudiante_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(name = "fecha_vinculacion", columnDefinition = "timestamptz")
    private OffsetDateTime fechaVinculacion;

    /** Asigna la fecha de vinculación al insertar si no se proporcionó. */
    @PrePersist
    public void prePersist() {
        if (this.fechaVinculacion == null) {
            this.fechaVinculacion = OffsetDateTime.now();
        }
    }
}
