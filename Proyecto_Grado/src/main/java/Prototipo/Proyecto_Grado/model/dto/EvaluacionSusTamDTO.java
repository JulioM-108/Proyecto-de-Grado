package Prototipo.Proyecto_Grado.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Objeto DTO que estructura y encapsula la evaluación psicométrica de usabilidad (SUS)
 * y aceptación pedagógica (TAM) para persistir como JSON en participacion.evaluacion_sus_tam.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluacionSusTamDTO {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Puntaje oficial SUS calculado (escala de 0.0 a 100.0) */
    private Double susScore;

    /** 10 respuestas de la escala SUS (Brooke, 1986) con valores de 1 a 5 */
    private List<Integer> susRespuestas;

    /**
     * 6 respuestas de aceptación tecnológica (TAM - Davis, 1989) con valores de 1 a 5:
     * - utilidadPoo: Utilidad de preguntas y analogías para entender POO
     * - autonomia: Identificación de propios errores y resolución autónoma
     * - frustracion: Reto reflexivo constructivo vs frustración por falta de código
     * - claridadDialogo: Ritmo de una pregunta a la vez y metáforas cotidianas
     * - ventajaIa: Ventaja formativa frente a ChatGPT comercial
     * - adopcionFutura: Intención de uso en futuras materias de programación
     */
    private Map<String, Integer> tamRespuestas;

    /** Comentario cualitativo abierto opcional */
    private String comentario;

    /** Fecha y hora de diligenciamiento en formato ISO */
    private String fechaRegistro;

    /**
     * Calcula el puntaje estándar SUS (Brooke, 1986).
     * Preguntas impares (1, 3, 5, 7, 9 -> índices 0, 2, 4, 6, 8): (respuesta - 1)
     * Preguntas pares (2, 4, 6, 8, 10 -> índices 1, 3, 5, 7, 9): (5 - respuesta)
     * La suma de las 10 contribuciones se multiplica por 2.5 (rango 0 a 100).
     */
    public static Double calcularSusScore(List<Integer> respuestas) {
        if (respuestas == null || respuestas.size() != 10) {
            return 0.0;
        }
        double sumImpares = 0.0;
        double sumPares = 0.0;

        for (int i = 0; i < 10; i++) {
            int val = respuestas.get(i);
            // Asegurar rango 1 a 5
            val = Math.max(1, Math.min(5, val));
            if (i % 2 == 0) { // Impares
                sumImpares += (val - 1);
            } else { // Pares
                sumPares += (5 - val);
            }
        }
        double total = (sumImpares + sumPares) * 2.5;
        return Math.round(total * 10.0) / 10.0;
    }

    /**
     * Retorna la interpretación cualitativa de usabilidad según el estándar de la industria.
     */
    public static String interpretarSus(Double score) {
        if (score == null) return "Sin evaluar";
        if (score >= 80.3) return "Excelente (Grado A)";
        if (score >= 68.0) return "Bueno / Aceptable (Estándar industria)";
        if (score >= 51.0) return "Marginal / Regular";
        return "Deficiente / Requiere atención";
    }

    /** Serializa el DTO a formato JSON */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Deserializa una cadena JSON al DTO */
    public static EvaluacionSusTamDTO fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, EvaluacionSusTamDTO.class);
        } catch (Exception e) {
            return null;
        }
    }
}
