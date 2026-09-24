package Prototipo.Proyecto_Grado.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa la arquitectura de prompts en 3 capas:
 * - Capa 1: Fija / Sistema (reglas socráticas inviolables).
 * - Capa 2: Docente / CRAFT (parámetros pedagógicos configurados por el profesor).
 * - Capa 3: Estudiante / Dinámica (contexto actual y progreso_md acumulado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCapasDto {
    private String capa1Sistema;
    private String capa2DocenteCraft;
    private String capa3EstudianteDinamica;

    /**
     * Ensambla el prompt consolidado final para enviar al modelo LLM.
     */
    public String compilarPromptFinal(String mensajeEstudiante) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CAPA 1: REGLAS DEL SISTEMA SOCRÁTICO ===\n");
        sb.append(capa1Sistema).append("\n\n");

        if (capa2DocenteCraft != null && !capa2DocenteCraft.isBlank()) {
            sb.append("=== CAPA 2: DIRECTRICES DOCENTES (CRAFT) ===\n");
            sb.append(capa2DocenteCraft).append("\n\n");
        }

        if (capa3EstudianteDinamica != null && !capa3EstudianteDinamica.isBlank()) {
            sb.append("=== CAPA 3: HISTORIAL Y PROGRESO DEL ESTUDIANTE ===\n");
            sb.append(capa3EstudianteDinamica).append("\n\n");
        }

        sb.append("=== CONSULTA DEL ESTUDIANTE ===\n");
        sb.append(mensajeEstudiante).append("\n");

        return sb.toString();
    }
}
