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
        /** Estudiantes que han escrito al menos un mensaje (de totalParticipaciones inscritos) */
        private long estudiantesActivos;
        /** Promedio de consultas por estudiante, calculado solo entre quienes han escrito (M-02) */
        private double promedioTurnosPorEstudiante;
        /** Conteo de intentos de solicitar código directo prevenidos (M-04) */
        private long totalSolicitudesDirectas;
        /** Compatibilidad hacia atrás */
        private long totalIntervencionesAuditorRf10;
        /** M-01: errores recurrentes con el número y porcentaje de estudiantes (con informe) que los presentan, de mayor a menor */
        private List<FocoDificultad> dificultadesPorConcepto;
        /** Participaciones con diagnóstico (progreso_md) generado por el docente: base de M-01 */
        private long diagnosticosGenerados;
        /** Resumen ejecutivo y recomendaciones pedagógicas generadas con Gemini */
        private String resumenIa;
        /** Listado detallado de estudiantes y avances */
        private List<ParticipacionDetalleDto> participacionesDetalle;
    }

    /**
     * M-01: un error recurrente, cuántos estudiantes lo presentan y qué porcentaje representan
     * sobre los estudiantes con informe generado (base de la barra).
     */
    @lombok.Value
    public static class FocoDificultad {
        String error;
        long estudiantes;
        long porcentaje;
    }

    @Data
    @Builder
    public static class ParticipacionDetalleDto {
        private UUID id;
        private String estudianteNombre;
        private String estudianteCorreo;
        /** Fecha del último mensaje del estudiante ("Sin mensajes" si nunca escribió) */
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

    /** Texto y clases CSS de una insignia del dashboard. */
    record NivelTurnos(String etiqueta, String clase) {}

    /**
     * M-03: estado de evolución según si completó la ruta de preguntas guía y lo que dice su informe
     * (si la sección "Errores recurrentes" lista errores). No se basa en palabras sueltas del texto.
     */
    static NivelTurnos clasificarEvolucion(long consultas, boolean rutaCompletada, boolean tieneInforme, boolean tieneErrores) {
        if (consultas == 0) {
            return new NivelTurnos("Sin interacción", "bg-light text-muted border");
        }
        if (rutaCompletada) {
            if (!tieneInforme) {
                return new NivelTurnos("Completó la ruta · genera el informe", "bg-success-subtle text-success border border-success-subtle");
            }
            return tieneErrores
                    ? new NivelTurnos("Completó con dudas", "bg-info-subtle text-info-emphasis border border-info-subtle")
                    : new NivelTurnos("Consolidado", "bg-success-subtle text-success border border-success-subtle");
        }
        if (!tieneInforme) {
            return new NivelTurnos("En diálogo activo", "bg-primary-subtle text-primary border border-primary-subtle");
        }
        return tieneErrores
                ? new NivelTurnos("Dudas en progreso", "bg-warning-subtle text-warning-emphasis border border-warning-subtle")
                : new NivelTurnos("Avanzando sin errores", "bg-primary-subtle text-primary border border-primary-subtle");
    }

    /**
     * M-02: clasifica los turnos del estudiante frente a un margen esperado de
     * {@value #MARGEN_PREGUNTAS_POR_GUIA} preguntas por cada pregunta guía.
     * Dentro del margen nunca se marca "Requiere apoyo"; fuera de él, solo si además hay señales
     * (≥2 solicitudes de atajo o errores recurrentes en su informe). Si no, es "Diálogo perseverante".
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
        long estudiantesActivos = 0;
        int totalPreguntasGuia = ChatService.contarPreguntasGuia(sesion.getPreguntasGuia());
        Map<String, Long> dificultades = new LinkedHashMap<>();
        List<ParticipacionDetalleDto> detalleList = new ArrayList<>();

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
            if (cEstudiante > 0) {
                estudiantesActivos++;
            }

            // Consultar métricas de esta participación
            List<Metrica> metricasPart = metricasPorPart.getOrDefault(p.getId(), List.of());
            long rf10Part = contarAlertasAtajo(metricasPart, msgs);
            totalRf10 += rf10Part;

            String pMd = (p.getProgresoMd() != null) ? p.getProgresoMd() : "";
            boolean tieneInforme = GeminiService.esTextoGeneradoValido(pMd);
            Set<String> erroresInforme = tieneInforme ? extraerErroresRecurrentes(pMd) : Set.of();
            if (tieneInforme) {
                diagnosticosGenerados++;
            }

            // M-01: cada error de la sección "Errores recurrentes" del informe cuenta UNA vez por estudiante
            for (String error : erroresInforme) {
                dificultades.merge(error, 1L, Long::sum);
            }

            // Fecha del último mensaje (no la de inscripción)
            java.time.OffsetDateTime ultimaFecha = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getCreatedAt();
            String fechaStr = ultimaFecha != null ? ultimaFecha.format(FECHA_FORMATTER) : "Sin mensajes";
            String nombreEst = (p.getEstudiante() != null && p.getEstudiante().getNombre() != null)
                    ? p.getEstudiante().getNombre() : "Estudiante";
            String correoEst = (p.getEstudiante() != null && p.getEstudiante().getCorreo() != null)
                    ? p.getEstudiante().getCorreo() : "";

            // M-02: Turnos y Nivel (margen de 3 preguntas por pregunta guía). Señales: ≥2 atajos o errores en su informe
            boolean senalesDeApoyo = rf10Part >= 2 || !erroresInforme.isEmpty();
            NivelTurnos nivel = clasificarTurnos(cEstudiante, totalPreguntasGuia, senalesDeApoyo);
            String nivelDificultad = nivel.etiqueta();
            String nivelDificultadClase = nivel.clase();

            // M-03: Evolución según ruta completada + errores recurrentes del informe
            boolean rutaCompletada = Boolean.TRUE.equals(p.getRutaCompletada());
            NivelTurnos evolucion = clasificarEvolucion(cEstudiante, rutaCompletada, tieneInforme, !erroresInforme.isEmpty());
            String estadoEvolucion = evolucion.etiqueta();
            String estadoEvolucionClase = evolucion.clase();

            // M-05: Actividad y Estado de Compromiso (quien terminó la tutoría nunca está "en riesgo")
            String estadoActividad;
            String estadoActividadColor;
            if (cEstudiante == 0) {
                estadoActividad = "Sin participación";
                estadoActividadColor = "text-muted";
            } else if (p.isTutoriaFinalizada()) {
                estadoActividad = "Sesión completada";
                estadoActividadColor = "text-success";
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

        // Promedio de consultas por estudiante (M-02), solo entre quienes han escrito
        double promTurnos = estudiantesActivos == 0 ? 0.0 :
                Math.round(((double) totalConsultasEstudiantes / estudiantesActivos) * 10.0) / 10.0;

        // M-01: porcentaje = estudiantes con el error / estudiantes con informe generado; de mayor a menor
        final long baseInformes = diagnosticosGenerados;
        List<FocoDificultad> dificultadesOrdenadas = dificultades.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new FocoDificultad(e.getKey(), e.getValue(),
                        baseInformes > 0 ? Math.round(e.getValue() * 100.0 / baseInformes) : 0L))
                .collect(Collectors.toList());

        // Recuperar síntesis consolidada de la caché (Lazy Synthesis)
        // para evitar llamadas síncronas a Gemini en cada GET / recarga del dashboard
        String resumenIa;
        if (totalConsultasEstudiantes == 0) {
            resumenIa = ""; // el dashboard muestra "Aún no hay interacciones…"
        } else {
            resumenIa = sintesisPorSesionCache.getOrDefault(sesion.getId(), "");
        }

        return MetricasDashboard.builder()
                .totalSesiones(1)
                .totalParticipaciones(participaciones.size())
                .estudiantesActivos(estudiantesActivos)
                .totalMensajes(totalMensajes)
                .totalConsultasEstudiante(totalConsultasEstudiantes)
                .totalRespuestasTutor(totalRespuestasTutor)
                .promedioTurnosPorEstudiante(promTurnos)
                .totalSolicitudesDirectas(totalRf10)
                .totalIntervencionesAuditorRf10(totalRf10)
                .dificultadesPorConcepto(dificultadesOrdenadas)
                .diagnosticosGenerados(diagnosticosGenerados)
                .resumenIa(resumenIa)
                .participacionesDetalle(detalleList)
                .build();
    }

    /** Máximo de caracteres de mensajes por estudiante sin informe que se envían a la síntesis grupal. */
    static final int MAX_CARACTERES_MENSAJES_POR_ESTUDIANTE = 3000;

    /**
     * Genera bajo demanda la síntesis pedagógica del grupo con Gemini y la guarda en la caché en memoria.
     * Fuentes: preguntas guía, métricas de la sesión, el informe individual de cada estudiante que lo tenga
     * y, para quienes no tienen informe, sus propios mensajes. Los estudiantes se anonimizan ("Estudiante N").
     *
     * @throws IllegalStateException si no hay interacciones o la IA no pudo generar la síntesis
     *                               (nunca se devuelve ni se guarda un texto prefabricado)
     */
    public String generarYGuardarSintesisIa(Sesion sesion) {
        List<Participacion> participaciones = participacionRepository.findBySesion(sesion);

        Map<UUID, List<Mensaje>> mensajesPorPart = participaciones.isEmpty() ? Map.of() :
                mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));
        Map<UUID, List<Metrica>> metricasPorPart = participaciones.isEmpty() ? Map.of() :
                metricaRepository.findByParticipacionIn(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        long consultas = 0;
        long activos = 0;
        long rf10 = 0;
        long conInforme = 0;
        long rutasCompletadas = 0;
        StringBuilder evidencias = new StringBuilder();
        int numero = 0;

        for (Participacion p : participaciones) {
            List<Mensaje> msgs = mensajesPorPart.getOrDefault(p.getId(), List.of());
            List<Mensaje> delEstudiante = msgs.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getEmisor()) && m.getContenido() != null)
                    .toList();
            if (delEstudiante.isEmpty()) continue;

            numero++;
            activos++;
            consultas += delEstudiante.size();
            long atajos = contarAlertasAtajo(metricasPorPart.getOrDefault(p.getId(), List.of()), msgs);
            rf10 += atajos;
            boolean ruta = Boolean.TRUE.equals(p.getRutaCompletada());
            if (ruta) rutasCompletadas++;

            evidencias.append("\n### Estudiante ").append(numero)
                    .append(" (").append(delEstudiante.size()).append(" consultas")
                    .append(ruta ? ", completó la ruta" : ", ruta en curso")
                    .append(atajos > 0 ? ", " + atajos + " solicitudes de código directo" : "")
                    .append(")\n");

            String informe = p.getProgresoMd();
            if (GeminiService.esTextoGeneradoValido(informe)) {
                conInforme++;
                evidencias.append("Fuente: informe individual\n").append(informe.trim()).append("\n");
            } else {
                evidencias.append("Fuente: mensajes del estudiante (aún sin informe)\n");
                StringBuilder bloque = new StringBuilder();
                for (Mensaje m : delEstudiante) {
                    String linea = "- " + m.getContenido().trim().replace("\n", " ") + "\n";
                    if (bloque.length() + linea.length() > MAX_CARACTERES_MENSAJES_POR_ESTUDIANTE) {
                        bloque.append("- (… mensajes adicionales omitidos por extensión)\n");
                        break;
                    }
                    bloque.append(linea);
                }
                evidencias.append(bloque);
            }
        }

        if (consultas == 0) {
            throw new IllegalStateException("Aún no hay interacciones de estudiantes para generar la síntesis grupal.");
        }

        String preguntasGuia = (sesion.getPreguntasGuia() != null && !sesion.getPreguntasGuia().isBlank())
                ? sesion.getPreguntasGuia().trim() : "(La sesión no tiene preguntas guía configuradas)";

        String prompt = """
            Eres un Asesor Pedagógico y Analista Educativo en Programación Orientada a Objetos (POO).
            Analiza la evidencia de la clase "%s" y redacta una síntesis para el profesor.

            ## Preguntas guía del profesor (objetivos de la sesión)
            %s

            ## Métricas de la sesión
            - Estudiantes inscritos: %d
            - Estudiantes que interactuaron con el tutor: %d
            - Estudiantes que completaron la ruta de preguntas guía: %d
            - Estudiantes con informe individual generado: %d
            - Consultas enviadas por los estudiantes: %d
            - Solicitudes de código o respuesta directa detectadas: %d

            ## Evidencia por estudiante (anonimizada)
            %s

            Basa tu análisis ÚNICAMENTE en la evidencia anterior; no inventes datos. Si la evidencia es escasa, dilo.
            Responde en Markdown, conciso y constructivo, EXACTAMENTE con estos tres encabezados de nivel 3:

            ### Diagnóstico Grupal
            (Nivel de avance del grupo frente a las preguntas guía y madurez conceptual observada)

            ### Focos de Confusión y Atajos
            (Conceptos donde más estudiantes mostraron dudas o errores, y solicitudes de atajo)

            ### Recomendaciones para el Aula Presencial
            (2 o 3 sugerencias didácticas puntuales para la próxima clase, en lista numerada: 1. ..., 2. ...)
            """.formatted(
                sesion.getTema(), preguntasGuia,
                participaciones.size(), activos, rutasCompletadas, conInforme, consultas, rf10,
                evidencias.toString().trim());

        String sintesis = geminiService.generarRespuesta(prompt);
        if (!GeminiService.esTextoGeneradoValido(sintesis)) {
            throw new IllegalStateException("El motor de IA no pudo generar la síntesis en este momento. Intenta de nuevo en unos segundos.");
        }
        sintesis = sintesis.trim();
        sintesisPorSesionCache.put(sesion.getId(), sintesis);
        return sintesis;
    }

    /**
     * M-01: Extrae los títulos (sin repetir) de los errores recurrentes que Gemini identificó en el informe
     * de un estudiante, para contar cuántos estudiantes presentan cada error.
     */
    static Set<String> extraerErroresRecurrentes(String pMd) {
        Set<String> errores = new LinkedHashSet<>();
        if (pMd == null || !pMd.contains("## Errores recurrentes")) return errores;
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
                errores.add(concepto);
            }
        }
        return errores;
    }
}
