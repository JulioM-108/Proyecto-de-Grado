package Prototipo.Proyecto_Grado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración del cliente HTTP para consumir la API de Gemini y del ObjectMapper compartido.
 * Incluye timeouts de conexión y lectura para evitar bloqueos indefinidos durante congestión de red.
 */
@Configuration
public class RestClientConfig {

    /** RestClient que usa GeminiService: 10 s para conectar y 35 s de espera máxima de la respuesta. */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(35));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * ObjectMapper de Jackson 2 (com.fasterxml) que inyecta PromptAssistantService para leer el JSON
     * de las directrices CRAFT. Se declara a mano porque Spring Boot 4 autoconfigura Jackson 3.
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
