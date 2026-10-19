package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "Sesion" de Supabase.
 * Columnas: id (uuid PK), created_at (timestamptz), tema (varchar), craft_contexto (text),
 *           craft_rol (text), craft_audiencia (text), craft_formato (text), craft_tono (text),
 *           enlace_unico (varchar), estado (bool), preguntas_guia (text), sintesis_md (text),
 *           sintesis_fecha (timestamptz), profesor_id (uuid FK → Profesor).
 */
@Entity
@Table(name = "\"Sesion\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_creacion", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(length = 150)
    private String tema;

    @Column(name = "craft_contexto", columnDefinition = "TEXT")
    private String craftContexto;

    @Column(name = "craft_rol", columnDefinition = "TEXT")
    private String craftRol;

    @Column(name = "craft_audiencia", columnDefinition = "TEXT")
    private String craftAudiencia;

    @Column(name = "craft_formato", columnDefinition = "TEXT")
    private String craftFormato;

    @Column(name = "craft_tono", columnDefinition = "TEXT")
    private String craftTono;

    @Column(name = "enlace_unico", length = 150, unique = true)
    private String enlaceUnico;

    /** true = sesión activa, false = sesión cerrada/finalizada. */
    @Column(name = "estado")
    private Boolean estado;

    /** Preguntas guía del docente para orientar la sesión socrática. */
    @Column(name = "preguntas_guia", columnDefinition = "TEXT")
    private String preguntasGuia;

    /** RF-07: última síntesis pedagógica grupal generada con IA (Markdown), persistida para que sobreviva a reinicios. */
    @Column(name = "sintesis_md", columnDefinition = "TEXT")
    private String sintesisMd;

    /** RF-07: fecha en que se generó la síntesis grupal. */
    @Column(name = "sintesis_fecha", columnDefinition = "timestamptz")
    private OffsetDateTime sintesisFecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.estado == null) {
            this.estado = true; // activa por defecto al crear
        }
    }
}
