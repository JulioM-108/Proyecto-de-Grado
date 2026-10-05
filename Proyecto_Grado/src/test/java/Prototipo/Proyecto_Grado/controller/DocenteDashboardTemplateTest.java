package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.context.WebContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DocenteDashboardTemplateTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    @DisplayName("El template docente/dashboard.html se parsea y procesa como WebContext sin TemplateInputException")
    void testProcesarTemplateDashboardSinErrores() {
        Profesor profesor = Profesor.builder()
                .id(UUID.randomUUID())
                .nombre("Profesor Julio Mazo")
                .correo("julio.mazo@javerianacali.edu.co")
                .facultad("Facultad de Ingeniería")
                .build();

        Sesion sesion = Sesion.builder()
                .id(UUID.randomUUID())
                .tema("Polimorfismo e Interfaces en POO")
                .enlaceUnico("K9X2P7")
                .profesor(profesor)
                .craftContexto("Contexto educativo de POO")
                .craftRol("Tutor socrático")
                .craftAudiencia("Estudiantes Javerianos")
                .craftFormato("Preguntas y analogías")
                .craftTono("Amable y motivador")
                .estado(true)
                .build();

        AnalyticsService.MetricasDashboard metricas = AnalyticsService.MetricasDashboard.builder()
                .totalSesiones(1)
                .totalParticipaciones(3)
                .estudiantesActivos(2)
                .totalMensajes(12)
                .totalConsultasEstudiante(6)
                .totalRespuestasTutor(6)
                .totalIntervencionesAuditorRf10(1)
                .diagnosticosGenerados(2)
                .dificultadesPorConcepto(List.of(
                        new AnalyticsService.FocoDificultad("Polimorfismo", 2, 100),
                        new AnalyticsService.FocoDificultad("Interfaces", 1, 50)))
                .resumenIa("Diagnóstico generado con Gemini: El grupo presenta dudas en ligadura dinámica.")
                .participacionesDetalle(List.of(
                        AnalyticsService.ParticipacionDetalleDto.builder()
                                .id(UUID.randomUUID())
                                .estudianteNombre("Estudiante Prueba")
                                .estudianteCorreo("estudiante.prueba@javerianacali.edu.co")
                                .fechaFormateada("15/09/2026 14:00")
                                .totalMensajes(4)
                                .consultasEstudiante(2)
                                .progresoMd("Avanzó en la comprensión de métodos abstractos.")
                                .build()
                ))
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(request.getServletContext());
        IWebExchange exchange = app.buildExchange(request, response);

        WebContext context = new WebContext(exchange);
        context.setVariable("profesor", profesor);
        context.setVariable("sesion", sesion);
        context.setVariable("metricas", metricas);
        context.setVariable("craftGuardado", false);
        context.setVariable("estudianteVinculado", false);
        context.setVariable("estudianteDesvinculado", false);

        String htmlProcesado = templateEngine.process("docente/dashboard", context);

        assertNotNull(htmlProcesado);
        assertTrue(htmlProcesado.contains("Polimorfismo e Interfaces en POO"));
        assertTrue(htmlProcesado.contains("K9X2P7"));
        assertTrue(htmlProcesado.contains("Estudiante Prueba"));
        assertTrue(htmlProcesado.contains("Diagnóstico generado con Gemini"));
        assertTrue(htmlProcesado.contains("Promedio Turnos"));
        assertTrue(htmlProcesado.contains("Alertas de Atajo"));
        assertTrue(htmlProcesado.contains("Seguimiento Individual de Estudiantes"));
        assertTrue(htmlProcesado.contains("modalDiagnosticoCognitivo"));
        assertTrue(htmlProcesado.contains("modalDesvincularEstudiante"));
        // M-01: porcentaje sobre los estudiantes con informe y ancho de barra proporcional
        assertTrue(htmlProcesado.contains("100% · 2 de 2 estudiantes"));
        assertTrue(htmlProcesado.contains("50% · 1 de 2 estudiantes"));
        assertTrue(htmlProcesado.contains("width: 50%;"));
        // KPI de estudiantes: activos de inscritos; CRAFT vacío muestra el valor real que usa el tutor
        assertTrue(htmlProcesado.contains("Han escrito al tutor · de 3 inscritos"));
        assertTrue(htmlProcesado.contains("Contexto educativo de POO"));
        assertTrue(htmlProcesado.contains("Preguntas y analogías"));
        // Comprobar que la jerga técnica RF-10 ya no aparece en el HTML docente
        org.junit.jupiter.api.Assertions.assertFalse(htmlProcesado.contains("Auditor RF-10"));
    }
}
