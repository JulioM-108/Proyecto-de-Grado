package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
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

    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
    private final GeminiService geminiService;

    /** Caché en memoria para evitar invocar a Gemini en cada recarga de página del dashboard */
    private final Map<UUID, String> sintesisPorSesionCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        /** Mapa concepto POO → cantidad de errores/dudas acumuladas (M-01, extraído de los diagnósticos generados) */
        private Map<String, Long> dificultadesPorConcepto;
        /** Participaciones con diagnóstico (progreso_md) generado por el docente: base de M-01 */
        private long diagnosticosGenerados;
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

    /** Preguntas que el estudiante puede hacer por cada pregunta guía antes de considerar que necesita apoyo. */
    static final int MARGEN_PREGUNTAS_POR_GUIA = 3;
    /** Sin preguntas guía configuradas se asume una ruta equivalente a 3 hitos. */
    static final int HITOS_POR_DEFECTO = 3;

    record NivelTurnos(String etiqueta, String clase) {}

    /**
     * M-02: clasifica los turnos del estudiante frente a un margen esperado de
     * {@value #MARGEN_PREGUNTAS_POR_GUIA} preguntas por cada pregunta guía.
     * Dentro del margen nunca se marca "Requiere apoyo"; fuera de él, solo si además hay señales
     * (≥2 solicitudes de atajo o confusión persistente en el diagnóstico). Si no, es "Diálogo perseverante".
     */
    static NivelTurnos clasificarTurnos(long turnos, int preguntasGuia, boolean senalesDeApoyo) {
        int hitos = preguntasGuia > 0 ? preguntasGuia : HITOS_POR_DEFECTO;
        long margen = (long) hitos * MARGEN_PREGUNTAS_POR_GUIA;
        String base = turnos + " de " + margen + " turnos";

        if (turnos == 0) {
            return new NivelTurnos("Sin consultas", "bg-light text-muted border");
        }
        if (turnos <= margen / 2) {
            return new NivelTurnos(base + " · Fluido", "bg-success-subtle text-success border border-success-subtle");
        }
        if (turnos <= margen) {
            return new NivelTurnos(base + " · Ritmo normal", "bg-info-subtle text-info-emphasis border border-info-subtle");
        }
        if (senalesDeApoyo) {
            return new NivelTurnos(base + " · Requiere apoyo", "bg-danger-subtle text-danger border border-danger-subtle");
        }
        return new NivelTurnos(base + " · Diálogo perseverante", "bg-primary-subtle text-primary border border-primary-subtle");
    }

    /**
     * M-04: alertas de atajo (solicitudes de código/solución directa) de una participación.
     * Fuente principal: métricas codigo=10 registradas en cada turno (patrón de texto o señal del tutor).
     * Respaldo para datos históricos sin métrica: el patrón sobre los mensajes del estudiante.
     */
    static long contarAlertasAtajo(List<Metrica> metricas, List<Mensaje> mensajes) {
        long registradas = metricas.stream()
                .filter(m -> "RF-10".equalsIgnoreCase(m.getConcepto()) || Long.valueOf(10L).equals(m.getCodigo()))
                .count();
        if (registradas > 0) {
            return registradas;
        }
        boolean atajoEnMensajes = mensajes.stream()
                .filter(m -> Boolean.TRUE.equals(m.getEmisor()))
                .anyMatch(m -> ChatService.esSolicitudAtajo(m.getContenido()));
        return atajoEnMensajes ? 1L : 0L;
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
        long diagnosticosGenerados = 0;
        int totalPreguntasGuia = ChatService.contarPreguntasGuia(sesion.getPreguntasGuia());
        Map<String, Long> dificultades = new LinkedHashMap<>();
        List<ParticipacionDetalleDto> detalleList = new ArrayList<>();
        List<String> fragmentosConsultas = new ArrayList<>();

        Map<UUID, List<Mensaje>> mensajesPorPart = participaciones.isEmpty() ? Map.of() :
                mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        Map<UUID, List<Metrica>> metricasPorPart = participaciones.isEmpty() ? Map.of() :
                metricaRepository.findByParticipacionIn(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        for (Participacion p : participaciones) {
            List<Mensaje> msgs = mensajesPorPart.getOrDefault(p.getId(), List.of());
            long cEstudiante = msgs.stream().filter(m -> Boolean.TRUE.equals(m.getEmisor())).count();
            long rTutor = msgs.stream().filter(m -> Boolean.FALSE.equals(m.getEmisor())).count();

            totalConsultasEstudiantes += cEstudiante;
            totalRespuestasTutor += rTutor;

            // Consultar métricas de esta participación
            List<Metrica> metricasPart = metricasPorPart.getOrDefault(p.getId(), List.of());
            long rf10Part = contarAlertasAtajo(metricasPart, msgs);
            totalRf10 += rf10Part;

            String pMd = (p.getProgresoMd() != null) ? p.getProgresoMd() : "";
            if (!GeminiService.esRespuestaDeError(pMd)) {
                diagnosticosGenerados++;
            }

            for (Metrica m : metricasPart) {
                String c = m.getConcepto();
                if (c != null
                        && !"RF-10".equalsIgnoreCase(c)
                        && !"sesion_cerrada".equalsIgnoreCase(c)
                        && !"turnos_totales".equalsIgnoreCase(c)
                        && !Long.valueOf(2L).equals(m.getCodigo())
                        && !Long.valueOf(3L).equals(m.getCodigo())
                        && !Long.valueOf(5L).equals(m.getCodigo())
                        && !Long.valueOf(10L).equals(m.getCodigo())) {
                    long val = m.getValorNumerico() != null ? m.getValorNumerico() : 1L;
                    dificultades.merge(c, val, Long::sum);
                }
            }

            // M-01: Extraer falencias conceptuales y patrones de error reales diagnosticados por la IA
            extraerErroresRecurrentes(p.getProgresoMd(), dificultades);

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

            // M-02: Turnos y Nivel (margen de 3 preguntas del estudiante por cada pregunta guía del docente)
            boolean senalesDeApoyo = rf10Part >= 2
                    || (pMd.contains("## Errores recurrentes")
                        && (pMd.toLowerCase().contains("persiste") || pMd.toLowerCase().contains("bloqueo")));
            NivelTurnos nivel = clasificarTurnos(cEstudiante, totalPreguntasGuia, senalesDeApoyo);
            String nivelDificultad = nivel.etiqueta();
            String nivelDificultadClase = nivel.clase();

            // M-03: Evolución Pedagógica
            String estadoEvolucion;
            String estadoEvolucionClase;
            if (cEstudiante == 0) {
                estadoEvolucion = "Sin interacción";
                estadoEvolucionClase = "bg-light text-muted border";
            } else if (!pMd.isBlank() && pMd.contains("## Conceptos dominados")) {
                if (pMd.contains("## Errores recurrentes") && (pMd.toLowerCase().contains("persiste") || pMd.toLowerCase().contains("confunde") || pMd.toLowerCase().contains("bloqueo"))) {
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

        // Si aún no hay diagnósticos de error procesados pero hay consultas activas, mostramos el tema de clase
        if (dificultades.isEmpty() && totalConsultasEstudiantes > 0) {
            String temaNombre = (sesion.getTema() != null && !sesion.getTema().isBlank()) ? sesion.getTema().trim() : "POO General";
            dificultades.put("Consultas en tema: " + temaNombre, totalConsultasEstudiantes);
        }

        // Recuperar síntesis consolidada de la caché (Lazy Synthesis)
        // para evitar llamadas síncronas a Gemini en cada GET / recarga del dashboard
        String resumenIa;
        if (totalConsultasEstudiantes == 0) {
            resumenIa = "Aún no se registran interacciones en esta sesión de clase. "
                    + "Comparte el código privado con tus estudiantes para que dialoguen con el tutor socrático. "
                    + "Tan pronto como envíen sus consultas, la Inteligencia Artificial generará aquí un análisis pedagógico consolidado.";
        } else {
            resumenIa = sintesisPorSesionCache.getOrDefault(sesion.getId(), "");
        }

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
                .diagnosticosGenerados(diagnosticosGenerados)
                .resumenIa(resumenIa)
                .participacionesDetalle(detalleList)
                .build();
    }

    /**
     * Genera la síntesis pedagógica consolidada bajo demanda con Gemini y la guarda en la caché en memoria.
     */
    public String generarYGuardarSintesisIa(Sesion sesion) {
        List<Participacion> participaciones = participacionRepository.findBySesion(sesion);
        long totalConsultasEstudiantes = 0;
        long totalRespuestasTutor = 0;
        long totalRf10 = 0;
        List<String> fragmentosConsultas = new ArrayList<>();

        if (participaciones.isEmpty()) {
            return "Aún no se registran interacciones en esta sesión de clase. "
                    + "Comparte el código privado con tus estudiantes para que dialoguen con el tutor socrático. "
                    + "Tan pronto como envíen sus consultas, la Inteligencia Artificial generará aquí un análisis pedagógico consolidado.";
        }

        Map<UUID, List<Mensaje>> mensajesPorPart = mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(participaciones).stream()
                .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        Map<UUID, List<Metrica>> metricasPorPart = metricaRepository.findByParticipacionIn(participaciones).stream()
                .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        for (Participacion p : participaciones) {
            List<Mensaje> msgs = mensajesPorPart.getOrDefault(p.getId(), List.of());
            totalConsultasEstudiantes += msgs.stream().filter(m -> Boolean.TRUE.equals(m.getEmisor())).count();
            totalRespuestasTutor += msgs.stream().filter(m -> Boolean.FALSE.equals(m.getEmisor())).count();

            List<Metrica> metricasPart = metricasPorPart.getOrDefault(p.getId(), List.of());
            totalRf10 += contarAlertasAtajo(metricasPart, msgs);

            msgs.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getEmisor()) && m.getContenido() != null)
                    .map(m -> m.getContenido().trim())
                    .limit(3)
                    .forEach(fragmentosConsultas::add);
        }

        String sintesis = generarSintesisIa(sesion, participaciones.size(), totalConsultasEstudiantes,
                totalRespuestasTutor, totalRf10, fragmentosConsultas);

        if (sintesis != null && !sintesis.isBlank()) {
            sintesisPorSesionCache.put(sesion.getId(), sintesis);
        }
        return sintesis;
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

            Genera un informe pedagógico ejecutivo conciso (máximo 3 párrafos en Markdown, con tono profesional y constructivo para el profesor).
            IMPORTANTE: Estructura tu respuesta EXACTAMENTE con estos tres (3) encabezados de nivel 3:

            ### Diagnóstico Grupal
            (Análisis del nivel de interacción y madurez conceptual observada frente al tema)

            ### Focos de Confusión y Atajos
            (Conceptos donde los estudiantes requirieron mayor guía, mostraron dudas o solicitaron atajos)

            ### Recomendaciones para el Aula Presencial
            (2 o 3 sugerencias didácticas puntuales para reforzar en la próxima clase, en lista numerada: 1. ..., 2. ...)
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
            if (!GeminiService.esRespuestaDeError(respuesta) && !respuesta.startsWith("[Modo Mock")) {
                return respuesta.trim();
            }
        } catch (Exception e) {
            log.warn("No se pudo generar la síntesis con IA, generando reporte algorítmico: {}", e.getMessage());
        }

        // Fallback pedagógico estructurado
        return """
            ### Diagnóstico Grupal
            La sesión sobre **%s** cuenta con **%d** estudiantes activos y un total de **%d** consultas registradas. La dinámica socrática ha permitido guiar el razonamiento autónomo de los alumnos sin suministrar código de manera pasiva.
            
            ### Focos de Confusión y Atajos
            Se registraron **%d** solicitudes de solución directa reconducidas con éxito hacia el razonamiento conceptual, garantizando que el tutor respondiera con preguntas orientadoras y analogías en lugar de código directo.
            
            ### Recomendaciones para el Aula Presencial
            1. **Modelado en Pizarra**: Iniciar la siguiente clase presencial repasando la definición formal y el diseño conceptual relacionado con *%s*, planteando un ejercicio colaborativo en el tablero.
            2. **Prueba de Escritorio Grupal**: Trazar el cambio de valores y flujos de control paso a paso en lenguaje natural antes de escribir código.
            """.formatted(sesion.getTema(), cantEstudiantes, consultas, rf10, sesion.getTema());
    }

    /**
     * M-01: Extrae los títulos de las falencias y errores recurrentes identificados por Gemini en el progreso pedagógico.
     */
    private void extraerErroresRecurrentes(String pMd, Map<String, Long> dificultades) {
        if (pMd == null || !pMd.contains("## Errores recurrentes")) return;
        int start = pMd.indexOf("## Errores recurrentes");
        int nextSection = pMd.indexOf("## ", start + 22);
        String seccionErrores = (nextSection != -1)
                ? pMd.substring(start + 22, nextSection).trim()
                : pMd.substring(start + 22).trim();

        for (String linea : seccionErrores.split("\n")) {
            String l = linea.trim();
            if (l.startsWith("-") || l.startsWith("*")) {
                l = l.substring(1).trim();
            }
            if (l.isBlank() || l.startsWith("(") || l.startsWith("#")) continue;

            String concepto;
            if (l.contains(":")) {
                concepto = l.substring(0, l.indexOf(":")).replace("**", "").replace("*", "").trim();
            } else if (l.length() > 50) {
                concepto = l.substring(0, 50).trim() + "...";
            } else {
                concepto = l.replace("**", "").replace("*", "").trim();
            }

            if (!concepto.isBlank() && concepto.length() >= 3 && concepto.length() <= 65) {
                concepto = Character.toUpperCase(concepto.charAt(0)) + concepto.substring(1);
                dificultades.merge(concepto, 1L, Long::sum);
            }
        }
    }
}
