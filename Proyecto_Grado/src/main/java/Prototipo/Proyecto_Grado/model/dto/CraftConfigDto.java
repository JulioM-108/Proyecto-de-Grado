package Prototipo.Proyecto_Grado.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la configuración pedagógica del docente basada en la estructura CRAFT:
 * - Context (Contexto de la lección)
 * - Role (Rol pedagógico del tutor)
 * - Audience (Público objetivo / nivel del estudiante)
 * - Format (Formato de las explicaciones)
 * - Tone (Tono conversacional y motivador)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CraftConfigDto {
    private String context;
    private String role;
    private String audience;
    private String format;
    private String tone;
    private String temaActualPoo; // Ej: "Polimorfismo y Herencia"
    private String preguntasGuia; // Preguntas guía del docente (una por línea)
}
