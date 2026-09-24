package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.*;
import Prototipo.Proyecto_Grado.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private SesionRepository sesionRepository;

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

        when(mensajeRepository.findByParticipacionOrderByCreatedAtAsc(part))
                .thenReturn(List.of(msgEstudiante, msgTutor));

        Metrica rf10 = Metrica.builder()
                .codigo(10L)
                .concepto("RF-10")
                .valorNumerico(1L)
                .participacion(part)
                .build();

        when(metricaRepository.findByParticipacion(part)).thenReturn(List.of(rf10));
        when(geminiService.generarRespuesta(any())).thenReturn("Diagnóstico: El grupo muestra curiosidad.");

        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesionDemo);

        assertNotNull(metricas);
        assertEquals(1, metricas.getTotalParticipaciones());
        assertEquals(2, metricas.getTotalMensajes());
        assertEquals(1, metricas.getTotalConsultasEstudiante());
        assertEquals(1, metricas.getTotalRespuestasTutor());
        assertEquals(1, metricas.getTotalIntervencionesAuditorRf10());
        assertEquals(1, metricas.getParticipacionesDetalle().size());
        assertEquals("Estudiante 1", metricas.getParticipacionesDetalle().get(0).getEstudianteNombre());
        assertEquals("Diagnóstico: El grupo muestra curiosidad.", metricas.getResumenIa());
    }
}
