package Prototipo.Proyecto_Grado.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de la comunicación HTTP REST con la API de Google Gemini.
 * Diseñado con alta resiliencia pedagógica:
 * 1. Reintentos automáticos con backoff exponencial para picos de saturación (HTTP 503 Service Unavailable / 429 Too Many Requests).
 * 2. Conmutación por fallo (Graceful Fallback) al modelo auxiliar gemini-3.1-flash-lite-preview si
 *    gemini-3-flash-preview no responde. Las respuestas del auxiliar no pueden cerrar la ruta socrática.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3-flash-preview}")
    private String modelName;

    @Value("${gemini.model.auxiliary:gemini-3.1-flash-lite}")
    private String auxiliaryModelName;

    private static final String DEFAULT_PRIMARY_MODEL = "gemini-3-flash-preview";
    private static final String DEFAULT_AUXILIARY_MODEL = "gemini-3.1-flash-lite";

    /**
     * Envía un prompt a la API de Gemini y retorna el texto generado.
     * Flujo estricto:
     * 1. Siempre inicia prioritariamente intentando con gemini-3-flash-preview.
     * 2. Si Google reporta saturación o alta demanda (HTTP 503 / 429), reintenta con backoff.
     * 3. Únicamente si el modelo principal no responde tras reintentar, activa el auxiliar exclusivo gemini-3.1-flash-lite.
     */
    public String generarRespuesta(String prompt) {
        return generarRespuestaDetallada(prompt).texto();
    }

    /** Mensaje mostrado al estudiante cuando ningún modelo pudo responder. */
    public static final String MENSAJE_ERROR_TEMPORAL =
            "Ocurrió un inconveniente temporal en el servicio del tutor inteligente. Por favor intenta de nuevo en unos segundos.";

    /**
     * Resultado de una invocación a Gemini con metadatos de calidad.
     *
     * @param texto    texto generado (o mensaje de error amigable)
     * @param auxiliar true si respondió el modelo auxiliar de contingencia
     * @param error    true si ningún modelo respondió o la respuesta no es utilizable
     */
    public record RespuestaGemini(String texto, boolean auxiliar, boolean error) {}

    /**
     * Igual que {@link #generarRespuesta(String)}, pero informa qué modelo respondió
     * y si el texto es un error, para que las decisiones críticas (cerrar la ruta
     * socrática, guardar diagnósticos) no se tomen con respuestas degradadas.
     */
    public RespuestaGemini generarRespuestaDetallada(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API Key no configurada. Retornando respuesta socrática simulada para desarrollo.");
            return new RespuestaGemini("[Modo Mock/Desarrollo] Hola. Has planteado una pregunta interesante sobre POO. "
                 + "¿Qué entiendes por este concepto y cómo crees que se relacionan las clases involucradas?", false, false);
        }

        String modeloPrincipal = resolverNombreModelo(modelName != null && !modelName.isBlank() ? modelName : DEFAULT_PRIMARY_MODEL);
        String modeloAuxiliar = resolverNombreModelo(auxiliaryModelName != null && !auxiliaryModelName.isBlank() ? auxiliaryModelName : DEFAULT_AUXILIARY_MODEL);

        // 1. Intento estricto con el modelo principal (siempre prioritario)
        try {
            String texto = ejecutarConReintentos(modeloPrincipal, prompt, 2);
            return new RespuestaGemini(texto, false, esRespuestaDeError(texto));
        } catch (Exception e) {
            log.warn("El modelo primario '{}' no pudo responder tras reintentos ({}). Activando auxiliar de contingencia: '{}'...",
                    modeloPrincipal, e.getMessage(), modeloAuxiliar);

            // 2. Auxiliar exclusivo: solo se activa en contingencia
            if (!modeloPrincipal.equalsIgnoreCase(modeloAuxiliar)) {
                try {
                    log.info("Invocando modelo auxiliar exclusivo: '{}'", modeloAuxiliar);
                    // Un solo intento: acota el peor caso total a ~70 s (principal + auxiliar)
                    String texto = ejecutarConReintentos(modeloAuxiliar, prompt, 1);
                    return new RespuestaGemini(texto, true, esRespuestaDeError(texto));
                } catch (Exception fallbackEx) {
                    log.error("Modelo auxiliar '{}' también falló: {}", modeloAuxiliar, fallbackEx.getMessage());
                }
            }

            return new RespuestaGemini(MENSAJE_ERROR_TEMPORAL, false, true);
        }
    }

    /**
     * Indica si un texto corresponde a un mensaje de error/contingencia generado por este
     * servicio (y no a contenido pedagógico real del modelo).
     */
    public static boolean esRespuestaDeError(String texto) {
        if (texto == null || texto.isBlank()) return true;
        String t = texto.trim();
        return t.startsWith("Ocurrió un inconveniente temporal")
                || t.equals("No se obtuvo respuesta del modelo.")
                || t.equals("Sin candidatos de respuesta.")
                || t.equals("Respuesta vacía.")
                || t.equals("Error al procesar la respuesta generada.");
    }

    private String ejecutarConReintentos(String modelo, String prompt, int maxIntentos) throws Exception {
        Exception ultimoError = null;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                log.info("Invocando Gemini [{}] (intento {}/{})", modelo, intento, maxIntentos);
                return llamarApiGemini(modelo, prompt);
            } catch (RestClientResponseException e) {
                ultimoError = e;
                int status = e.getStatusCode().value();
                boolean esSaturacionTransitoria = (status == 503 || status == 429 || status == 500 || status == 504);

                if (esSaturacionTransitoria && intento < maxIntentos) {
                    long pausaMs = 1200L * intento;
                    log.warn("Gemini [{}] reportó saturación/alta demanda (HTTP {}). Esperando {} ms antes de reintentar...",
                            modelo, status, pausaMs);
                    try {
                        Thread.sleep(pausaMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                } else {
                    log.warn("Error HTTP {} en Gemini [{}]: {}", status, modelo, e.getMessage());
                    throw e;
                }
            } catch (Exception e) {
                ultimoError = e;
                log.warn("Error de conexión al invocar Gemini [{}]: {}", modelo, e.getMessage());
                // Un timeout de lectura ya consumió ~35 s: reintentar el mismo modelo duplicaría la espera
                // del estudiante (y retendría el hilo de Tomcat). Se pasa directamente al siguiente modelo.
                if (esTimeout(e)) {
                    throw e;
                }
                if (intento < maxIntentos) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                } else {
                    throw e;
                }
            }
        }

        throw (ultimoError != null) ? ultimoError : new RuntimeException("No se pudo obtener respuesta de Gemini");
    }

    private static boolean esTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException) return true;
        }
        return false;
    }

    private String llamarApiGemini(String modelo, String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":generateContent";

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            )
        );

        Map<?, ?> response = restClient.post()
            .uri(url)
            .header("x-goog-api-key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(Map.class);

        return extraerTextoDeRespuesta(response);
    }

    private String resolverNombreModelo(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "gemini-3-flash-preview";
        }
        String n = nombre.trim();
        if (n.equalsIgnoreCase("gemini-3") || n.equalsIgnoreCase("gemini-3.0") 
                || n.equalsIgnoreCase("gemini-3-flash") || n.equalsIgnoreCase("gemini-3.0-flash")) {
            return "gemini-3-flash-preview";
        }
        if (n.equalsIgnoreCase("gemini-3.1-flash-lite") || n.equalsIgnoreCase("gemini-3.1-lite")
                || n.equalsIgnoreCase("gemini-3.1-flash-lite-preview")) {
            return "gemini-3.1-flash-lite-preview";
        }
        if (n.equalsIgnoreCase("gemini-2.5-flash-lite") || n.equalsIgnoreCase("gemini-2.5-flash-lite-preview")) {
            return "gemini-2.5-flash-lite";
        }
        if (n.equalsIgnoreCase("gemini-2.5-flash")) {
            return "gemini-2.5-flash";
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private String extraerTextoDeRespuesta(Map<?, ?> response) {
        if (response == null || !response.containsKey("candidates")) {
            return "No se obtuvo respuesta del modelo.";
        }
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates.isEmpty()) return "Sin candidatos de respuesta.";
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts.isEmpty()) return "Respuesta vacía.";
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("Error parseando respuesta de Gemini: {}", e.getMessage());
            return "Error al procesar la respuesta generada.";
        }
    }
}
