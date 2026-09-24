package Prototipo.Proyecto_Grado.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla "metrica" de Supabase.
 * Almacena métricas discretas de desempeño asociadas a una participación.
 * Columnas: id (uuid PK), created_at (timestamptz), codigo (int8), concepto (varchar),
 *           valor_numerico (int8), participacion_id (uuid FK → participacion).
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

    /** Código numérico que identifica el tipo de métrica (int8 en Supabase). */
    @Column(name = "codigo")
    private Long codigo;

    /** Nombre del concepto POO evaluado (ej: "Herencia", "Polimorfismo"). */
    @Column(name = "concepto", length = 150)
    private String concepto;

    /** Valor cuantitativo de la métrica (conteo, puntaje, intentos, etc.). */
    @Column(name = "valor_numerico")
    private Long valorNumerico;

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
