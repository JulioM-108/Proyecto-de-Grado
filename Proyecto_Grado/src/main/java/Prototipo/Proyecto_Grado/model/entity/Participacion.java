package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "participacion" de Supabase.
 * Representa la participación de un estudiante en una sesión del tutor.
 * Columnas: id (uuid PK), created_at (timestamptz), progreso_md (text),
 *           estudiante_id (uuid FK → Estudiante), sesion_id (uuid FK → Sesion).
 *
 * progreso_md acumula el resumen cualitativo del avance del estudiante (Capa 3 del prompt).
 */
@Entity
@Table(name = "participacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_creacion", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    /** Resumen Markdown del progreso acumulado del estudiante en esta sesión (Capa 3 del prompt). */
    @Column(name = "progreso_md", columnDefinition = "TEXT")
    @Builder.Default
    private String progresoMd = "";

    /** Retroalimentación y comentarios del estudiante para el profesor sobre la sesión */
    @Column(name = "feedback_estudiante", columnDefinition = "TEXT")
    private String feedbackEstudiante;

    /** Calificación de la sesión por el estudiante (1 a 5 estrellas) */
    @Column(name = "valoracion_estudiante")
    private Integer valoracionEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.progresoMd == null) {
            this.progresoMd = "";
        }
    }

    /** Alias de compatibilidad para plantillas Thymeleaf */
    public OffsetDateTime getFechaCreacion() {
        return this.createdAt;
    }
}
