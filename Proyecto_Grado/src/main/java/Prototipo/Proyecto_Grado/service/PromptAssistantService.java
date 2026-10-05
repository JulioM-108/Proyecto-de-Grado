package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio del Asistente IA para la Configuración de Prompts Pedagógicos (Módulo 3).
 * Utiliza Gemini (GeminiService) para asistir al profesor en la construcción de directrices
 * socráticas estructuradas bajo el framework CRAFT (Context, Role, Audience, Format, Tone).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptAssistantService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Genera una propuesta CRAFT socrática a partir del tema de POO suministrado por el docente.
     *
     * @param tema Tema o concepto de POO (ej. "Polimorfismo y Clases Abstractas").
     * @return DTO con los 5 campos CRAFT listos para revisión y edición del profesor.
     */
    public CraftConfigDto generarDirectricesCraft(String tema) {
        String temaLimpio = (tema != null && !tema.isBlank()) ? tema.trim() : "Conceptos Fundamentales de POO";

        String promptAsistente = """
            Eres un Diseñador Instruccional experto en Programación Orientada a Objetos (POO) y Pedagogía Socrática.
            Un docente universitario desea configurar un Tutor Inteligente Socrático para sus estudiantes sobre el tema: "%s".

            Genera la directriz pedagógica bajo el framework CRAFT (Context, Role, Audience, Format, Tone) para que el tutor inteligente guíe a los estudiantes mediante preguntas y analogías, NUNCA proporcionando la solución completa de código.

            Debes responder ÚNICAMENTE con un bloque JSON válido (sin texto adicional antes ni después) con exactamente estas claves:
            {
              "context": "Contexto del tema dentro de POO, prerequisitos y pertinencia en el software.",
              "role": "Rol pedagógico socrático asignado al tutor (ej. guía reflexivo que descompone dudas mediante preguntas clave y analogías cotidianas).",
              "audience": "Perfil del grupo objetivo (ej. estudiantes universitarios de ingeniería de sistemas en etapa de fundamentación).",
              "format": "Formato de intervención (ej. 1 analogía del mundo real seguida de UNA sola pregunta orientadora, prohibido entregar bloques de código funcionales).",
              "tone": "Tono comunicativo (ej. empático, constructivo, motivador y riguroso)."
            }
            """.formatted(temaLimpio);

        try {
            String respuestaGemini = geminiService.generarRespuesta(promptAsistente);
            return extraerCraftDeJson(respuestaGemini, temaLimpio);
        } catch (Exception e) {
            log.error("Error generando propuesta CRAFT con Gemini: {}", e.getMessage(), e);
            return generarPropuestaFallback(temaLimpio);
        }
    }

    private CraftConfigDto extraerCraftDeJson(String rawText, String tema) {
        if (rawText == null || rawText.isBlank()) {
            return generarPropuestaFallback(tema);
        }

        try {
            // Limpiar posibles delimitadores Markdown ```json ... ```
            String jsonLimpio = rawText.replaceAll("(?s)^```json\\s*", "")
                                       .replaceAll("(?s)^```\\s*", "")
                                       .replaceAll("(?s)\\s*```$", "")
                                       .trim();

            // Si hay texto antes o después del JSON, extraer el contenido entre { y }
            int primerBrace = jsonLimpio.indexOf('{');
            int ultimoBrace = jsonLimpio.lastIndexOf('}');
            if (primerBrace != -1 && ultimoBrace != -1 && ultimoBrace > primerBrace) {
                jsonLimpio = jsonLimpio.substring(primerBrace, ultimoBrace + 1);
            }

            JsonNode root = objectMapper.readTree(jsonLimpio);

            return CraftConfigDto.builder()
                .temaActualPoo(tema)
                .context(root.path("context").asText("Curso de Programación Orientada a Objetos"))
                .role(root.path("role").asText("Tutor socrático orientador"))
                .audience(root.path("audience").asText("Estudiantes de ingeniería de sistemas"))
                .format(root.path("format").asText("Preguntas reflexivas y analogías concisas"))
                .tone(root.path("tone").asText("Empático, motivador y reflexivo"))
                .generadoPorIa(true)
                .build();

        } catch (Exception ex) {
            log.warn("No se pudo parsear el JSON de Gemini, aplicando fallback estructurado. Raw: {}", rawText);
            return generarPropuestaFallback(tema);
        }
    }

    private CraftConfigDto generarPropuestaFallback(String tema) {
        return CraftConfigDto.builder()
            .temaActualPoo(tema)
            .context("Curso de Programación Orientada a Objetos. Sesión focalizada en: " + tema)
            .role("Tutor pedagógico socrático. Guía al estudiante mediante cuestionamientos reflexivos sin revelar código directo.")
            .audience("Estudiantes de Ingeniería de Sistemas de la Pontificia Universidad Javeriana Cali.")
            .format("1 analogía de la vida real que ilustre el concepto + 1 pregunta orientadora para que el estudiante formule la solución.")
            .tone("Cálido, empático, motivador y académicamente riguroso.")
            .build();
    }
}
