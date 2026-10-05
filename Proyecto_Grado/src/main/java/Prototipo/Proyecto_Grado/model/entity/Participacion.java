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

    /** Evaluación psicométrica y pedagógica de la plataforma (SUS + TAM en formato JSON) */
    @Column(name = "evaluacion_sus_tam", columnDefinition = "TEXT")
    private String evaluacionSusTam;

    /**
     * true cuando el tutor certificó (y el servidor validó) que el estudiante completó
     * la ruta de preguntas guía. Es independiente de progreso_md: que el docente genere
     * un diagnóstico NO cierra la tutoría del estudiante.
     */
    @Column(name = "ruta_completada", nullable = false)
    @Builder.Default
    private Boolean rutaCompletada = false;

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
        if (this.rutaCompletada == null) {
            this.rutaCompletada = false;
        }
    }

    /**
     * Indica si la tutoría quedó cerrada para el estudiante: completó la ruta de preguntas guía
     * o envió la encuesta SUS+TAM. El "Feedback Profe" (valoración) y el informe del docente
     * (progreso_md) NO cierran la tutoría.
     */
    public boolean isTutoriaFinalizada() {
        return Boolean.TRUE.equals(rutaCompletada) || evaluacionSusTam != null;
    }

    /** Alias de compatibilidad para plantillas Thymeleaf */
    public OffsetDateTime getFechaCreacion() {
        return this.createdAt;
    }

    /** Retorna el DTO parseado de la evaluación SUS + TAM o null si no se ha diligenciado */
    public Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO getEvaluacionSusTamDTO() {
        return Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO.fromJson(this.evaluacionSusTam);
    }

    /** Retorna el puntaje SUS oficial (0 a 100) o null si no se ha evaluado */
    public Double getSusScore() {
        Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO dto = getEvaluacionSusTamDTO();
        return dto != null ? dto.getSusScore() : null;
    }
}
