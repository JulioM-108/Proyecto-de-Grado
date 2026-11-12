package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla metrica de Supabase.
 * Registro de eventos numéricos de una participación que alimentan las métricas del dashboard docente.
 * Columnas: id (uuid PK), fecha_registro (timestamptz), codigo (int8), concepto (varchar),
 *           valor_numerico (int8), participacion_id (uuid FK → participacion).
 * Relación: muchas métricas pertenecen a una {@link Participacion}.
 */
@Entity
@Table(name = "metrica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metrica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_registro", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    /**
     * Tipo de métrica (los registra ChatService):
     * <ul>
     *   <li>2: total de mensajes de la conversación tras cada turno (registro de actividad).</li>
     *   <li>10: solicitud de respuesta directa (M-03).</li>
     *   <li>20: apertura de la ruta, turnos previos a la Pregunta Guía #1 (M-02).</li>
     *   <li>21: pregunta guía superada; el valor es el número de turnos que tomó (M-02).</li>
     * </ul>
     */
    @Column(name = "codigo")
    private Long codigo;

    /**
     * Etiqueta según el código: tema de la sesión (2), "M-03" (10; los registros antiguos usan "RF-10"),
     * "APERTURA" (20) o la pregunta guía, p. ej. "P3" o "P2-P4" si el tutor omitió marcas (21).
     */
    @Column(name = "concepto", length = 150)
    private String concepto;

    /** Valor según el código: total de mensajes (2), 1 por solicitud (10) o número de turnos (20 y 21). */
    @Column(name = "valor_numerico")
    private Long valorNumerico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participacion_id", nullable = false)
    private Participacion participacion;

    /** Asigna la fecha de registro al insertar si no se proporcionó. */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
