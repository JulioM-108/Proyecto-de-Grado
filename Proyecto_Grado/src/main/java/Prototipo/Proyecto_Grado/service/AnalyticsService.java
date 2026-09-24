package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de analítica docente y síntesis pedagógica con IA.
 * Consolida métricas cuantitativas reales y genera diagnósticos ejecutivos con Gemini.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final SesionRepository sesionRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
    private final GeminiService geminiService;

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Data
    @Builder
    public static class MetricasDashboard {
        private long totalSesiones;
        private long totalParticipaciones;
        private long totalMensajes;
        private long totalConsultasEstudiante;
        private long totalRespuestasTutor;
        /** Cantidad de reformulaciones RF-10 detectadas para prevenir entrega directa de código */
        private long totalIntervencionesAuditorRf10;
        /** Mapa concepto POO → cantidad de errores/dudas acumuladas */
        private Map<String, Long> dificultadesPorConcepto;
        /** Resumen ejecutivo y recomendaciones pedagógicas generadas con Gemini */
        private String resumenIa;
        /** Listado detallado de estudiantes y avances */
        private List<ParticipacionDetalleDto> participacionesDetalle;
    }

    @Data
    @Builder
    public static class ParticipacionDetalleDto {
        private UUID id;
        private String estudianteNombre;
        private String estudianteCorreo;
        private String fechaFormateada;
        private long totalMensajes;
        private long consultasEstudiante;
        private String progresoMd;
    }

    /**
     * Resumen global de métricas del docente en toda la plataforma.
     */
    @Transactional(readOnly = true)
    public MetricasDashboard obtenerMetricasDocente() {
        long totalSesiones        = sesionRepository.count();
        long totalParticipaciones = participacionRepository.count();
        long totalMensajes        = mensajeRepository.count();
        long totalReformulaciones = metricaRepository.findByCodigo(10L).size();

        List<Metrica> todasLasMetricas = metricaRepository.findAll();
        Map<String, Long> dificultades = todasLasMetricas.stream()
                .filter(m -> m.getConcepto() != null && !"RF-10".equalsIgnoreCase(m.getConcepto()) && m.getValorNumerico() != null)
                .collect(Collectors.groupingBy(
                        Metrica::getConcepto,
                        LinkedHashMap::new,
                        Collectors.summingLong(Metrica::getValorNumerico)
                ));

        return MetricasDashboard.builder()
                .totalSesiones(totalSesiones)
                .totalParticipaciones(totalParticipaciones)
                .totalMensajes(totalMensajes)
                .totalConsultasEstudiante(0)
                .totalRespuestasTutor(0)
                .totalIntervencionesAuditorRf10(totalReformulaciones)
                .dificultadesPorConcepto(dificultades)
                .resumenIa(null)
                .participacionesDetalle(List.of())
                .build();
    }

    /**
     * Métricas detalladas e interpretadas con IA para una sesión específica.
     */
    @Transactional(readOnly = true)
    public MetricasDashboard obtenerMetricasPorSesion(Sesion sesion) {
        List<Participacion> participaciones = participacionRepository.findBySesion(sesion);

        long totalConsultasEstudiantes = 0;
        long totalRespuestasTutor = 0;
        long totalRf10 = 0;
        Map<String, Long> dificultades = new LinkedHashMap<>();
        List<ParticipacionDetalleDto> detalleList = new ArrayList<>();
        List<String> fragmentosConsultas = new ArrayList<>();

        for (Participacion p : participaciones) {
            List<Mensaje> msgs = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(p);
            long cEstudiante = msgs.stream().filter(m -> Boolean.TRUE.equals(m.getEmisor())).count();
            long rTutor = msgs.stream().filter(m -> Boolean.FALSE.equals(m.getEmisor())).count();

            totalConsultasEstudiantes += cEstudiante;
            totalRespuestasTutor += rTutor;

            // Consultar métricas de esta participación
            List<Metrica> metricasPart = metricaRepository.findByParticipacion(p);
            long rf10Part = metricasPart.stream()
                    .filter(m -> "RF-10".equalsIgnoreCase(m.getConcepto()) || Long.valueOf(10L).equals(m.getCodigo()))
                    .count();
            totalRf10 += rf10Part;

            for (Metrica m : metricasPart) {
                if (m.getConcepto() != null && !"RF-10".equalsIgnoreCase(m.getConcepto())) {
                    long val = m.getValorNumerico() != null ? m.getValorNumerico() : 1L;
                    dificultades.merge(m.getConcepto(), val, Long::sum);
                }
            }

            // Guardar fragmentos de preguntas para la IA
            msgs.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getEmisor()) && m.getContenido() != null)
                    .map(m -> m.getContenido().trim())
                    .limit(3)
                    .forEach(fragmentosConsultas::add);

            String fechaStr = p.getCreatedAt() != null ? p.getCreatedAt().format(FECHA_FORMATTER) : "Reciente";
            String nombreEst = (p.getEstudiante() != null && p.getEstudiante().getNombre() != null)
                    ? p.getEstudiante().getNombre() : "Estudiante";
            String correoEst = (p.getEstudiante() != null && p.getEstudiante().getCorreo() != null)
                    ? p.getEstudiante().getCorreo() : "estudiante@javeriana.edu.co";

            detalleList.add(ParticipacionDetalleDto.builder()
                    .id(p.getId())
                    .estudianteNombre(nombreEst)
                    .estudianteCorreo(correoEst)
                    .fechaFormateada(fechaStr)
                    .totalMensajes(msgs.size())
                    .consultasEstudiante(cEstudiante)
                    .progresoMd(p.getProgresoMd() != null ? p.getProgresoMd() : "")
                    .build());
        }

        long totalMensajes = totalConsultasEstudiantes + totalRespuestasTutor;

        // Si no hay métricas explícitas pero hay tema, añadimos el tema principal
        if (dificultades.isEmpty() && totalConsultasEstudiantes > 0) {
            dificultades.put(sesion.getTema(), totalConsultasEstudiantes);
        }

        // Generar resumen ejecutivo con IA (Gemini)
        String resumenIa = generarSintesisIa(sesion, participaciones.size(), totalConsultasEstudiantes,
                totalRespuestasTutor, totalRf10, fragmentosConsultas);

        return MetricasDashboard.builder()
                .totalSesiones(1)
                .totalParticipaciones(participaciones.size())
                .totalMensajes(totalMensajes)
                .totalConsultasEstudiante(totalConsultasEstudiantes)
                .totalRespuestasTutor(totalRespuestasTutor)
                .totalIntervencionesAuditorRf10(totalRf10)
                .dificultadesPorConcepto(dificultades)
                .resumenIa(resumenIa)
                .participacionesDetalle(detalleList)
                .build();
    }

    /**
     * Utiliza Gemini para sintetizar pedagógicamente las interacciones de los estudiantes.
     */
    private String generarSintesisIa(Sesion sesion, int cantEstudiantes, long consultas,
                                     long respuestas, long rf10, List<String> fragmentosConsultas) {
        if (consultas == 0) {
            return "Aún no se registran interacciones en esta sesión de clase. "
                    + "Comparte el código privado con tus estudiantes para que dialoguen con el tutor socrático. "
                    + "Tan pronto como envíen sus consultas, la Inteligencia Artificial generará aquí un análisis pedagógico consolidado.";
        }

        String prompt = """
            Eres un Asesor Pedagógico y Analista Educativo en Programación Orientada a Objetos (POO).
            Analiza las siguientes métricas de la clase de POO titulada "%s":
            - Directrices CRAFT configuradas por el profesor: Rol="%s", Enfoque="%s".
            - Estudiantes participantes: %d
            - Preguntas enviadas por los estudiantes: %d
            - Orientaciones socráticas dadas por el tutor: %d
            - Intervenciones del Auditor RF-10 (intentos de pedir código directo prevenidos): %d
            - Muestra de consultas realizadas por los alumnos: %s

            Genera un informe pedagógico ejecutivo conciso (máximo 3 párrafos en Markdown, con tono profesional y constructivo para el profesor):
            1. **Diagnóstico Grupal**: Nivel de interacción y madurez conceptual observada frente al tema.
            2. **Focos de Confusión o Dificultad**: Conceptos donde los estudiantes requirieron mayor guía o mostraron dudas.
            3. **Recomendaciones para el Aula Presencial**: 2 o 3 sugerencias didácticas puntuales para reforzar en la próxima clase.
            """.formatted(
                sesion.getTema(),
                sesion.getCraftRol() != null ? sesion.getCraftRol() : "Tutor Socrático",
                sesion.getCraftContexto() != null ? sesion.getCraftContexto() : sesion.getTema(),
                cantEstudiantes,
                consultas,
                respuestas,
                rf10,
                String.join(" | ", fragmentosConsultas.stream().limit(5).toList())
        );

        try {
            String respuesta = geminiService.generarRespuesta(prompt);
            if (respuesta != null && !respuesta.isBlank() && !respuesta.startsWith("[Modo Mock")) {
                return respuesta.trim();
            }
        } catch (Exception e) {
            log.warn("No se pudo generar la síntesis con IA, generando reporte algorítmico: {}", e.getMessage());
        }

        // Fallback pedagógico estructurado
        return """
            **Diagnóstico Grupal**: La sesión sobre **%s** cuenta con **%d** estudiantes activos y un total de **%d** consultas registradas. La dinámica socrática ha permitido guiar el razonamiento autónomo de los alumnos sin suministrar código de manera pasiva.
            
            **Auditoría Pedagógica**: Se registraron **%d** activaciones del Auditor RF-10, garantizando que el tutor respondiera con preguntas orientadoras y analogías conceptuales en lugar de soluciones directas.
            
            **Recomendación Docente**: Se sugiere iniciar la siguiente clase presencial repasando la definición formal y el diseño de clases relacionado con *%s*, planteando un ejercicio colaborativo en el tablero.
            """.formatted(sesion.getTema(), cantEstudiantes, consultas, rf10, sesion.getTema());
    }
}
