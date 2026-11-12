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
 * Equivale a los campos craft_* de Sesion más el tema y las preguntas guía. Lo usan el formulario de
 * nueva clase y PromptAssistantService, que lo devuelve como propuesta generada con IA (RF-05).
 * ChatService admite uno opcional para la Capa 2; el chat pasa null y se usan los campos de la Sesion.
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
    private String temaActualPoo; // Tema de la clase (Sesion.tema), p. ej. "Polimorfismo y Herencia"
    private String preguntasGuia; // Preguntas guía del docente (una por línea)
    private boolean generadoPorIa; // false si la IA no respondió y se usó la plantilla base
}
