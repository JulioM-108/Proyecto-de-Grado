package Prototipo.Proyecto_Grado.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de la comunicación HTTP REST con la API de Google Gemini (Gemini 3 Flash).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    /**
     * Envía un prompt a la API de Gemini y retorna el texto generado.
     */
    public String generarRespuesta(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API Key no configurada. Retornando respuesta socrática simulada para desarrollo.");
            return "[Modo Mock/Desarrollo] Hola. Has planteado una pregunta interesante sobre POO. "
                 + "¿Qué entiendes por este concepto y cómo crees que se relacionan las clases involucradas?";
        }

        try {
            String modeloResuelto = (modelName != null && (modelName.contains("gemini-3.0-flash") || modelName.contains("gemini-3-flash")))
                    ? "gemini-3-flash-preview"
                    : modelName;

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modeloResuelto + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            );

            Map<?, ?> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            return extraerTextoDeRespuesta(response);
        } catch (Exception e) {
            log.error("Error al comunicarse con la API de Gemini: {}", e.getMessage(), e);
            return "Ocurrió un inconveniente al consultar con el tutor inteligente. Por favor intenta de nuevo.";
        }
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
