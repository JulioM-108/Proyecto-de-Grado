package Prototipo.Proyecto_Grado.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Módulo de Doble Verificación (RF-10 - Auditor de Respuestas).
 * Evalúa si la respuesta de Gemini entregó soluciones directas de código
 * en lugar de guiar con preguntas y analogías (Método Socrático).
 * Si detecta violación de la regla socrática, solicita una reformulación inmediata.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocraticAuditorService {

    private final GeminiService geminiService;

    // Patrones heurísticos para detectar bloques de código completos o directos
    private static final Pattern PATRON_CODIGO_BLOQUE = Pattern.compile("```[a-zA-Z]*\\s*(class|public|void|def|function|int|String|return)[\\s\\S]*?```");
    private static final Pattern PATRON_ESTRUCTURA_CLASE = Pattern.compile("public\\s+class\\s+[A-Za-z0-9_]+|class\\s+[A-Za-z0-9_]+");

    public record ResultadoAuditoria(String respuestaFinal, boolean fueReformulada) {}

    /**
     * Audita la respuesta del tutor y la reformula si entrega código directo.
     */
    public ResultadoAuditoria auditarYCorregir(String respuestaOriginal, String consultaEstudiante) {
        boolean violaReglaSocratica = detectarViolacionRegla(respuestaOriginal);

        if (!violaReglaSocratica) {
            return new ResultadoAuditoria(respuestaOriginal, false);
        }

        log.warn("RF-10 ACTIVADO: La respuesta del LLM violó la regla socrática (entregó código directo). Solicitando reformulación...");
        String respuestaReformulada = solicitarReformulacionSocratica(respuestaOriginal, consultaEstudiante);

        return new ResultadoAuditoria(respuestaReformulada, true);
    }

    /**
     * Detecta si la respuesta contiene código directo o soluciones completas.
     */
    public boolean detectarViolacionRegla(String respuesta) {
        if (respuesta == null) return false;
        
        // Verifica si contiene bloques de código con declaraciones de clases o métodos completos
        if (PATRON_CODIGO_BLOQUE.matcher(respuesta).find()) {
            return true;
        }
        
        // Verifica si tiene código plano con estructura de clase directa
        if (PATRON_ESTRUCTURA_CLASE.matcher(respuesta).find()) {
            return true;
        }

        return false;
    }

    /**
     * Solicita al LLM transformar la respuesta en un cuestionamiento socrático guiado.
     */
    private String solicitarReformulacionSocratica(String respuestaConCodigo, String consultaEstudiante) {
        String promptReformulacion = """
            Eres un Auditor Pedagógico Socrático para cursos de Programación Orientada a Objetos (POO).
            Un tutor inteligente generó la siguiente respuesta para un estudiante, pero VIOLÓ la regla socrática porque proporcionó código directo:
            
            --- RESPUESTA CON CÓDIGO DIRECTO ---
            %s
            -----------------------------------
            
            Consulta original del estudiante: "%s"
            
            INSTRUCCIÓN DE REFORMULACIÓN:
            1. ELIMINA por completo cualquier bloque de código o solución implementada.
            2. Transforma la explicación en una o dos preguntas guía o una analogía del mundo real que ayude al estudiante a descubrir la respuesta por sí mismo.
            3. Mantén un tono motivador, reflexivo y pedagógico.
            """.formatted(respuestaConCodigo, consultaEstudiante);

        return geminiService.generarRespuesta(promptReformulacion);
    }
}
