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
import java.util.Set;
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

    @Mock
    private SesionRepository sesionRepository;

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
        assertEquals("", metricas.getResumenIa(), "Sin interacciones no se muestra ningún texto como si fuera análisis");
        assertTrue(metricas.getParticipacionesDetalle().isEmpty());
        // Sin interacciones no se puede generar la síntesis (ni se inventa una)
        assertThrows(IllegalStateException.class, () -> analyticsService.generarYGuardarSintesisIa(sesionDemo));
    }

    @Test
    @DisplayName("Si la IA falla, la síntesis grupal NO devuelve un texto prefabricado ni se guarda en la sesión")
    void testSintesisSinTextoPrefabricadoCuandoFallaIa() {
        Estudiante e = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo("ana@javerianacali.edu.co").build();
        Participacion p = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e)
                .createdAt(OffsetDateTime.now()).progresoMd("").build();
        Mensaje m = Mensaje.builder().emisor(true).contenido("¿Qué es una interfaz?").participacion(p).build();
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(List.of(p));
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(List.of(p))).thenReturn(List.of(m));
        when(metricaRepository.findByParticipacionIn(List.of(p))).thenReturn(List.of());
        when(geminiService.generarRespuesta(any())).thenReturn(GeminiService.MENSAJE_ERROR_TEMPORAL);

        assertThrows(IllegalStateException.class, () -> analyticsService.generarYGuardarSintesisIa(sesionDemo));
        assertEquals("", analyticsService.obtenerMetricasPorSesion(sesionDemo).getResumenIa(), "No debe quedar nada guardado");
    }

    @Test
    @DisplayName("La síntesis grupal usa el informe de quien lo tiene y los mensajes de quien no, anonimizados")
    void testSintesisUsaInformesYMensajes() {
        Estudiante e1 = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana Pérez").correo("ana@javerianacali.edu.co").build();
        Estudiante e2 = Estudiante.builder().id(UUID.randomUUID()).nombre("Luis Gómez").correo("luis@javerianacali.edu.co").build();
        Participacion conInforme = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e1)
                .createdAt(OffsetDateTime.now()).progresoMd("## Errores recurrentes\n- **INFORME_ANA:** x").build();
        Participacion sinInforme = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e2)
                .createdAt(OffsetDateTime.now()).progresoMd("").build();
        List<Participacion> parts = List.of(conInforme, sinInforme);
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(parts);
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(parts)).thenReturn(List.of(
                Mensaje.builder().emisor(true).contenido("hola tutor").participacion(conInforme).build(),
                Mensaje.builder().emisor(true).contenido("MENSAJE_LUIS sobre herencia").participacion(sinInforme).build()));
        when(metricaRepository.findByParticipacionIn(parts)).thenReturn(List.of());
        when(geminiService.generarRespuesta(any())).thenReturn("### Diagnóstico Grupal\nok");

        analyticsService.generarYGuardarSintesisIa(sesionDemo);

        org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(geminiService).generarRespuesta(prompt.capture());
        assertTrue(prompt.getValue().contains("INFORME_ANA"));
        assertTrue(prompt.getValue().contains("MENSAJE_LUIS"));
        assertFalse(prompt.getValue().contains("Ana Pérez"), "Los estudiantes se anonimizan");
    }

    private static Metrica hito(String concepto, long turnos) {
        return Metrica.builder().codigo(ChatService.CODIGO_TURNOS_POR_GUIA).concepto(concepto).valorNumerico(turnos).build();
    }

    @Test
    @DisplayName("M-02: avance por pregunta guía y estados (avanzando, se trabó, requiere apoyo, ruta completada)")
    void testAvancePorPreguntaGuia() {
        // P1 2 turnos, P2 9 turnos (se trabó), va en P3 con 1 turno
        AnalyticsService.AvanceGuias avance = AnalyticsService.calcularAvance(
                List.of(hito("P1", 2), hito("P2", 9)), 12, 8, false);
        assertEquals(3, avance.preguntaEnCurso());
        assertEquals(1, avance.turnosEnCurso());
        assertEquals("Se trabó en P2 (9 turnos)", AnalyticsService.clasificarAvance(avance, 12, 8, false, false).etiqueta());
        assertEquals("Requiere apoyo · P2", AnalyticsService.clasificarAvance(avance, 12, 8, false, true).etiqueta());

        AnalyticsService.AvanceGuias fluido = AnalyticsService.calcularAvance(List.of(hito("P1", 2)), 4, 8, false);
        assertEquals("Avanzando · va en P2 de 8", AnalyticsService.clasificarAvance(fluido, 4, 8, false, true).etiqueta());

        AnalyticsService.AvanceGuias completa = AnalyticsService.calcularAvance(List.of(hito("P1", 3), hito("P2-P8", 15)), 18, 8, true);
        assertEquals("Ruta completada en 18 turnos", AnalyticsService.clasificarAvance(completa, 18, 8, true, false).etiqueta());
        assertEquals(0, completa.preguntaEnCurso());

        // Insignias: tramo agrupado neutro y pregunta en curso marcada "en curso"
        List<AnalyticsService.TramoGuia> vista = AnalyticsService.tramosParaVista(avance);
        assertEquals("P1 · 2", vista.get(0).getEtiqueta());
        assertEquals("P3 · 1 en curso", vista.get(2).getEtiqueta());
        assertEquals("P2-P8 · 15", AnalyticsService.tramosParaVista(completa).get(1).getEtiqueta());

        assertEquals("Sin consultas", AnalyticsService.clasificarAvance(AnalyticsService.calcularAvance(List.of(), 0, 8, false), 0, 8, false, false).etiqueta());
    }

    @Test
    @DisplayName("M-02: un tramo agrupado con más de 3 turnos por pregunta se muestra como 'esfuerzo en Pa-Pb'")
    void testTramoAgrupadoConEsfuerzo() {
        // Caso real: el tutor no marcó P3 y el estudiante se confundió ahí (P3-P4 con 7 turnos > 2 x 3)
        List<Metrica> conEsfuerzo = List.of(hito("P1", 1), hito("P2", 1), hito("P3-P4", 7), hito("P5", 1));
        AnalyticsService.AvanceGuias completa = AnalyticsService.calcularAvance(conEsfuerzo, 10, 5, true);
        assertEquals("Ruta completada · esfuerzo en P3-P4", AnalyticsService.clasificarAvance(completa, 10, 5, true, false).etiqueta());
        assertTrue(AnalyticsService.tramosParaVista(completa).get(2).getClase().contains("warning"));

        AnalyticsService.AvanceGuias enCurso = AnalyticsService.calcularAvance(
                List.of(hito("P1", 1), hito("P2-P3", 8)), 9, 5, false);
        assertEquals("Esfuerzo en P2-P3 (8 turnos)", AnalyticsService.clasificarAvance(enCurso, 9, 5, false, false).etiqueta());
        assertEquals("Requiere apoyo · P2-P3", AnalyticsService.clasificarAvance(enCurso, 9, 5, false, true).etiqueta());

        // Agrupado dentro de lo esperado (6 turnos en 2 preguntas): no es señal
        AnalyticsService.AvanceGuias normal = AnalyticsService.calcularAvance(
                List.of(hito("P1", 1), hito("P2-P3", 6), hito("P4", 1), hito("P5", 1)), 9, 5, true);
        assertEquals("Ruta completada en 9 turnos", AnalyticsService.clasificarAvance(normal, 9, 5, true, false).etiqueta());
    }

    @Test
    @DisplayName("M-02 grupal: promedio por pregunta guía solo con tramos individuales y marca de la más difícil")
    void testPromediosPorGuia() {
        List<AnalyticsService.AvanceGuias> avances = List.of(
                AnalyticsService.calcularAvance(List.of(hito("P1", 2), hito("P2", 8)), 10, 3, false),
                AnalyticsService.calcularAvance(List.of(hito("P1", 4), hito("P2-P3", 6)), 10, 3, true));
        List<AnalyticsService.PromedioGuia> promedios = AnalyticsService.promediosPorGuia(avances, 3);
        assertEquals(3, promedios.size());
        assertEquals(3.0, promedios.get(0).getPromedio());
        assertEquals(2, promedios.get(0).getEstudiantes());
        assertEquals(8.0, promedios.get(1).getPromedio(), "El tramo agrupado P2-P3 no entra al promedio de P2");
        assertTrue(promedios.get(1).isMasDificil());
        assertNull(promedios.get(2).getPromedio());
    }

    @Test
    @DisplayName("M-02 grupal: 'la que más le cuesta' solo se marca si la pregunta más costosa supera los 3 turnos")
    void testMasDificilSoloSiNoEsFluida() {
        List<AnalyticsService.AvanceGuias> fluidos = List.of(
                AnalyticsService.calcularAvance(List.of(hito("P1", 3), hito("P2", 1)), 4, 2, true),
                AnalyticsService.calcularAvance(List.of(hito("P1", 2), hito("P2", 1)), 3, 2, true));
        assertFalse(AnalyticsService.promediosPorGuia(fluidos, 2).stream().anyMatch(AnalyticsService.PromedioGuia::isMasDificil),
                "P1 con 2,5 turnos es fluida: no se marca");
    }

    @Test
    @DisplayName("M-02: la apertura (código 20) se muestra aparte y no cuenta en la pregunta en curso")
    void testAperturaSeparadaDeP1() {
        Metrica apertura = Metrica.builder().codigo(ChatService.CODIGO_APERTURA).concepto("APERTURA").valorNumerico(2L).build();
        AnalyticsService.AvanceGuias avance = AnalyticsService.calcularAvance(List.of(apertura), 3, 5, false);
        assertEquals(1, avance.preguntaEnCurso());
        assertEquals(1, avance.turnosEnCurso(), "3 consultas - 2 de apertura = 1 turno en P1");
        List<AnalyticsService.TramoGuia> vista = AnalyticsService.tramosParaVista(avance);
        assertEquals("Apertura · 2", vista.get(0).getEtiqueta());
        assertEquals("P1 · 1 en curso", vista.get(1).getEtiqueta());
    }

    @Test
    @DisplayName("M-01: títulos equivalentes (mayúsculas, tildes, puntuación) cuentan como el mismo error")
    void testErroresEquivalentesSeAgrupan() {
        Estudiante e1 = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo("ana@javerianacali.edu.co").build();
        Estudiante e2 = Estudiante.builder().id(UUID.randomUUID()).nombre("Luis").correo("luis@javerianacali.edu.co").build();
        Participacion p1 = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e1).createdAt(OffsetDateTime.now())
                .progresoMd("## Errores recurrentes\n- **Composición vs. agregación:** a\n## Recomendación para la siguiente sesión\n- x").build();
        Participacion p2 = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e2).createdAt(OffsetDateTime.now())
                .progresoMd("## Errores recurrentes\n- **composicion vs agregacion:** b\n## Recomendación para la siguiente sesión\n- y").build();
        List<Participacion> parts = List.of(p1, p2);
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(parts);
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(parts)).thenReturn(List.of());
        when(metricaRepository.findByParticipacionIn(parts)).thenReturn(List.of());

        List<AnalyticsService.FocoDificultad> focos = analyticsService.obtenerMetricasPorSesion(sesionDemo).getDificultadesPorConcepto();
        assertEquals(1, focos.size());
        assertEquals("Composición vs. agregación", focos.get(0).getError());
        assertEquals(2, focos.get(0).getEstudiantes());
        assertEquals(100, focos.get(0).getPorcentaje());
    }

    @Test
    @DisplayName("M-01: las viñetas que dicen que no hubo errores no se cuentan como errores")
    void testErroresIgnoraNegaciones() {
        String informe = """
                ## Errores recurrentes
                * No se observaron errores conceptuales durante la sesión. El estudiante avanzó bien.
                * En este historial **no se observan errores recurrentes**; respondió con precisión.
                * Se detectó una **limitación inicial** (no un error): no conocía los nombres formales.
                * Ninguno observado.
                * **Ninguno**
                * **Ninguno detectado:** el estudiante respondió con precisión en toda la sesión.
                * **Nota:** el estudiante escribió en inglés al inicio.
                * **Nota de precaución:** las respuestas fueron breves.
                * **Sesgo hacia la Composición:** prefiere siempre la composición.
                ## Recomendación para la siguiente sesión
                - x
                """;
        assertEquals(Set.of("Sesgo hacia la Composición"), AnalyticsService.extraerErroresRecurrentes(informe));
        assertFalse(AnalyticsService.niegaErrores("Confunde el manejo de errores con excepciones"));
    }

    @Test
    void testErroresConNegacionEnLaExplicacionSeConservan() {
        String informe = """
                ## Errores recurrentes
                * **Manejo de excepciones:** no captura los errores de entrada del usuario.
                * **Validación de datos:** sin controlar los errores de formato.
                * **Herencia:** no se observaron errores en este tema.
                ## Recomendación para la siguiente sesión
                - x
                """;
        assertEquals(Set.of("Manejo de excepciones", "Validación de datos"), AnalyticsService.extraerErroresRecurrentes(informe));
    }

    @Test
    @DisplayName("M-03: tasa de solicitudes directas y 'Posible frustración' con ≥2 solicitudes y ≥20 %")
    void testSolicitudesDirectas() {
        assertEquals(60, AnalyticsService.tasaSolicitudes(3, 5));
        assertTrue(AnalyticsService.posibleFrustracion(3, 5));
        assertFalse(AnalyticsService.posibleFrustracion(3, 40), "3 de 40 es puntual");
        assertFalse(AnalyticsService.posibleFrustracion(1, 2), "Una sola solicitud no basta");
        assertEquals("3 · 60 % · Posible frustración", AnalyticsService.clasificarSolicitudes(3, 5).etiqueta());
        assertEquals("3 · 8 % · Puntual", AnalyticsService.clasificarSolicitudes(3, 40).etiqueta());
        assertEquals("Sin solicitudes", AnalyticsService.clasificarSolicitudes(0, 10).etiqueta());
    }

    @Test
    @DisplayName("M-04: el tiempo activo no cuenta silencios de más de 10 minutos")
    void testTiempoActivo() {
        OffsetDateTime t0 = OffsetDateTime.parse("2026-10-18T10:00:00-05:00");
        List<OffsetDateTime> horas = List.of(t0, t0.plusMinutes(3), t0.plusMinutes(7), t0.plusHours(4), t0.plusHours(4).plusMinutes(5));
        assertEquals(12, AnalyticsService.minutosActivos(horas), "3 + 4 + 5 min; el almuerzo de 4 h no cuenta");
        assertEquals(0, AnalyticsService.minutosActivos(List.of(t0)));
        assertEquals("< 1 min", AnalyticsService.formatearMinutos(0));
        assertEquals("18 min", AnalyticsService.formatearMinutos(18));
        assertEquals("1 h 05 min", AnalyticsService.formatearMinutos(65));
    }

    @Test
    @DisplayName("Informes pendientes: cuenta a quienes escribieron y no tienen informe")
    void testInformesPendientes() {
        Estudiante e1 = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo("ana@javerianacali.edu.co").build();
        Estudiante e2 = Estudiante.builder().id(UUID.randomUUID()).nombre("Luis").correo("luis@javerianacali.edu.co").build();
        Participacion escribioSinInforme = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e1)
                .createdAt(OffsetDateTime.now()).progresoMd("").build();
        Participacion sinEscribir = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e2)
                .createdAt(OffsetDateTime.now()).progresoMd("").build();
        List<Participacion> parts = List.of(escribioSinInforme, sinEscribir);
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(parts);
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(parts)).thenReturn(List.of(
                Mensaje.builder().emisor(true).contenido("hola").participacion(escribioSinInforme).build()));
        when(metricaRepository.findByParticipacionIn(parts)).thenReturn(List.of());

        assertEquals(1, analyticsService.obtenerMetricasPorSesion(sesionDemo).getInformesPendientes());
    }

    @Test
    @DisplayName("Obtener métricas con interacciones, solicitudes directas (M-03) y síntesis guardada en la sesión (RF-07)")
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
                .concepto("RF-10") // concepto de los registros antiguos: se sigue leyendo
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

        // 3. Se guarda en Supabase (sobrevive a reinicios) y las consultas posteriores la leen de la sesión sin invocar a Gemini
        AnalyticsService.MetricasDashboard metricasEnCache = analyticsService.obtenerMetricasPorSesion(sesionDemo);
        assertEquals("Diagnóstico: El grupo muestra curiosidad.", metricasEnCache.getResumenIa());
    }

    @Test
    @DisplayName("M-03 no se infiere de palabras del diagnóstico; sí de los mensajes del estudiante. M-01 cuenta diagnósticos")
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
    @DisplayName("M-01: porcentaje de estudiantes con informe que presentan cada error, contando una vez por estudiante")
    void testFocosDificultadEnPorcentaje() {
        Estudiante e1 = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo("ana@javerianacali.edu.co").build();
        Estudiante e2 = Estudiante.builder().id(UUID.randomUUID()).nombre("Luis").correo("luis@javerianacali.edu.co").build();
        Estudiante e3 = Estudiante.builder().id(UUID.randomUUID()).nombre("Sin informe").correo("sin@javerianacali.edu.co").build();

        // Ana repite el mismo error en dos viñetas: debe contar una sola vez
        Participacion p1 = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e1)
                .createdAt(OffsetDateTime.now())
                .progresoMd("## Errores recurrentes\n- **Confusión clase/objeto:** a\n- **Confusión clase/objeto:** b\n- **Interferencia de otros lenguajes:** c\n## Recomendación para la siguiente sesión\n- x")
                .build();
        Participacion p2 = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e2)
                .createdAt(OffsetDateTime.now())
                .progresoMd("## Errores recurrentes\n- **Confusión clase/objeto:** d")
                .build();
        Participacion p3 = Participacion.builder().id(UUID.randomUUID()).sesion(sesionDemo).estudiante(e3)
                .createdAt(OffsetDateTime.now()).progresoMd("").build();

        List<Participacion> parts = List.of(p1, p2, p3);
        when(participacionRepository.findBySesion(sesionDemo)).thenReturn(parts);
        when(mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(parts)).thenReturn(List.of());
        when(metricaRepository.findByParticipacionIn(parts)).thenReturn(List.of());

        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesionDemo);

        assertEquals(2, metricas.getDiagnosticosGenerados(), "La base son los 2 estudiantes con informe, no los 3 de la clase");
        List<AnalyticsService.FocoDificultad> focos = metricas.getDificultadesPorConcepto();
        assertEquals(2, focos.size());
        // Ordenado de mayor a menor
        assertEquals("Confusión clase/objeto", focos.get(0).getError());
        assertEquals(2, focos.get(0).getEstudiantes());
        assertEquals(100, focos.get(0).getPorcentaje());
        assertEquals("Interferencia de otros lenguajes", focos.get(1).getError());
        assertEquals(1, focos.get(1).getEstudiantes());
        assertEquals(50, focos.get(1).getPorcentaje());
    }

    @Test
    @DisplayName("M-02 (sin marcas por pregunta): 5 preguntas guía dan un margen de 15 turnos; dentro del margen nunca es 'Requiere apoyo'")
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
    @DisplayName("esSolicitudAtajo reconoce solicitudes de código/solución directa (M-03)")
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
