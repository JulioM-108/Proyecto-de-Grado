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
        /** Promedio de turnos/consultas por estudiante (M-02) */
        private double promedioTurnosPorEstudiante;
        /** Conteo de intentos de solicitar código directo prevenidos (M-04) */
        private long totalSolicitudesDirectas;
        /** Compatibilidad hacia atrás */
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

        // M-02: Turnos y Nivel de Dificultad
        private String nivelDificultad;
        private String nivelDificultadClase;

        // M-03: Evolución Pedagógica
        private String estadoEvolucion;
        private String estadoEvolucionClase;

        // M-05: Actividad y Estado de Compromiso
        private String estadoActividad;
        private String estadoActividadColor;
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
                .promedioTurnosPorEstudiante(0.0)
                .totalSolicitudesDirectas(totalReformulaciones)
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

            // M-02: Turnos y Nivel de Dificultad
            String nivelDificultad;
            String nivelDificultadClase;
            if (cEstudiante == 0) {
                nivelDificultad = "Sin consultas";
                nivelDificultadClase = "bg-light text-muted border";
            } else if (cEstudiante <= 3) {
                nivelDificultad = cEstudiante + " turnos · Fluido";
                nivelDificultadClase = "bg-success-subtle text-success border border-success-subtle";
            } else if (cEstudiante <= 7) {
                nivelDificultad = cEstudiante + " turnos · Guiado";
                nivelDificultadClase = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
            } else {
                nivelDificultad = cEstudiante + " turnos · Requiere Apoyo";
                nivelDificultadClase = "bg-danger-subtle text-danger border border-danger-subtle";
            }

            // M-03: Evolución Pedagógica
            String estadoEvolucion;
            String estadoEvolucionClase;
            String pMd = (p.getProgresoMd() != null) ? p.getProgresoMd() : "";
            if (cEstudiante == 0) {
                estadoEvolucion = "Sin interacción";
                estadoEvolucionClase = "bg-light text-muted border";
            } else if (!pMd.isBlank() && pMd.contains("## Conceptos dominados")) {
                if (pMd.contains("## Errores recurrentes") && (pMd.toLowerCase().contains("persiste") || pMd.toLowerCase().contains("confunde") || cEstudiante >= 8)) {
                    estadoEvolucion = "Dudas en progreso";
                    estadoEvolucionClase = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
                } else {
                    estadoEvolucion = "Consolidado";
                    estadoEvolucionClase = "bg-success-subtle text-success border border-success-subtle";
                }
            } else {
                estadoEvolucion = "En diálogo activo";
                estadoEvolucionClase = "bg-primary-subtle text-primary border border-primary-subtle";
            }

            // M-05: Actividad y Estado de Compromiso
            String estadoActividad;
            String estadoActividadColor;
            java.time.OffsetDateTime ultimaFecha = msgs.isEmpty() ? p.getCreatedAt() : msgs.get(msgs.size() - 1).getCreatedAt();
            if (cEstudiante == 0) {
                estadoActividad = "Sin participación";
                estadoActividadColor = "text-muted";
            } else if (ultimaFecha != null) {
                long horas = java.time.Duration.between(ultimaFecha, java.time.OffsetDateTime.now()).toHours();
                if (horas < 24) {
                    estadoActividad = "Activo hoy";
                    estadoActividadColor = "text-success";
                } else {
                    long dias = horas / 24;
                    if (dias <= 3) {
                        estadoActividad = "Hace " + dias + (dias == 1 ? " día" : " días");
                        estadoActividadColor = "text-secondary";
                    } else {
                        estadoActividad = "Hace " + dias + " días (En riesgo)";
                        estadoActividadColor = "text-danger fw-semibold";
                    }
                }
            } else {
                estadoActividad = "Reciente";
                estadoActividadColor = "text-success";
            }

            detalleList.add(ParticipacionDetalleDto.builder()
                    .id(p.getId())
                    .estudianteNombre(nombreEst)
                    .estudianteCorreo(correoEst)
                    .fechaFormateada(fechaStr)
                    .totalMensajes(msgs.size())
                    .consultasEstudiante(cEstudiante)
                    .progresoMd(p.getProgresoMd() != null ? p.getProgresoMd() : "")
                    .nivelDificultad(nivelDificultad)
                    .nivelDificultadClase(nivelDificultadClase)
                    .estadoEvolucion(estadoEvolucion)
                    .estadoEvolucionClase(estadoEvolucionClase)
                    .estadoActividad(estadoActividad)
                    .estadoActividadColor(estadoActividadColor)
                    .build());
        }

        long totalMensajes = totalConsultasEstudiantes + totalRespuestasTutor;

        // Promedio de turnos por estudiante (M-02)
        double promTurnos = participaciones.isEmpty() ? 0.0 :
                Math.round(((double) totalConsultasEstudiantes / participaciones.size()) * 10.0) / 10.0;

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
                .promedioTurnosPorEstudiante(promTurnos)
                .totalSolicitudesDirectas(totalRf10)
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
            - Solicitudes de código directo o atajo prevenidas (M-04): %d
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
            
            **Seguimiento Socrático (M-04)**: Se registraron **%d** solicitudes de solución directa reconducidas con éxito hacia el razonamiento conceptual, garantizando que el tutor respondiera con preguntas orientadoras y analogías en lugar de código directo.
            
            **Recomendación Docente**: Se sugiere iniciar la siguiente clase presencial repasando la definición formal y el diseño de clases relacionado con *%s*, planteando un ejercicio colaborativo en el tablero.
            """.formatted(sesion.getTema(), cantEstudiantes, consultas, rf10, sesion.getTema());
    }
}
