package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "mensaje" de Supabase.
 * Cada fila es un turno de la conversación dentro de una participación.
 * Columnas: id (uuid PK), created_at (timestamptz), emisor (bool), contenido (text),
 *           participacion_id (uuid FK → participacion).
 *
 * emisor:
 *   - true  → el mensaje fue enviado por el ESTUDIANTE
 *   - false → el mensaje fue generado por el TUTOR socrático (Gemini)
 */
@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_creacion", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    /**
     * true  → emitido por el estudiante.
     * false → emitido por el tutor socrático (Gemini).
     */
    @Column(name = "emisor", nullable = false)
    private Boolean emisor;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participacion_id", nullable = false)
    private Participacion participacion;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
