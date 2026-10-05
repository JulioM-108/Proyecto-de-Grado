package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.*;
import Prototipo.Proyecto_Grado.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ParticipacionRepository participacionRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private MetricaRepository metricaRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Sesion sesionDemo;
    private Profesor profesorDemo;

    @BeforeEach
    void setUp() {
        profesorDemo = Profesor.builder()
                .id(UUID.randomUUID())
                .nombre("Profesor Demo")
                .correo("profesor@javerianacali.edu.co")
                .build();

        sesionDemo = Sesion.builder()
                .id(UUID.randomUUID())
                .tema("Polimorfismo e Interfaces")
                .craftRol("Tutor socrático riguroso")
                .craftContexto("Curso POO Javeriana")
                .enlaceUnico("K9X2P7")
                .profesor(profesorDemo)
                .estado(true)
                .build();
    }

    @Test
    @DisplayName("Obtener métricas por sesión vacía (sin participantes aún)")
    void testObtenerMetricasPorSesionVacia() {
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(List.of());

        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesionDemo);

        assertNotNull(metricas);
        assertEquals(0, metricas.getTotalParticipaciones());
        assertEquals(0, metricas.getTotalMensajes());
        assertEquals(0, metricas.getTotalConsultasEstudiante());
        assertEquals(0, metricas.getTotalRespuestasTutor());
        assertEquals(0, metricas.getTotalIntervencionesAuditorRf10());
        assertNotNull(metricas.getResumenIa());
        assertTrue(metricas.getResumenIa().contains("Aún no se registran interacciones"));
        assertTrue(metricas.getParticipacionesDetalle().isEmpty());
    }

    @Test
    @DisplayName("Obtener métricas con interacciones y activación del Auditor RF-10")
    void testObtenerMetricasConInteracciones() {
        Estudiante estudiante = Estudiante.builder()
                .id(UUID.randomUUID())
                .nombre("Estudiante 1")
                .correo("estudiante1@javerianacali.edu.co")
                .build();

        Participacion part = Participacion.builder()
                .id(UUID.randomUUID())
                .sesion(sesionDemo)
                .estudiante(estudiante)
                .createdAt(OffsetDateTime.now())
                .progresoMd("- Consulta: ¿Qué es polimorfismo?")
                .build();

        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(List.of(part));

        Mensaje msgEstudiante = Mensaje.builder()
                .emisor(true)
                .contenido("¿Qué es polimorfismo?")
                .participacion(part)
                .build();

        Mensaje msgTutor = Mensaje.builder()
                .emisor(false)
                .contenido("Imagina que tienes una interfaz Instrumento Musical...")
                .participacion(part)
                .build();

        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(List.of(part)))
                .thenReturn(List.of(msgEstudiante, msgTutor));

        Metrica rf10 = Metrica.builder()
                .codigo(10L)
                .concepto("RF-10")
                .valorNumerico(1L)
                .participacion(part)
                .build();

        when(metricaRepository.findByParticipacionIn(List.of(part))).thenReturn(List.of(rf10));
        when(geminiService.generarRespuesta(any())).thenReturn("Diagnóstico: El grupo muestra curiosidad.");

        // 1. Inicialmente sin generar bajo demanda, el dashboard carga sin llamar a Gemini (resumenIa vacío en caché)
        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesionDemo);

        assertNotNull(metricas);
        assertEquals(1, metricas.getTotalParticipaciones());
        assertEquals(2, metricas.getTotalMensajes());
        assertEquals(1, metricas.getTotalConsultasEstudiante());
        assertEquals(1, metricas.getTotalRespuestasTutor());
        assertEquals(1, metricas.getTotalIntervencionesAuditorRf10());
        assertEquals(1, metricas.getParticipacionesDetalle().size());
        assertEquals("Estudiante 1", metricas.getParticipacionesDetalle().get(0).getEstudianteNombre());
        assertEquals("", metricas.getResumenIa());

        // 2. Al generarse la síntesis bajo demanda con IA
        String sintesis = analyticsService.generarYGuardarSintesisIa(sesionDemo);
        assertEquals("Diagnóstico: El grupo muestra curiosidad.", sintesis);

        // 3. Ahora en consultas posteriores al dashboard, se recupera directamente de la memoria sin invocar a Gemini
        AnalyticsService.MetricasDashboard metricasEnCache = analyticsService.obtenerMetricasPorSesion(sesionDemo);
        assertEquals("Diagnóstico: El grupo muestra curiosidad.", metricasEnCache.getResumenIa());
    }

    @Test
    @DisplayName("RF-10 no se infiere de palabras del diagnóstico; sí de los mensajes del estudiante. M-01 cuenta diagnósticos")
    void testAtajoSoloDesdeMensajesNoDesdeDiagnostico() {
        Estudiante estudiante = Estudiante.builder()
                .id(UUID.randomUUID())
                .nombre("Estudiante Mejia")
                .correo("mejia@javerianacali.edu.co")
                .build();

        Participacion part = Participacion.builder()
                .id(UUID.randomUUID())
                .sesion(sesionDemo)
                .estudiante(estudiante)
                .createdAt(OffsetDateTime.now())
                .progresoMd("""
                        ## Conceptos dominados
                        Clases y objetos.
                        ## Errores recurrentes
                        Preferencia por el aprendizaje pasivo: El estudiante muestra una tendencia a evitar el esfuerzo cognitivo que requiere el método socrático, solicitando "la respuesta directa".
                        Bloqueo ante la abstracción.
                        """)
                .build();

        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(List.of(part));
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(List.of(part))).thenReturn(List.of());
        when(metricaRepository.findByParticipacionIn(List.of(part))).thenReturn(List.of()); // Sin metrica previa en BD

        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesionDemo);

        assertNotNull(metricas);
        assertEquals(0, metricas.getTotalSolicitudesDirectas(), "Las palabras del diagnóstico ya no generan alertas de atajo");
        assertEquals(1, metricas.getDiagnosticosGenerados());

        // Un mensaje real del estudiante pidiendo el código sí cuenta (respaldo para datos sin métrica codigo=10)
        Mensaje pedido = Mensaje.builder().emisor(true).contenido("pásame el código").participacion(part).build();
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(List.of(part))).thenReturn(List.of(pedido));
        assertEquals(1, analyticsService.obtenerMetricasPorSesion(sesionDemo).getTotalSolicitudesDirectas());
    }

    @Test
    @DisplayName("M-02: 5 preguntas guía dan un margen de 15 turnos; dentro del margen nunca es 'Requiere apoyo'")
    void testClasificacionTurnosConMargenPorPreguntaGuia() {
        // Dentro del margen (15), aunque haya señales de apoyo
        assertTrue(AnalyticsService.clasificarTurnos(15, 5, true).etiqueta().contains("Ritmo normal"));
        assertTrue(AnalyticsService.clasificarTurnos(7, 5, true).etiqueta().contains("Fluido"));
        assertEquals("15 de 15 turnos · Ritmo normal", AnalyticsService.clasificarTurnos(15, 5, false).etiqueta());

        // Fuera del margen: solo rojo con señales; si no, diálogo perseverante
        assertTrue(AnalyticsService.clasificarTurnos(16, 5, true).etiqueta().contains("Requiere apoyo"));
        assertTrue(AnalyticsService.clasificarTurnos(16, 5, false).etiqueta().contains("Diálogo perseverante"));

        // Sin preguntas guía se asume un margen de 9 turnos (3 hitos)
        assertTrue(AnalyticsService.clasificarTurnos(9, 0, true).etiqueta().contains("Ritmo normal"));
        assertEquals("Sin consultas", AnalyticsService.clasificarTurnos(0, 5, false).etiqueta());
    }

    @ParameterizedTest
    @DisplayName("esSolicitudAtajo reconoce solicitudes de código/solución directa (RF-10)")
    @ValueSource(strings = {
            "dime la respuesta",
            "cuál es el código",
            "decime la solución",
            "puedes darme el código",
            "quiero la respuesta directa",
            "dame el código",
            "dame la respuesta",
            "hazme el ejercicio",
            "cuál es la respuesta",
            "resuélvelo tú",
            "me puedes dar el código?",
            "Dame directamente la respuesta",
            "quiero que me des la solución",
            "pásame el código",
            "escríbeme el código porfa",
            "muéstrame el código completo",
            "dime cómo se hace",
            "dámelo hecho",
            "escribe un programa que calcule el área",
            "no quiero preguntas, dame el for",
            "¿PODRÍAS ESCRIBIR LA SOLUCIÓN?"
    })
    void testDeteccionAtajo(String mensaje) {
        assertTrue(ChatService.esSolicitudAtajo(mensaje), mensaje);
    }

    @ParameterizedTest
    @DisplayName("esSolicitudAtajo no marca preguntas conceptuales ni código propio del estudiante")
    @ValueSource(strings = {
            "¿Cuál es la diferencia conceptual entre clase abstracta e interfaz?",
            "mi código da error en la respuesta del método",
            "no sé cómo hacer el ejercicio, ¿por dónde empiezo?",
            "el código de la tarea no compila",
            "te muestro mi código: class Perro extends Animal {}",
            "necesito entender la herencia",
            "dime qué es un objeto",
            "¿qué hace el método toString?",
            "ya entendí, gracias"
    })
    void testSinFalsosPositivos(String mensaje) {
        assertFalse(ChatService.esSolicitudAtajo(mensaje), mensaje);
    }
}
