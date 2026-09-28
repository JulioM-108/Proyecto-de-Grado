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
 * 2. Conmutación por fallo transparente (Graceful Fallback) a gemini-2.5-flash si gemini-3-flash-preview está saturado.
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

    @Value("${gemini.model.auxiliary:gemini-3.5-flash-lite}")
    private String auxiliaryModelName;

    private static final String DEFAULT_PRIMARY_MODEL = "gemini-3-flash-preview";
    private static final String DEFAULT_AUXILIARY_MODEL = "gemini-3.5-flash-lite";

    /**
     * Envía un prompt a la API de Gemini y retorna el texto generado.
     * Flujo estricto:
     * 1. Siempre inicia prioritariamente intentando con gemini-3-flash-preview.
     * 2. Si Google reporta saturación o alta demanda (HTTP 503 / 429), reintenta con backoff.
     * 3. Únicamente si el modelo principal no responde tras reintentar, activa el auxiliar exclusivo gemini-3.5-flash-lite.
     */
    public String generarRespuesta(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API Key no configurada. Retornando respuesta socrática simulada para desarrollo.");
            return "[Modo Mock/Desarrollo] Hola. Has planteado una pregunta interesante sobre POO. "
                 + "¿Qué entiendes por este concepto y cómo crees que se relacionan las clases involucradas?";
        }

        String modeloPrincipal = resolverNombreModelo(modelName != null && !modelName.isBlank() ? modelName : DEFAULT_PRIMARY_MODEL);
        String modeloAuxiliar = (auxiliaryModelName != null && !auxiliaryModelName.isBlank()) ? auxiliaryModelName.trim() : DEFAULT_AUXILIARY_MODEL;

        // 1. Intento estricto con el modelo principal (siempre prioritario)
        try {
            return ejecutarConReintentos(modeloPrincipal, prompt, 2);
        } catch (Exception e) {
            log.warn("El modelo primario '{}' no pudo responder tras reintentos ({}). Activando auxiliar de contingencia: '{}'...",
                    modeloPrincipal, e.getMessage(), modeloAuxiliar);

            // 2. Auxiliar exclusivo: solo se activa en contingencia
            if (!modeloPrincipal.equalsIgnoreCase(modeloAuxiliar)) {
                try {
                    log.info("Invocando modelo auxiliar exclusivo: '{}'", modeloAuxiliar);
                    return ejecutarConReintentos(modeloAuxiliar, prompt, 2);
                } catch (Exception fallbackEx) {
                    log.error("Modelo auxiliar '{}' también falló: {}", modeloAuxiliar, fallbackEx.getMessage());
                }
            }

            return "Ocurrió un inconveniente temporal en el servicio del tutor inteligente. Por favor intenta de nuevo en unos segundos.";
        }
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
