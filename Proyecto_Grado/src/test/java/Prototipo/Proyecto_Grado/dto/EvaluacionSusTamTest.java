package Prototipo.Proyecto_Grado.dto;

import Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluacionSusTamTest {

    @Test
    @DisplayName("Debe calcular 100.0 puntos cuando la usabilidad es perfecta (5 en impares, 1 en pares)")
    void testCalculoSusMaximo() {
        // Impares: 5, Pares: 1
        List<Integer> respuestasPerfectas = Arrays.asList(5, 1, 5, 1, 5, 1, 5, 1, 5, 1);
        Double score = EvaluacionSusTamDTO.calcularSusScore(respuestasPerfectas);
        assertEquals(100.0, score);
        assertEquals("Excelente (Grado A)", EvaluacionSusTamDTO.interpretarSus(score));
    }

    @Test
    @DisplayName("Debe calcular 0.0 puntos cuando la usabilidad es mínima (1 en impares, 5 en pares)")
    void testCalculoSusMinimo() {
        List<Integer> respuestasMinimas = Arrays.asList(1, 5, 1, 5, 1, 5, 1, 5, 1, 5);
        Double score = EvaluacionSusTamDTO.calcularSusScore(respuestasMinimas);
        assertEquals(0.0, score);
        assertEquals("Deficiente / Requiere atención", EvaluacionSusTamDTO.interpretarSus(score));
    }

    @Test
    @DisplayName("Debe calcular 50.0 puntos cuando todas las respuestas son neutrales (3)")
    void testCalculoSusNeutro() {
        List<Integer> respuestasNeutras = Arrays.asList(3, 3, 3, 3, 3, 3, 3, 3, 3, 3);
        Double score = EvaluacionSusTamDTO.calcularSusScore(respuestasNeutras);
        assertEquals(50.0, score);
    }

    @Test
    @DisplayName("Debe serializar y deserializar correctamente hacia y desde JSON")
    void testJsonRoundtrip() {
        List<Integer> sus = Arrays.asList(5, 2, 4, 1, 5, 2, 4, 1, 5, 1);
        Double score = EvaluacionSusTamDTO.calcularSusScore(sus);

        Map<String, Integer> tam = new HashMap<>();
        tam.put("utilidadPoo", 5);
        tam.put("autonomia", 4);
        tam.put("frustracion", 5);
        tam.put("claridadDialogo", 4);
        tam.put("ventajaIa", 5);
        tam.put("adopcionFutura", 5);

        EvaluacionSusTamDTO dtoOriginal = EvaluacionSusTamDTO.builder()
                .susScore(score)
                .susRespuestas(sus)
                .tamRespuestas(tam)
                .comentario("Excelente experiencia socrática")
                .fechaRegistro("2026-09-27T10:00:00")
                .build();

        String json = dtoOriginal.toJson();
        assertNotNull(json);
        assertTrue(json.contains("susScore"));
        assertTrue(json.contains("utilidadPoo"));

        EvaluacionSusTamDTO dtoRecuperado = EvaluacionSusTamDTO.fromJson(json);
        assertNotNull(dtoRecuperado);
        assertEquals(score, dtoRecuperado.getSusScore());
        assertEquals("Excelente experiencia socrática", dtoRecuperado.getComentario());
        assertEquals(5, dtoRecuperado.getTamRespuestas().get("utilidadPoo"));
    }
}
