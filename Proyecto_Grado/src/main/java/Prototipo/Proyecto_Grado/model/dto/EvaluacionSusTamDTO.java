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
 * DTO de la encuesta final del estudiante: usabilidad (SUS, ítems 1 a 10) y aceptación pedagógica
 * (TAM, ítems 11 a 16). Se persiste como JSON en participacion.evaluacion_sus_tam.
 * Además de los datos, ofrece el cálculo del puntaje SUS, su interpretación cualitativa y la
 * conversión desde y hacia JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluacionSusTamDTO {

    // ObjectMapper estático: el DTO se (de)serializa desde la entidad Participacion, fuera de la inyección de Spring
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Puntaje SUS calculado con {@link #calcularSusScore(List)} (0.0 a 100.0, con un decimal). */
    private Double susScore;

    /** 10 respuestas de la escala SUS (Brooke, 1986) con valores de 1 a 5, en el orden de los ítems. */
    private List<Integer> susRespuestas;

    /**
     * 6 respuestas de aceptación tecnológica (TAM - Davis, 1989; ítems 11 a 16 de la encuesta) con valores
     * de 1 a 5, indexadas por estas claves:
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

    /** Fecha y hora de diligenciamiento en formato ISO-8601 (OffsetDateTime.toString()). */
    private String fechaRegistro;

    /**
     * Calcula el puntaje estándar SUS (Brooke, 1986).
     * Preguntas impares (1, 3, 5, 7, 9 -> índices 0, 2, 4, 6, 8): (respuesta - 1)
     * Preguntas pares (2, 4, 6, 8, 10 -> índices 1, 3, 5, 7, 9): (5 - respuesta)
     * La suma de las 10 contribuciones se multiplica por 2.5 (rango 0 a 100).
     * Cada respuesta se acota al rango 1 a 5 y el resultado se redondea a un decimal.
     *
     * @return el puntaje SUS, o 0.0 si la lista es nula o no tiene exactamente 10 respuestas
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
            if (i % 2 == 0) { // Índice par = ítem impar (1, 3, 5, 7, 9)
                sumImpares += (val - 1);
            } else { // Índice impar = ítem par (2, 4, 6, 8, 10)
                sumPares += (5 - val);
            }
        }
        double total = (sumImpares + sumPares) * 2.5;
        return Math.round(total * 10.0) / 10.0;
    }

    /**
     * Retorna la interpretación cualitativa del puntaje SUS con los umbrales de la plataforma:
     * 80,3 o más Excelente; 68 o más Bueno/Aceptable (68 es el promedio de referencia del SUS);
     * 51 o más Marginal; por debajo, Deficiente. Con null devuelve "Sin evaluar".
     */
    public static String interpretarSus(Double score) {
        if (score == null) return "Sin evaluar";
        if (score >= 80.3) return "Excelente (Grado A)";
        if (score >= 68.0) return "Bueno / Aceptable (Estándar industria)";
        if (score >= 51.0) return "Marginal / Regular";
        return "Deficiente / Requiere atención";
    }

    /** Serializa el DTO a JSON; si falla devuelve "{}". */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Deserializa el JSON al DTO (ignora propiedades desconocidas); null si está vacío o no es válido. */
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
