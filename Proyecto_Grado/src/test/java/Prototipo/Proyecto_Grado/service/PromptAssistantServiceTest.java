package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de PromptAssistantService, que propone con IA los campos CRAFT de una clase a partir
 * de su tema. GeminiService se simula con Mockito: no llama a la API, no levanta Spring ni usa la BD.
 */
@ExtendWith(MockitoExtension.class)
class PromptAssistantServiceTest {

    @Mock
    private GeminiService geminiService;

    private PromptAssistantService promptAssistantService;

    @BeforeEach
    void setUp() {
        promptAssistantService = new PromptAssistantService(geminiService, new ObjectMapper());
    }

    /** Extrae los cinco campos CRAFT del JSON de Gemini, aunque llegue envuelto en un bloque de código Markdown. */
    @Test
    void testGenerarDirectricesCraftExitoso() {
        String jsonSimulado = """
            ```json
            {
              "context": "Contexto de POO sobre Polimorfismo",
              "role": "Tutor reflexivo",
              "audience": "Estudiantes de tercer semestre",
              "format": "Analogía cotidiana + pregunta orientadora",
              "tone": "Empático y riguroso"
            }
            ```
            """;

        when(geminiService.generarRespuesta(anyString())).thenReturn(jsonSimulado);

        CraftConfigDto resultado = promptAssistantService.generarDirectricesCraft("Polimorfismo e Interfaces");

        assertNotNull(resultado);
        assertEquals("Polimorfismo e Interfaces", resultado.getTemaActualPoo());
        assertEquals("Contexto de POO sobre Polimorfismo", resultado.getContext());
        assertEquals("Tutor reflexivo", resultado.getRole());
        assertEquals("Estudiantes de tercer semestre", resultado.getAudience());
        assertEquals("Analogía cotidiana + pregunta orientadora", resultado.getFormat());
        assertEquals("Empático y riguroso", resultado.getTone());
    }

    /** Si Gemini falla, devuelve valores CRAFT de respaldo (no nulos) en lugar de propagar el error. */
    @Test
    void testGenerarDirectricesCraftFallbackPorError() {
        when(geminiService.generarRespuesta(anyString())).thenThrow(new RuntimeException("API Timeout"));

        CraftConfigDto resultado = promptAssistantService.generarDirectricesCraft("Herencia");

        assertNotNull(resultado);
        assertEquals("Herencia", resultado.getTemaActualPoo());
        assertNotNull(resultado.getContext());
        assertNotNull(resultado.getRole());
        assertNotNull(resultado.getAudience());
        assertNotNull(resultado.getFormat());
        assertNotNull(resultado.getTone());
    }
}
