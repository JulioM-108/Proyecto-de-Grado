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
 * Cliente HTTP de la API de Google Gemini (generateContent), usado por el tutor (RF-02), los informes
 * individuales y la síntesis grupal (RF-07) y el asistente CRAFT (RF-05).
 * Estrategia de resiliencia:
 * 1. Modelo principal gemini-3-flash: hasta 2 intentos, con espera lineal (1,2 s × intento) ante
 *    HTTP 503/429/500/504. Un timeout no se reintenta (10 s de conexión y 35 s de lectura, en RestClientConfig).
 * 2. Si el principal falla, un único intento con el modelo auxiliar gemini-3.1-flash-lite. Sus respuestas
 *    quedan marcadas como auxiliares y no pueden cerrar la ruta socrática.
 * 3. Si ninguno responde, se devuelve MENSAJE_ERROR_TEMPORAL en lugar de lanzar una excepción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestClient restClient;

    // Sin API key el servicio responde en modo simulado (desarrollo). Los nombres de modelo admiten alias
    // cortos (p. ej., gemini-3-flash) que resolverNombreModelo traduce al identificador de la API.
    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3-flash-preview}")
    private String modelName;

    @Value("${gemini.model.auxiliary:gemini-3.1-flash-lite}")
    private String auxiliaryModelName;

    private static final String DEFAULT_PRIMARY_MODEL = "gemini-3-flash-preview";
    private static final String DEFAULT_AUXILIARY_MODEL = "gemini-3.1-flash-lite";

    /**
     * Envía un prompt a Gemini y retorna solo el texto generado (principal y, en contingencia, auxiliar).
     * No lanza excepción si la API falla: devuelve un texto de error, por lo que quien guarde el resultado
     * debe comprobarlo con {@link #esTextoGeneradoValido(String)}.
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
     * Sin API key devuelve una respuesta simulada con PREFIJO_SIMULADO (no marcada como error).
     * Bloquea el hilo llamante mientras dura la llamada HTTP (peor caso cercano a 70 s).
     */
    public RespuestaGemini generarRespuestaDetallada(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API Key no configurada. Retornando respuesta socrática simulada para desarrollo.");
            return new RespuestaGemini(PREFIJO_SIMULADO + " Hola. Has planteado una pregunta interesante sobre POO. "
                 + "¿Qué entiendes por este concepto y cómo crees que se relacionan las clases involucradas?", false, false);
        }

        String modeloPrincipal = resolverNombreModelo(modelName != null && !modelName.isBlank() ? modelName : DEFAULT_PRIMARY_MODEL);
        String modeloAuxiliar = resolverNombreModelo(auxiliaryModelName != null && !auxiliaryModelName.isBlank() ? auxiliaryModelName : DEFAULT_AUXILIARY_MODEL);

        // 1. Intento estricto con el modelo principal (siempre prioritario). Una respuesta HTTP 200 sin texto
        //    utilizable se marca como error, pero no activa el auxiliar: este solo entra si hubo excepción.
        try {
            String texto = ejecutarConReintentos(modeloPrincipal, prompt, 2);
            return new RespuestaGemini(texto, false, esRespuestaDeError(texto));
        } catch (Exception e) {
            log.warn("El modelo primario '{}' no pudo responder tras reintentos ({}). Activando auxiliar de contingencia: '{}'...",
                    modeloPrincipal, e.getMessage(), modeloAuxiliar);

            // 2. Auxiliar exclusivo: solo se activa en contingencia y si es un modelo distinto del principal
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

    /** Prefijo de las respuestas simuladas cuando no hay API key (solo desarrollo). */
    public static final String PREFIJO_SIMULADO = "[Modo Mock/Desarrollo]";

    /**
     * true si el texto es contenido real del modelo: ni un error/contingencia ni una respuesta simulada.
     * Usar antes de guardar informes o síntesis.
     */
    public static boolean esTextoGeneradoValido(String texto) {
        return !esRespuestaDeError(texto) && !texto.trim().startsWith(PREFIJO_SIMULADO);
    }

    /**
     * Indica si un texto corresponde a un mensaje de error/contingencia generado por este
     * servicio (y no a contenido pedagógico real del modelo). Los textos comparados deben coincidir
     * con MENSAJE_ERROR_TEMPORAL y con los que devuelve extraerTextoDeRespuesta.
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

    /**
     * Invoca un modelo hasta maxIntentos veces. Reintenta los errores HTTP transitorios (503/429/500/504)
     * con una espera de 1,2 s × intento y los errores de conexión con 1 s; cualquier otro error HTTP o un
     * timeout se propaga de inmediato para pasar al siguiente modelo.
     *
     * @return el texto extraído de la respuesta (puede ser uno de los textos de error de extraerTextoDeRespuesta)
     * @throws Exception el último error si se agotan los intentos
     */
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
                // Un timeout (35 s de lectura o 10 s de conexión) ya consumió tiempo: reintentar el mismo modelo
                // alargaría la espera del estudiante (y retendría el hilo de Tomcat). Se pasa al siguiente modelo.
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

    /** Busca un SocketTimeoutException en la cadena de causas (RestClient lo envuelve en ResourceAccessException). */
    private static boolean esTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException) return true;
        }
        return false;
    }

    /**
     * Hace un POST a generateContent (v1beta) con el prompt como único contenido y la API key en la cabecera.
     *
     * @throws RestClientResponseException si la API responde con un estado HTTP de error
     */
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

    /**
     * Traduce los alias configurables al identificador real de la API (p. ej., gemini-3-flash pasa a
     * gemini-3-flash-preview). Un nombre no reconocido se usa tal cual.
     */
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

    /**
     * Extrae el texto de la primera parte del primer candidato. Si la estructura no es la esperada, devuelve
     * un texto de error fijo que esRespuestaDeError reconoce.
     */
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
