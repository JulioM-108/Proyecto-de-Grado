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
 * Analítica del dashboard docente de una clase (sesión) y síntesis pedagógica grupal con IA.
 * <p>
 * Calcula, a partir de los mensajes, las métricas de la tabla metrica y los informes individuales
 * (progreso_md): M-01 / RF-06 (errores recurrentes por concepto), M-02 (turnos por pregunta guía),
 * M-03 (solicitudes de respuesta directa) y M-04 / RF-08 (frecuencia, duración y riesgo de abandono).
 * También genera con Gemini y guarda la síntesis grupal (RF-07).
 * <p>
 * Respeta RNF-01: al docente le entrega métricas, informes y la síntesis anonimizada, nunca el chat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
    private final GeminiService geminiService;
    private final SesionRepository sesionRepository;

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Datos que muestra el dashboard docente de una sesión. */
    @Data
    @Builder
    public static class MetricasDashboard {
        /** Siempre 1: el dashboard es por sesión */
        private long totalSesiones;
        private long totalParticipaciones;
        private long totalMensajes;
        private long totalConsultasEstudiante;
        private long totalRespuestasTutor;
        /** Estudiantes que han escrito al menos un mensaje (de totalParticipaciones inscritos) */
        private long estudiantesActivos;
        /** Promedio de consultas por estudiante, solo entre quienes han escrito (referencia global de M-02) */
        private double promedioTurnosPorEstudiante;
        /** M-03: solicitudes de código o respuesta directa del grupo */
        private long totalSolicitudesDirectas;
        /** M-03: porcentaje de las consultas del grupo que fueron solicitudes de respuesta directa */
        private long tasaSolicitudesDirectas;
        /** Mismo valor que totalSolicitudesDirectas; nombre heredado de cuando M-03 se registraba como "RF-10" */
        private long totalIntervencionesAuditorRf10;
        /** M-01 / RF-06 grupal: errores recurrentes con el número y porcentaje de estudiantes (con informe) que los presentan, de mayor a menor */
        private List<FocoDificultad> dificultadesPorConcepto;
        /** Participaciones con informe (progreso_md): base de M-01 */
        private long diagnosticosGenerados;
        /** Estudiantes que escribieron al tutor y aún no tienen informe (botón "Generar informes pendientes") */
        private long informesPendientes;
        /** M-02 grupal: turnos promedio por pregunta guía, en el orden de las preguntas */
        private List<PromedioGuia> turnosPorPreguntaGuia;
        /** M-04: tiempo activo promedio en la clase entre quienes escribieron (texto: "18 min") */
        private String tiempoActivoPromedio;
        /** RF-07: síntesis grupal guardada en Sesion.sintesis_md; vacía si no hay consultas o aún no se generó */
        private String resumenIa;
        /** RF-07: fecha de generación de la síntesis ("dd/MM/yyyy HH:mm"); null si no hay */
        private String resumenIaFecha;
        /** Una fila por estudiante inscrito con su avance individual */
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

    /** M-02 grupal: turnos promedio que tomó una pregunta guía (null si nadie la ha superado aún). */
    @lombok.Value
    public static class PromedioGuia {
        int numero;
        Double promedio;
        /** Estudiantes que la superaron con un tramo individual (base del promedio) */
        long estudiantes;
        /** La pregunta con más turnos promedio del grupo (solo si al menos dos preguntas tienen datos) */
        boolean masDificil;
        /** Clases CSS de la insignia */
        String clase;
    }

    /** M-02 individual: un tramo de la ruta (una pregunta, o varias si el tutor no las marcó por separado). */
    @lombok.Value
    public static class TramoGuia {
        /** Texto de la insignia ("P2 · 4") */
        String etiqueta;
        /** Clases CSS de la insignia */
        String clase;
        /** Texto descriptivo (tooltip) */
        String titulo;
    }

    /**
     * Fila de un estudiante en el dashboard. Incluye su informe individual (RF-06) pero no los mensajes
     * del chat (RNF-01).
     */
    @Data
    @Builder
    public static class ParticipacionDetalleDto {
        private UUID id;
        private String estudianteNombre;
        private String estudianteCorreo;
        /** Fecha del último mensaje de la conversación, del estudiante o del tutor ("Sin mensajes" si no hay) */
        private String fechaFormateada;
        /** Mensajes del estudiante y del tutor */
        private long totalMensajes;
        private long consultasEstudiante;
        /** Informe individual en Markdown generado con Gemini ("" si no tiene) */
        private String progresoMd;

        // M-02: avance por pregunta guía
        private String nivelDificultad;
        private String nivelDificultadClase;
        private List<TramoGuia> tramosGuia;

        // M-03: solicitudes de respuesta directa
        private long solicitudesDirectas;
        private long tasaSolicitudesDirectas;
        private String etiquetaSolicitudes;
        private String claseSolicitudes;

        // M-04 (RF-08): frecuencia, duración y riesgo de abandono
        private String estadoActividad;
        private String estadoActividadColor;
        private String tiempoActivo;
        /** Clases del docente en las que está inscrito el estudiante, y en cuántas completó la ruta */
        private long clasesConElDocente;
        private long clasesCompletadas;
    }

    /**
     * M-02: hasta este número de turnos una pregunta guía se considera fluida; también es el margen
     * de turnos por hito cuando la sesión no tiene preguntas guía.
     */
    static final int MARGEN_PREGUNTAS_POR_GUIA = 3;
    /** M-02: más de este número de turnos en una sola pregunta guía indica que el estudiante "se trabó" (de 4 a 6 = con esfuerzo). */
    static final int TURNOS_TRABADO_POR_GUIA = 2 * MARGEN_PREGUNTAS_POR_GUIA;
    /** Sin preguntas guía configuradas se asume una ruta equivalente a 3 hitos. */
    static final int HITOS_POR_DEFECTO = 3;
    /** M-03: desde este porcentaje (con al menos 2 solicitudes) se marca "Posible frustración". */
    static final int TASA_FRUSTRACION = 20;
    /** M-04: un silencio mayor a este número de minutos entre mensajes no cuenta como tiempo activo. */
    static final long PAUSA_MAXIMA_MINUTOS = 10;

    /** Texto y clases CSS de una insignia del dashboard. */
    record NivelTurnos(String etiqueta, String clase) {}

    // Clases CSS (Bootstrap) de las insignias del dashboard
    private static final String CLASE_EXITO = "bg-success-subtle text-success border border-success-subtle";
    private static final String CLASE_INFO = "bg-info-subtle text-info-emphasis border border-info-subtle";
    private static final String CLASE_ALERTA = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
    private static final String CLASE_PELIGRO = "bg-danger-subtle text-danger border border-danger-subtle";
    private static final String CLASE_PRIMARIA = "bg-primary-subtle text-primary border border-primary-subtle";
    private static final String CLASE_NEUTRA = "bg-light text-muted border";

    /**
     * M-02 sin preguntas guía: clasifica los turnos totales frente a un margen de
     * {@value #MARGEN_PREGUNTAS_POR_GUIA} turnos por hito (un hito por pregunta guía o, si no hay,
     * {@value #HITOS_POR_DEFECTO}). Hasta la mitad del margen es "Fluido" y hasta el margen, "Ritmo normal".
     * Dentro del margen nunca se marca "Requiere apoyo"; fuera de él, solo si además hay señales.
     *
     * @param senalesDeApoyo posible frustración (M-03) o errores en el informe individual
     */
    static NivelTurnos clasificarTurnos(long turnos, int preguntasGuia, boolean senalesDeApoyo) {
        int hitos = preguntasGuia > 0 ? preguntasGuia : HITOS_POR_DEFECTO;
        long margen = (long) hitos * MARGEN_PREGUNTAS_POR_GUIA;
        String base = turnos + " de " + margen + " turnos";

        if (turnos == 0) {
            return new NivelTurnos("Sin consultas", CLASE_NEUTRA);
        }
        if (turnos <= margen / 2) {
            return new NivelTurnos(base + " · Fluido", CLASE_EXITO);
        }
        if (turnos <= margen) {
            return new NivelTurnos(base + " · Ritmo normal", CLASE_INFO);
        }
        if (senalesDeApoyo) {
            return new NivelTurnos(base + " · Requiere apoyo", CLASE_PELIGRO);
        }
        return new NivelTurnos(base + " · Diálogo perseverante", CLASE_PRIMARIA);
    }

    /** M-02: tramo de la ruta registrado por el tutor (desde-hasta) con los turnos que tomó. */
    record Tramo(int desde, int hasta, long turnos) {
        boolean individual() { return desde == hasta; }

        int preguntas() { return hasta - desde + 1; }

        /**
         * Tramo agrupado (el tutor no marcó las preguntas intermedias) con más turnos de los esperados:
         * más de {@value AnalyticsService#MARGEN_PREGUNTAS_POR_GUIA} por pregunta que abarca.
         */
        boolean agrupadoConEsfuerzo() {
            return !individual() && turnos > (long) preguntas() * MARGEN_PREGUNTAS_POR_GUIA;
        }

        String etiqueta() { return individual() ? "P" + hasta : "P" + desde + "-P" + hasta; }
    }

    /**
     * M-02: avance de un estudiante en las preguntas guía. {@code preguntaEnCurso} es 0 si no hay ninguna en
     * curso; {@code turnosEnCurso} son las consultas aún no asignadas a un tramo ni a la apertura;
     * turnosApertura &lt; 0 si el tutor no marcó la apertura.
     */
    record AvanceGuias(List<Tramo> tramos, int preguntaEnCurso, long turnosEnCurso, long turnosApertura) {
        AvanceGuias(List<Tramo> tramos, int preguntaEnCurso, long turnosEnCurso) {
            this(tramos, preguntaEnCurso, turnosEnCurso, -1);
        }
    }

    /**
     * M-02: reconstruye el avance a partir de las métricas código 21 ("P3" o "P2-P4"), la apertura (código 20, si el
     * tutor marcó GUIA_INICIADA) y las consultas totales. Las consultas que no cubren los tramos ni la apertura se
     * atribuyen a la pregunta siguiente a la última superada, salvo que la ruta esté completada.
     *
     * @param metricas  métricas de la participación (se ignoran los demás códigos)
     * @param consultas mensajes enviados por el estudiante
     */
    static AvanceGuias calcularAvance(List<Metrica> metricas, long consultas, int totalGuias, boolean rutaCompletada) {
        List<Tramo> tramos = new ArrayList<>();
        long apertura = -1;
        for (Metrica m : metricas) {
            if (Long.valueOf(ChatService.CODIGO_APERTURA).equals(m.getCodigo())) {
                // Puede haber varios registros de apertura: se suman partiendo de 0 (el -1 solo indica "sin marcar")
                apertura = Math.max(0, apertura) + (m.getValorNumerico() != null ? m.getValorNumerico() : 0);
                continue;
            }
            if (!Long.valueOf(ChatService.CODIGO_TURNOS_POR_GUIA).equals(m.getCodigo()) || m.getConcepto() == null) continue;
            java.util.regex.Matcher mt = java.util.regex.Pattern.compile("^P(\\d+)(?:-P(\\d+))?$").matcher(m.getConcepto().trim());
            if (!mt.matches()) continue;
            int desde = Integer.parseInt(mt.group(1));
            int hasta = mt.group(2) != null ? Integer.parseInt(mt.group(2)) : desde;
            tramos.add(new Tramo(desde, hasta, m.getValorNumerico() != null ? m.getValorNumerico() : 0));
        }
        tramos.sort(Comparator.comparingInt(Tramo::hasta));
        int ultima = tramos.isEmpty() ? 0 : tramos.get(tramos.size() - 1).hasta();
        long asignados = tramos.stream().mapToLong(Tramo::turnos).sum() + Math.max(0, apertura);
        boolean enCurso = !rutaCompletada && totalGuias > 0 && ultima < totalGuias;
        return new AvanceGuias(tramos, enCurso ? ultima + 1 : 0, enCurso ? Math.max(0, consultas - asignados) : 0, apertura);
    }

    /** M-02: clase de la insignia de una pregunta según los turnos: fluido (hasta 3), con esfuerzo (4 a 6) o se trabó (más de 6). */
    static String claseTurnosGuia(long turnos) {
        if (turnos <= MARGEN_PREGUNTAS_POR_GUIA) return CLASE_EXITO;
        if (turnos <= TURNOS_TRABADO_POR_GUIA) return CLASE_ALERTA;
        return CLASE_PELIGRO;
    }

    /** Insignias de M-02 para la fila del estudiante: "P1 · 2", "P2-P3 · 5", "P4 · 3 en curso". */
    static List<TramoGuia> tramosParaVista(AvanceGuias avance) {
        List<TramoGuia> vista = new ArrayList<>();
        if (avance.turnosApertura() > 0) {
            vista.add(new TramoGuia("Apertura · " + avance.turnosApertura(), CLASE_NEUTRA,
                    "Saludo y pregunta diagnóstica antes de la Pregunta Guía #1: " + avance.turnosApertura() + " turnos (no cuentan en P1)"));
        }
        for (Tramo t : avance.tramos()) {
            if (t.individual()) {
                vista.add(new TramoGuia("P" + t.hasta() + " · " + t.turnos(), claseTurnosGuia(t.turnos()),
                        "Pregunta guía " + t.hasta() + ": " + t.turnos() + " turnos"));
            } else {
                vista.add(new TramoGuia(t.etiqueta() + " · " + t.turnos(), t.agrupadoConEsfuerzo() ? CLASE_ALERTA : CLASE_NEUTRA,
                        "El tutor no marcó estas preguntas por separado: " + t.turnos() + " turnos en total"
                                + (t.agrupadoConEsfuerzo() ? " (más de " + MARGEN_PREGUNTAS_POR_GUIA + " por pregunta)" : "")));
            }
        }
        if (avance.preguntaEnCurso() > 0 && avance.turnosEnCurso() > 0) {
            long t = avance.turnosEnCurso();
            vista.add(new TramoGuia("P" + avance.preguntaEnCurso() + " · " + t + " en curso",
                    t > TURNOS_TRABADO_POR_GUIA ? CLASE_PELIGRO : CLASE_PRIMARIA,
                    "Pregunta guía " + avance.preguntaEnCurso() + " en curso: " + t + " turnos hasta ahora"));
        }
        return vista;
    }

    /**
     * M-02: estado del estudiante según su avance por pregunta guía. "Se trabó" = más de
     * {@value #TURNOS_TRABADO_POR_GUIA} turnos en una pregunta; con señales (frustración en M-03 o errores
     * en el informe) pasa a "Requiere apoyo". Si el tutor no marcó algunas preguntas y ese tramo agrupado
     * supera los {@value #MARGEN_PREGUNTAS_POR_GUIA} turnos por pregunta, se indica "esfuerzo en Pa-Pb" (no se
     * puede saber en cuál de ellas se trabó). Sin preguntas guía se usa el margen de turnos totales.
     */
    static NivelTurnos clasificarAvance(AvanceGuias avance, long consultas, int totalGuias,
                                        boolean rutaCompletada, boolean senalesDeApoyo) {
        if (consultas == 0) {
            return new NivelTurnos("Sin consultas", CLASE_NEUTRA);
        }
        if (totalGuias == 0) {
            return clasificarTurnos(consultas, 0, senalesDeApoyo);
        }

        int preguntaMasLarga = 0;
        long turnosMasLarga = 0;
        for (Tramo t : avance.tramos()) {
            if (t.individual() && t.turnos() > turnosMasLarga) {
                turnosMasLarga = t.turnos();
                preguntaMasLarga = t.hasta();
            }
        }
        if (avance.preguntaEnCurso() > 0 && avance.turnosEnCurso() > turnosMasLarga) {
            turnosMasLarga = avance.turnosEnCurso();
            preguntaMasLarga = avance.preguntaEnCurso();
        }
        boolean seTrabo = turnosMasLarga > TURNOS_TRABADO_POR_GUIA;
        // Tramo agrupado con más turnos de los esperados (el tutor olvidó alguna etiqueta GUIA_SUPERADA)
        Tramo agrupado = avance.tramos().stream()
                .filter(Tramo::agrupadoConEsfuerzo)
                .max(Comparator.comparingLong(Tramo::turnos))
                .orElse(null);

        if (rutaCompletada) {
            if (seTrabo) {
                return new NivelTurnos("Ruta completada · se trabó en P" + preguntaMasLarga, CLASE_INFO);
            }
            if (agrupado != null) {
                return new NivelTurnos("Ruta completada · esfuerzo en " + agrupado.etiqueta(), CLASE_INFO);
            }
            return new NivelTurnos("Ruta completada en " + consultas + " turnos", CLASE_EXITO);
        }
        if (seTrabo && senalesDeApoyo) {
            return new NivelTurnos("Requiere apoyo · P" + preguntaMasLarga, CLASE_PELIGRO);
        }
        if (seTrabo) {
            return new NivelTurnos("Se trabó en P" + preguntaMasLarga + " (" + turnosMasLarga + " turnos)", CLASE_ALERTA);
        }
        if (agrupado != null && senalesDeApoyo) {
            return new NivelTurnos("Requiere apoyo · " + agrupado.etiqueta(), CLASE_PELIGRO);
        }
        if (agrupado != null) {
            return new NivelTurnos("Esfuerzo en " + agrupado.etiqueta() + " (" + agrupado.turnos() + " turnos)", CLASE_ALERTA);
        }
        int actual = avance.preguntaEnCurso() > 0 ? avance.preguntaEnCurso() : totalGuias;
        return new NivelTurnos("Avanzando · va en P" + actual + " de " + totalGuias, CLASE_PRIMARIA);
    }

    /** M-02 grupal: promedio de turnos de cada pregunta guía entre quienes la superaron (tramos individuales). */
    static List<PromedioGuia> promediosPorGuia(List<AvanceGuias> avances, int totalGuias) {
        List<PromedioGuia> resultado = new ArrayList<>();
        if (totalGuias <= 0) return resultado;
        double[] suma = new double[totalGuias + 1];
        long[] cuenta = new long[totalGuias + 1];
        for (AvanceGuias a : avances) {
            for (Tramo t : a.tramos()) {
                if (t.individual() && t.hasta() >= 1 && t.hasta() <= totalGuias) {
                    suma[t.hasta()] += t.turnos();
                    cuenta[t.hasta()]++;
                }
            }
        }
        int conDatos = 0;
        int masDificil = 0;
        double mayor = -1;
        for (int n = 1; n <= totalGuias; n++) {
            if (cuenta[n] == 0) continue;
            conDatos++;
            double prom = suma[n] / cuenta[n];
            if (prom > mayor) {
                mayor = prom;
                masDificil = n;
            }
        }
        for (int n = 1; n <= totalGuias; n++) {
            Double prom = cuenta[n] == 0 ? null : Math.round(suma[n] / cuenta[n] * 10.0) / 10.0;
            // Solo se señala si al menos dos preguntas tienen datos y la más costosa ya no es fluida (> 3 turnos)
            resultado.add(new PromedioGuia(n, prom, cuenta[n], conDatos >= 2 && n == masDificil && mayor > MARGEN_PREGUNTAS_POR_GUIA,
                    prom == null ? CLASE_NEUTRA : claseTurnosGuia(Math.round(prom))));
        }
        return resultado;
    }

    /**
     * M-03: solicitudes de respuesta directa de una participación.
     * Fuente principal: métricas codigo=10 registradas en cada turno (patrón de texto o señal del tutor).
     * Respaldo para datos históricos sin métrica: el patrón sobre los mensajes del estudiante, que solo
     * indica si hubo alguna solicitud (devuelve como máximo 1).
     */
    static long contarAlertasAtajo(List<Metrica> metricas, List<Mensaje> mensajes) {
        long registradas = metricas.stream()
                // codigo 10; "RF-10" es el concepto con que se guardaban los registros antiguos
                .filter(m -> Long.valueOf(ChatService.CODIGO_ATAJO).equals(m.getCodigo()) || "RF-10".equalsIgnoreCase(m.getConcepto()))
                .count();
        if (registradas > 0) {
            return registradas;
        }
        boolean atajoEnMensajes = mensajes.stream()
                .filter(m -> Boolean.TRUE.equals(m.getEmisor()))
                .anyMatch(m -> ChatService.esSolicitudAtajo(m.getContenido()));
        return atajoEnMensajes ? 1L : 0L;
    }

    /** M-03: porcentaje de las consultas del estudiante que fueron solicitudes de respuesta directa. */
    static long tasaSolicitudes(long solicitudes, long consultas) {
        return consultas == 0 ? 0 : Math.min(100, Math.round(solicitudes * 100.0 / consultas));
    }

    /** M-03: "Posible frustración" con al menos 2 solicitudes que sean el {@value #TASA_FRUSTRACION} % o más de sus consultas. */
    static boolean posibleFrustracion(long solicitudes, long consultas) {
        return solicitudes >= 2 && tasaSolicitudes(solicitudes, consultas) >= TASA_FRUSTRACION;
    }

    /** M-03: insignia de la fila del estudiante: "Sin solicitudes", "Puntual" o "Posible frustración", con conteo y tasa. */
    static NivelTurnos clasificarSolicitudes(long solicitudes, long consultas) {
        if (solicitudes == 0) {
            return new NivelTurnos("Sin solicitudes", CLASE_NEUTRA);
        }
        String base = solicitudes + " · " + tasaSolicitudes(solicitudes, consultas) + " %";
        return posibleFrustracion(solicitudes, consultas)
                ? new NivelTurnos(base + " · Posible frustración", CLASE_PELIGRO)
                : new NivelTurnos(base + " · Puntual", CLASE_ALERTA);
    }

    /**
     * M-04: minutos activos a partir de las horas de los mensajes. Se suman los intervalos entre mensajes
     * consecutivos; un silencio de más de {@value #PAUSA_MAXIMA_MINUTOS} minutos (p. ej. una pausa) no cuenta.
     */
    public static long minutosActivos(List<java.time.OffsetDateTime> horas) {
        List<java.time.OffsetDateTime> ordenadas = horas.stream().filter(Objects::nonNull).sorted().toList();
        long segundos = 0;
        for (int i = 1; i < ordenadas.size(); i++) {
            long gap = java.time.Duration.between(ordenadas.get(i - 1), ordenadas.get(i)).getSeconds();
            if (gap > 0 && gap <= PAUSA_MAXIMA_MINUTOS * 60) {
                segundos += gap;
            }
        }
        return Math.round(segundos / 60.0);
    }

    /** M-04: "< 1 min", "18 min" o "1 h 05 min". */
    public static String formatearMinutos(long minutos) {
        if (minutos < 1) return "< 1 min";
        if (minutos < 60) return minutos + " min";
        return (minutos / 60) + " h " + String.format("%02d", minutos % 60) + " min";
    }

    /** Clases de un estudiante con el docente: [total, completadas]. */
    private record ClasesEstudiante(long total, long completadas) {}

    /**
     * Calcula las métricas del dashboard docente de una sesión: M-01 a M-04 a nivel grupal y por estudiante
     * (RF-06, RF-08), y adjunta la síntesis grupal ya guardada (RF-07).
     * Es de solo lectura: no llama a Gemini ni escribe en la BD.
     */
    @Transactional(readOnly = true)
    public MetricasDashboard obtenerMetricasPorSesion(Sesion sesion) {
        List<Participacion> participaciones = participacionRepository.findBySesion(sesion);

        long totalConsultasEstudiantes = 0;
        long totalRespuestasTutor = 0;
        long totalRf10 = 0;
        long diagnosticosGenerados = 0;
        long informesPendientes = 0;
        long estudiantesActivos = 0;
        long minutosActivosTotales = 0;
        int totalPreguntasGuia = ChatService.contarPreguntasGuia(sesion.getPreguntasGuia());
        // M-01: conteo por clave normalizada (sin tildes, mayúsculas ni puntuación); se muestra el primer título visto
        Map<String, Long> dificultades = new LinkedHashMap<>();
        Map<String, String> tituloPorClave = new HashMap<>();
        List<ParticipacionDetalleDto> detalleList = new ArrayList<>();
        List<AvanceGuias> avances = new ArrayList<>();

        // Mensajes y métricas de todo el grupo en dos consultas (evita una consulta por estudiante)
        Map<UUID, List<Mensaje>> mensajesPorPart = participaciones.isEmpty() ? Map.of() :
                mensajeRepository.findByParticipacionInOrderByCreatedAtAsc(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        Map<UUID, List<Metrica>> metricasPorPart = participaciones.isEmpty() ? Map.of() :
                metricaRepository.findByParticipacionIn(participaciones).stream()
                        .collect(Collectors.groupingBy(m -> m.getParticipacion().getId()));

        // M-04: clases de cada estudiante con este docente (total y con la ruta completada)
        Map<UUID, ClasesEstudiante> clasesPorEstudiante = new HashMap<>();
        if (!participaciones.isEmpty() && sesion.getProfesor() != null) {
            participacionRepository.findByProfesorWithEstudianteAndSesion(sesion.getProfesor()).stream()
                    .filter(p -> p.getEstudiante() != null)
                    .collect(Collectors.groupingBy(p -> p.getEstudiante().getId()))
                    .forEach((id, ps) -> clasesPorEstudiante.put(id, new ClasesEstudiante(ps.size(),
                            ps.stream().filter(p -> Boolean.TRUE.equals(p.getRutaCompletada())).count())));
        }

        for (Participacion p : participaciones) {
            List<Mensaje> msgs = mensajesPorPart.getOrDefault(p.getId(), List.of());
            // emisor: true = estudiante, false = tutor
            long cEstudiante = msgs.stream().filter(m -> Boolean.TRUE.equals(m.getEmisor())).count();
            long rTutor = msgs.stream().filter(m -> Boolean.FALSE.equals(m.getEmisor())).count();

            totalConsultasEstudiantes += cEstudiante;
            totalRespuestasTutor += rTutor;
            if (cEstudiante > 0) {
                estudiantesActivos++;
            }

            List<Metrica> metricasPart = metricasPorPart.getOrDefault(p.getId(), List.of());
            boolean rutaCompletada = Boolean.TRUE.equals(p.getRutaCompletada());

            String pMd = (p.getProgresoMd() != null) ? p.getProgresoMd() : "";
            boolean tieneInforme = GeminiService.esTextoGeneradoValido(pMd);
            Set<String> erroresInforme = tieneInforme ? extraerErroresRecurrentes(pMd) : Set.of();
            if (tieneInforme) {
                diagnosticosGenerados++;
            } else if (cEstudiante > 0) {
                informesPendientes++;
            }

            // M-01: cada error de la sección "Errores recurrentes" del informe cuenta UNA vez por estudiante
            Set<String> clavesEstudiante = new HashSet<>();
            for (String error : erroresInforme) {
                String clave = claveError(error);
                if (clave.isBlank() || !clavesEstudiante.add(clave)) continue;
                tituloPorClave.putIfAbsent(clave, error);
                dificultades.merge(clave, 1L, Long::sum);
            }

            // M-03: solicitudes de respuesta directa (conteo, tasa y etiqueta)
            long solicitudes = contarAlertasAtajo(metricasPart, msgs);
            totalRf10 += solicitudes;
            NivelTurnos etiquetaSolicitudes = clasificarSolicitudes(solicitudes, cEstudiante);

            // M-02: avance por pregunta guía. Señales de apoyo: posible frustración (M-03) o errores en su informe
            AvanceGuias avance = calcularAvance(metricasPart, cEstudiante, totalPreguntasGuia, rutaCompletada);
            avances.add(avance);
            boolean senalesDeApoyo = posibleFrustracion(solicitudes, cEstudiante) || !erroresInforme.isEmpty();
            NivelTurnos nivel = clasificarAvance(avance, cEstudiante, totalPreguntasGuia, rutaCompletada, senalesDeApoyo);

            // M-04: tiempo activo en esta clase y clases con el docente
            long minutos = minutosActivos(msgs.stream().map(Mensaje::getCreatedAt).toList());
            if (cEstudiante > 0) {
                minutosActivosTotales += minutos;
            }
            ClasesEstudiante clases = p.getEstudiante() == null ? null : clasesPorEstudiante.get(p.getEstudiante().getId());
            // Sin datos del docente se asume solo esta clase
            long clasesTotal = clases != null ? clases.total() : 1;
            long clasesCompletadas = clases != null ? clases.completadas() : (rutaCompletada ? 1 : 0);

            // Fecha del último mensaje de la conversación (no la de inscripción)
            java.time.OffsetDateTime ultimaFecha = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getCreatedAt();
            String fechaStr = ultimaFecha != null ? ultimaFecha.format(FECHA_FORMATTER) : "Sin mensajes";
            String nombreEst = (p.getEstudiante() != null && p.getEstudiante().getNombre() != null)
                    ? p.getEstudiante().getNombre() : "Estudiante";
            String correoEst = (p.getEstudiante() != null && p.getEstudiante().getCorreo() != null)
                    ? p.getEstudiante().getCorreo() : "";

            // M-04 (RF-08): actividad; más de 3 días sin actividad = "En riesgo" (quien terminó la tutoría nunca lo está)
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
                    .nivelDificultad(nivel.etiqueta())
                    .nivelDificultadClase(nivel.clase())
                    .tramosGuia(tramosParaVista(avance))
                    .solicitudesDirectas(solicitudes)
                    .tasaSolicitudesDirectas(tasaSolicitudes(solicitudes, cEstudiante))
                    .etiquetaSolicitudes(etiquetaSolicitudes.etiqueta())
                    .claseSolicitudes(etiquetaSolicitudes.clase())
                    .estadoActividad(estadoActividad)
                    .estadoActividadColor(estadoActividadColor)
                    .tiempoActivo(cEstudiante > 0 ? formatearMinutos(minutos) : "—")
                    .clasesConElDocente(clasesTotal)
                    .clasesCompletadas(clasesCompletadas)
                    .build());
        }

        long totalMensajes = totalConsultasEstudiantes + totalRespuestasTutor;

        // Promedio de consultas por estudiante (referencia global de M-02), solo entre quienes han escrito
        double promTurnos = estudiantesActivos == 0 ? 0.0 :
                Math.round(((double) totalConsultasEstudiantes / estudiantesActivos) * 10.0) / 10.0;

        // M-01: porcentaje = estudiantes con el error / estudiantes con informe generado; de mayor a menor
        final long baseInformes = diagnosticosGenerados;
        List<FocoDificultad> dificultadesOrdenadas = dificultades.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new FocoDificultad(tituloPorClave.getOrDefault(e.getKey(), e.getKey()), e.getValue(),
                        baseInformes > 0 ? Math.round(e.getValue() * 100.0 / baseInformes) : 0L))
                .collect(Collectors.toList());

        // RF-07: la síntesis guardada en la sesión (no se llama a Gemini al cargar el dashboard)
        String resumenIa = ""; // vacío: el dashboard muestra "Aún no hay interacciones…" o invita a generarla
        String resumenIaFecha = null;
        if (totalConsultasEstudiantes > 0 && GeminiService.esTextoGeneradoValido(sesion.getSintesisMd())) {
            resumenIa = sesion.getSintesisMd();
            resumenIaFecha = sesion.getSintesisFecha() != null ? sesion.getSintesisFecha().format(FECHA_FORMATTER) : null;
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
                .tasaSolicitudesDirectas(tasaSolicitudes(totalRf10, totalConsultasEstudiantes))
                .totalIntervencionesAuditorRf10(totalRf10)
                .dificultadesPorConcepto(dificultadesOrdenadas)
                .diagnosticosGenerados(diagnosticosGenerados)
                .informesPendientes(informesPendientes)
                .turnosPorPreguntaGuia(promediosPorGuia(avances, totalPreguntasGuia))
                .tiempoActivoPromedio(estudiantesActivos == 0 ? "—"
                        : formatearMinutos(Math.round((double) minutosActivosTotales / estudiantesActivos)))
                .resumenIa(resumenIa)
                .resumenIaFecha(resumenIaFecha)
                .participacionesDetalle(detalleList)
                .build();
    }

    /** Máximo de caracteres de mensajes por estudiante sin informe que se envían a la síntesis grupal. */
    static final int MAX_CARACTERES_MENSAJES_POR_ESTUDIANTE = 3000;

    /**
     * RF-07: genera la síntesis pedagógica del grupo con Gemini y la guarda en la sesión (Supabase), para que
     * sobreviva a reinicios y redespliegues. Se invoca con el botón del dashboard y, automáticamente, al terminar
     * "Generar informes pendientes".
     * Fuentes: preguntas guía, métricas de la sesión, el informe individual de cada estudiante que lo tenga
     * y, para quienes no tienen informe, sus propios mensajes (hasta {@value #MAX_CARACTERES_MENSAJES_POR_ESTUDIANTE}
     * caracteres). Los estudiantes se anonimizan ("Estudiante N"). Se piden tres secciones: Diagnóstico Grupal,
     * Focos de Confusión y Atajos, y Recomendaciones para el Aula Presencial.
     * <p>
     * Efectos secundarios: una llamada a la API de Gemini y la escritura de sintesis_md y sintesis_fecha en la
     * sesión (también se actualiza la entidad recibida).
     *
     * @return la síntesis en Markdown
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

            // Anonimización: el número sustituye al nombre y al correo, que no se incluyen en el prompt
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
        java.time.OffsetDateTime ahora = java.time.OffsetDateTime.now();
        // UPDATE directo de las dos columnas; la entidad en memoria se sincroniza a mano
        sesionRepository.guardarSintesis(sesion.getId(), sintesis, ahora);
        sesion.setSintesisMd(sintesis);
        sesion.setSintesisFecha(ahora);
        return sintesis;
    }

    /**
     * M-01: Extrae los títulos (sin repetir) de los errores recurrentes que Gemini identificó en el informe
     * de un estudiante, para contar cuántos estudiantes presentan cada error.
     * Toma las viñetas de la sección "## Errores recurrentes" hasta el siguiente encabezado; de "Título: explicación"
     * usa el título, y descarta las viñetas que niegan errores o son aclaraciones.
     *
     * @return títulos en orden de aparición, con la primera letra en mayúscula; vacío si no hay sección
     */
    static Set<String> extraerErroresRecurrentes(String pMd) {
        Set<String> errores = new LinkedHashSet<>();
        if (pMd == null || !pMd.contains("## Errores recurrentes")) return errores;
        int start = pMd.indexOf("## Errores recurrentes");
        // 22 = longitud de "## Errores recurrentes"
        int nextSection = pMd.indexOf("## ", start + 22);
        String seccionErrores = (nextSection != -1)
                ? pMd.substring(start + 22, nextSection).trim()
                : pMd.substring(start + 22).trim();

        for (String linea : seccionErrores.split("\n")) {
            String l = linea.trim();
            if (l.startsWith("-") || l.startsWith("*")) {
                l = l.substring(1).trim();
            }
            l = l.replace("**", "").replace("*", "").trim();
            // Se omiten líneas vacías, notas entre paréntesis y subencabezados
            if (l.isBlank() || l.startsWith("(") || l.startsWith("#")) continue;

            String concepto;
            if (l.contains(":")) {
                concepto = l.substring(0, l.indexOf(":")).trim();
                // La explicación de un error real puede contener "no … errores" ("no captura los errores de entrada"):
                // en ella solo se descartan las aclaraciones que empiezan negando ("No se observaron errores…")
                if (explicacionNiegaErrores(l.substring(l.indexOf(":") + 1))) continue;
            } else if (niegaErrores(l)) {
                continue;
            } else if (l.length() > 50) {
                // Viñeta sin título: se recorta para que sirva como nombre del error
                concepto = l.substring(0, 50).trim() + "...";
            } else {
                concepto = l;
            }

            // El título también puede negar errores ("**Ninguno observado:** …") o ser una nota ("**Nota:** …")
            if (niegaErrores(concepto) || esTituloAclaratorio(concepto)) continue;

            if (!concepto.isBlank() && concepto.length() >= 3 && concepto.length() <= 65) {
                concepto = Character.toUpperCase(concepto.charAt(0)) + concepto.substring(1);
                errores.add(concepto);
            }
        }
        return errores;
    }

    /**
     * Aclaraciones del informe que niegan errores: "No se observaron errores", "sin errores", "(no un error)",
     * o viñetas que solo dicen "Ninguno", "Ninguno observado", "No aplica".
     */
    private static final java.util.regex.Pattern PATRON_SIN_ERRORES = java.util.regex.Pattern.compile(
            "\\b(no|ningun|ninguna|ninguno|sin)\\b(\\s+\\S+){0,5}\\s+error(es)?\\b"
            + "|^(ninguno|ninguna|no aplica|n a|no se (observ|detect|identific|evidenci)\\w*)(\\s+(observado|observada|observados|detectado|detectada|detectados|identificado|identificada|evidente))?$");

    /**
     * M-01: clave para agrupar títulos de error equivalentes ("Composición vs. agregación" y
     * "composicion vs agregacion" cuentan como el mismo error).
     */
    static String claveError(String titulo) {
        return ChatService.normalizarTexto(titulo == null ? "" : titulo);
    }

    /** Títulos que Gemini usa para aclaraciones y no para errores ("Nota", "Nota de precaución", "Observación"…). */
    private static final java.util.regex.Pattern PATRON_TITULO_ACLARATORIO = java.util.regex.Pattern.compile(
            "^(nota|notas|observacion|observaciones|aclaracion|comentario|comentarios|advertencia|precaucion)( (de|del|general|importante|final|adicional)\\b.*)?$");

    /** M-01: true si el título es una aclaración del informe y no el nombre de un error. */
    static boolean esTituloAclaratorio(String titulo) {
        return PATRON_TITULO_ACLARATORIO.matcher(ChatService.normalizarTexto(titulo)).matches();
    }

    /** M-01: true si la viñeta dice que no hubo errores, en lugar de describir uno. */
    static boolean niegaErrores(String linea) {
        return PATRON_SIN_ERRORES.matcher(ChatService.normalizarTexto(linea)).find();
    }

    /** Explicaciones que solo aclaran que no hubo errores: "No se observaron…", "Sin errores…", "Ninguno…". */
    private static final java.util.regex.Pattern PATRON_EXPLICACION_SIN_ERRORES = java.util.regex.Pattern.compile(
            "^(no se (observ|detect|identific|evidenci)\\w*|no (hubo|hay|presento|presenta|cometio|comete)\\b(\\s+\\S+){0,2}\\s+error(es)?\\b|sin errores\\b|ningun\\w*\\b)");

    /** M-01: true si la explicación tras el título ("Título: explicación") empieza negando que hubiera errores. */
    static boolean explicacionNiegaErrores(String explicacion) {
        return PATRON_EXPLICACION_SIN_ERRORES.matcher(ChatService.normalizarTexto(explicacion).trim()).find();
    }
}
