package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import Prototipo.Proyecto_Grado.model.dto.MensajeDto;
import Prototipo.Proyecto_Grado.model.dto.PromptCapasDto;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.ProfesorEstudiante;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorEstudianteRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio central del Módulo de Tutoría Socrática.
 * Coordina la arquitectura de 3 capas de prompts, el llamado a Gemini
 * y la persistencia en las tablas participacion → mensaje y metrica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    /** Meta-etiqueta con la que el tutor certifica que el estudiante superó la última Pregunta Guía. */
    public static final String ETIQUETA_RUTA_COMPLETADA = "<!-- RUTA_COMPLETADA -->";

    /** Meta-etiqueta con la que el tutor señala una solicitud de código/solución directa (RF-10). */
    public static final String ETIQUETA_ATAJO_DETECTADO = "<!-- ATAJO_DETECTADO -->";

    /** Turnos mínimos del estudiante antes de aceptar el cierre de la ruta, aunque haya pocas preguntas guía. */
    static final int MIN_TURNOS_CIERRE_RUTA = 4;

    // ─── Detección de atajos cognitivos (RF-10) ──────────────────────────────
    // Se aplica sobre texto normalizado (minúsculas, sin tildes ni puntuación), por lo que
    // "pásame", "pasame" y "PÁSAME" son equivalentes.

    /** Verbos imperativos / subjuntivos con los que se pide algo directamente ("dame", "que me des"). */
    private static final String VERBOS_PETICION =
            "da|dame|danos|des|di|dime|decime|decinos|pasa|pasame|pases|haz|hazme|hagas|"
            + "escribe|escribeme|escribime|escribas|genera|generame|generes|muestra|muestrame|mostrame|muestres|"
            + "ensename|resuelve|resuelveme|resuelvas|completa|completame|termina|terminame|quiero|necesito|exijo|"
            + "darme|pasarme|hacerme|escribirme|generarme|mostrarme|decirme|resolverme|ensenarme";

    /** Infinitivos que solo cuentan tras un modal ("me puedes dar…", "podrías escribir…"). */
    private static final String INFINITIVOS_CON_MODAL =
            "(?:puedes|podrias|podes|puede|podria|vas a|va a)\\s+(?:me\\s+)?"
            + "(?:dar|pasar|hacer|escribir|generar|mostrar|decir|resolver|ensenar)(?:me|nos)?";

    /** Palabras de relleno admitidas entre el verbo y el objeto (máx. 4): "dame directamente la …". */
    private static final String RELLENO =
            "(?:me|te|nos|lo|la|el|los|las|un|una|unos|unas|todo|toda|directamente|directo|ya|solo|solamente|"
            + "simplemente|de una vez|de una|una vez|completo|completa|entero|entera|final|correcto|correcta|"
            + "resuelto|resuelta|hecho|hecha|porfa|por favor|tu|mejor|ahora)";

    private static final String OBJETOS_SOLUCION =
            "(?:codigo|codigos|solucion|soluciones|respuesta|respuestas|programa|algoritmo|ejercicio|tarea|"
            + "implementacion|taller|resolucion)";

    /** Expresiones completas que por sí solas evidencian la búsqueda de un atajo. */
    private static final String FRASES_ATAJO =
            "no quiero (?:mas |tantas )?preguntas|sin (?:mas |tantas )?preguntas|"
            + "deja(?:te)? de (?:preguntar|hacer preguntas|preguntas)|"
            + "dime como (?:se hace|hacerlo|lo hago|se resuelve|resolverlo)|"
            + "damelo|hazmelo|hacemelo|pasamelo|escribemelo|resuelvemelo|hazlo tu|hacelo vos|resuelvelo|"
            + "cual es (?:el |la )?(?:codigo|solucion|respuesta)|como (?:es|seria) (?:el |la )?(?:codigo|solucion)";

    static final Pattern PATRON_ATAJO_DIRECTO = Pattern.compile(
            "\\b(?:(?:" + VERBOS_PETICION + ")|" + INFINITIVOS_CON_MODAL + ")"
            + "(?:\\s+" + RELLENO + "){0,4}\\s+" + OBJETOS_SOLUCION + "\\b"
            + "|\\b(?:" + FRASES_ATAJO + ")\\b"
    );

    /**
     * Indica si el mensaje del estudiante solicita explícitamente el código, la solución
     * o la respuesta directa (alerta de atajo cognitivo RF-10 / métrica M-04).
     */
    public static boolean esSolicitudAtajo(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return false;
        return PATRON_ATAJO_DIRECTO.matcher(normalizarTexto(mensaje)).find();
    }

    /** Minúsculas, sin tildes/diéresis/ñ y con la puntuación reemplazada por espacios. */
    static String normalizarTexto(String texto) {
        String sinTildes = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private final GeminiService geminiService;
    private final EstudianteRepository estudianteRepository;
    private final SesionRepository sesionRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
    private final ProfesorEstudianteRepository profesorEstudianteRepository;
    private final TransactionTemplate transactionTemplate;

    // ─── Capa 1: cargada dinámicamente desde resources ───────────────────────
    private String capa1ReglasSistema;

    @PostConstruct
    public void cargarCapa1() {
        try {
            Resource resource = new ClassPathResource("prompts/capa1_socratico.md");
            this.capa1ReglasSistema = new String(
                    resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("Capa 1 cargada desde resources ({} caracteres)", capa1ReglasSistema.length());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo cargar prompts/capa1_socratico.md desde resources", e);
        }
    }

    // ─── Procesar mensaje del estudiante ─────────────────────────────────────

    private record PromptContext(String promptFinal, UUID participacionId,
                                 int totalPreguntasGuia, long turnosPreviosEstudiante) {}

    /**
     * Procesa una consulta del estudiante y genera la respuesta socrática.
     * Flujo desacoplado:
     * 1. Transacción corta de lectura: compila el prompt de 3 capas.
     * 2. Sin transacción: llamada HTTP externa a Gemini (evita agotar el pool HikariCP).
     *    Si ningún modelo responde, no se persiste nada y el estudiante puede reintentar.
     * 3. Transacción corta: guarda el mensaje del estudiante, la respuesta del tutor y las métricas.
     *
     * @param correoEstudiante correo institucional del estudiante
     * @param enlaceSesion     enlace único de la sesión (identifica la Sesion activa)
     * @param mensajeTexto     texto enviado por el estudiante
     * @param craftDocente     configuración CRAFT del docente (puede ser null para usar la de la Sesion)
     */
    public MensajeDto procesarMensajeEstudiante(String correoEstudiante,
                                                String enlaceSesion,
                                                String mensajeTexto,
                                                CraftConfigDto craftDocente) {

        final String mensajeLimpio = (mensajeTexto != null && mensajeTexto.length() > 2500)
                ? mensajeTexto.substring(0, 2500)
                : mensajeTexto;

        final OffsetDateTime recibidoEn = OffsetDateTime.now();

        // Fase 1 (Transacción corta ~50ms): ensamblar el prompt (solo lectura)
        PromptContext contexto = transactionTemplate.execute(status -> {
            Estudiante estudiante = estudianteRepository.findByCorreoIgnoreCase(correoEstudiante)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Estudiante no encontrado: " + correoEstudiante));
            Sesion sesion = sesionRepository.findByEnlaceUnico(enlaceSesion)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Sesión no encontrada con enlace: " + enlaceSesion));

            Participacion participacion = participacionRepository
                    .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                    .orElseThrow(() -> new IllegalStateException(
                            "El estudiante " + correoEstudiante + " no cuenta con una participación activa en la sesión " + enlaceSesion));

            PromptCapasDto promptCapas = PromptCapasDto.builder()
                    .capa1Sistema(capa1ReglasSistema)
                    .capa2DocenteCraft(construirCapaDocente(sesion, craftDocente))
                    .capa3EstudianteDinamica(construirCapa3(participacion))
                    .build();

            String preguntasGuia = craftDocente != null ? craftDocente.getPreguntasGuia() : sesion.getPreguntasGuia();
            String promptFinal = promptCapas.compilarPromptFinal(mensajeLimpio);
            return new PromptContext(promptFinal, participacion.getId(),
                    contarPreguntasGuia(preguntasGuia),
                    mensajeRepository.countByParticipacionAndEmisor(participacion, true));
        });

        if (contexto == null) {
            throw new IllegalStateException("Error al inicializar el contexto de la consulta socrática.");
        }

        // Fase 2 (FUERA de transacción JDBC, 5-35s): llamada HTTP a Gemini
        GeminiService.RespuestaGemini respuesta = geminiService.generarRespuestaDetallada(contexto.promptFinal());

        if (respuesta.error()) {
            // Nada se persiste: el texto de error no debe quedar en el historial ni contaminar el diagnóstico.
            log.warn("Gemini no respondió para la participación {}. El estudiante podrá reintentar su mensaje.",
                    contexto.participacionId());
            return MensajeDto.builder()
                    .emisor("TUTOR")
                    .contenido(GeminiService.MENSAJE_ERROR_TEMPORAL)
                    .errorIa(true)
                    .build();
        }

        String respuestaTutor = respuesta.texto();
        boolean etiquetaRuta = respuestaTutor.contains(ETIQUETA_RUTA_COMPLETADA);
        boolean etiquetaAtajo = respuestaTutor.contains(ETIQUETA_ATAJO_DETECTADO);
        String contenidoLimpio = respuestaTutor
                .replace(ETIQUETA_RUTA_COMPLETADA, "")
                .replace(ETIQUETA_ATAJO_DETECTADO, "")
                .trim();

        long turnosEstudiante = contexto.turnosPreviosEstudiante() + 1;
        boolean rutaCompletada = etiquetaRuta && validarCierreDeRuta(
                contexto.participacionId(), respuesta.auxiliar(), contexto.totalPreguntasGuia(), turnosEstudiante);
        boolean atajoDetectado = esSolicitudAtajo(mensajeLimpio) || etiquetaAtajo;

        // Fase 3 (Transacción corta ~50ms): persistir el turno completo y sus métricas
        return transactionTemplate.execute(status -> {
            Participacion participacion = participacionRepository.findById(contexto.participacionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + contexto.participacionId()));

            mensajeRepository.save(Mensaje.builder()
                    .emisor(true)
                    .contenido(mensajeLimpio)
                    .participacion(participacion)
                    .createdAt(recibidoEn)
                    .build());

            long totalMensajes = mensajeRepository.countByParticipacion(participacion);
            registrarMetrica(participacion, 2L, participacion.getSesion().getTema(), totalMensajes);

            // Atajo cognitivo / solicitud de código directo (RF-10): regex sobre el mensaje o señal del tutor
            if (atajoDetectado) {
                registrarMetrica(participacion, 10L, "RF-10", 1L);
                log.info("Alerta de Atajo Cognitivo (RF-10) registrada para participación {} (fuente: {})",
                        participacion.getId(), etiquetaAtajo ? "señal del tutor" : "patrón de texto");
            }

            mensajeRepository.save(Mensaje.builder()
                    .emisor(false)
                    .contenido(contenidoLimpio)
                    .participacion(participacion)
                    .build());

            // Completar la ruta cierra la tutoría del estudiante; el diagnóstico (progreso_md)
            // solo lo genera el docente manualmente desde el dashboard.
            if (rutaCompletada) {
                participacion.setRutaCompletada(true);
                participacionRepository.save(participacion);
                log.info("Ruta de aprendizaje socrática completada para participación {}.", participacion.getId());
            }

            return MensajeDto.builder()
                    .emisor("TUTOR")
                    .contenido(contenidoLimpio)
                    .sesionCompletada(rutaCompletada)
                    .build();
        });
    }

    /**
     * Validación en servidor de la meta-etiqueta RUTA_COMPLETADA: el cierre definitivo de la
     * tutoría no puede depender solo del criterio del modelo. Se rechaza si respondió el modelo
     * auxiliar (menos fiable con reglas condicionales), si la sesión no tiene preguntas guía
     * o si el estudiante no ha tenido turnos suficientes para recorrerlas.
     */
    private boolean validarCierreDeRuta(UUID participacionId, boolean modeloAuxiliar,
                                        int totalPreguntasGuia, long turnosEstudiante) {
        if (modeloAuxiliar) {
            log.warn("RUTA_COMPLETADA ignorada en participación {}: la respuesta provino del modelo auxiliar.", participacionId);
            return false;
        }
        if (totalPreguntasGuia == 0) {
            log.warn("RUTA_COMPLETADA ignorada en participación {}: la sesión no tiene preguntas guía.", participacionId);
            return false;
        }
        long minimo = Math.max(MIN_TURNOS_CIERRE_RUTA, totalPreguntasGuia);
        if (turnosEstudiante < minimo) {
            log.warn("RUTA_COMPLETADA ignorada en participación {}: {} turnos del estudiante (mínimo {}).",
                    participacionId, turnosEstudiante, minimo);
            return false;
        }
        return true;
    }

    /** Cuenta las preguntas guía (una por línea no vacía, igual que el dashboard docente). */
    static int contarPreguntasGuia(String preguntasGuia) {
        if (preguntasGuia == null || preguntasGuia.isBlank()) return 0;
        return (int) preguntasGuia.lines().filter(l -> !l.isBlank()).count();
    }

    // ─── Flujo 3: Cierre de Sesión y Síntesis Pedagógica ─────────────────────

    /**
     * Genera el diagnóstico pedagógico de una participación. Solo se invoca manualmente desde el
     * dashboard docente. Llama a Gemini con el historial completo y persiste el Markdown de 4 secciones
     * en participacion.progreso_md. La llamada a Gemini se ejecuta fuera de transacción.
     * Generar el diagnóstico NO cierra la tutoría del estudiante (ver Participacion#isTutoriaFinalizada).
     *
     * @return el diagnóstico Markdown guardado (o el que ya existía)
     * @throws IllegalStateException si el motor de IA no pudo responder; en ese caso no se guarda nada
     */
    public String cerrarSesionYGenerarProgreso(UUID participacionId) {
        return generarDiagnostico(participacionId, false);
    }

    /**
     * @param regenerar true para volver a generar el diagnóstico con el historial actual aunque ya exista uno
     *                  (lo reemplaza); false para reutilizar el existente sin llamar a Gemini.
     */
    public String generarDiagnostico(UUID participacionId, boolean regenerar) {
        String progresoExistente = transactionTemplate.execute(status -> {
            Participacion p = participacionRepository.findById(participacionId).orElse(null);
            return (p != null && !GeminiService.esRespuestaDeError(p.getProgresoMd())) ? p.getProgresoMd() : null;
        });
        if (progresoExistente != null && !regenerar) {
            log.info("Participación {} ya cuenta con progreso_md en BD. Omitiendo invocación redundante a Gemini.", participacionId);
            return progresoExistente;
        }
        final boolean primerDiagnostico = progresoExistente == null;

        // Fase 1: Recuperar datos del historial
        String instruccion = transactionTemplate.execute(status -> {
            Participacion p = participacionRepository.findById(participacionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + participacionId));

            List<Mensaje> historial = mensajeRepository
                    .findByParticipacionOrderByCreatedAtAsc(p);

            String historialTexto = historial.stream()
                    .map(m -> (Boolean.TRUE.equals(m.getEmisor()) ? "[ESTUDIANTE]" : "[TUTOR]")
                            + ": " + m.getContenido())
                    .collect(Collectors.joining("\n"));

            return """
                    Analiza el siguiente historial de conversación entre un tutor socrático \
                    y un estudiante de POO. Genera un resumen pedagógico en Markdown con \
                    exactamente estas 4 secciones:

                    ## Conceptos dominados
                    (lista de conceptos que el estudiante demostró comprender)

                    ## Conceptos en progreso
                    (conceptos donde el estudiante avanzó pero aún no consolida)

                    ## Errores recurrentes
                    (patrones de error repetidos o confusiones persistentes)

                    ## Recomendación para la siguiente sesión
                    (qué explorar o reforzar en la próxima interacción)

                    HISTORIAL:
                    """ + historialTexto;
        });

        // Fase 2: Llamada a Gemini fuera de transacción JDBC
        String progreso = geminiService.generarRespuesta(instruccion);
        if (GeminiService.esRespuestaDeError(progreso)) {
            throw new IllegalStateException("El motor de IA no pudo generar el diagnóstico pedagógico en este momento.");
        }

        // Fase 3: Persistir resumen y métricas en transacción corta
        transactionTemplate.executeWithoutResult(status -> {
            Participacion p = participacionRepository.findById(participacionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + participacionId));

            List<Mensaje> historial = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(p);
            p.setProgresoMd(progreso);
            participacionRepository.save(p);

            // Marcas del primer diagnóstico; al regenerar no se duplican
            if (primerDiagnostico) {
                registrarMetrica(p, 3L, "sesion_cerrada", (long) historial.size());
                registrarMetrica(p, 5L, "turnos_totales", (long) historial.size());
            }

            log.info("Flujo 3: síntesis pedagógica generada para participación {} ({} turnos)",
                    participacionId, historial.size());
        });
        return progreso;
    }

    // ─── Historial ────────────────────────────────────────────────────────────

    /**
     * Recupera el historial de mensajes de la participación activa de un estudiante en una sesión.
     */
    @Transactional(readOnly = true)
    public List<MensajeDto> obtenerHistorial(String correoEstudiante, String enlaceSesion) {
        Estudiante estudiante = estudianteRepository.findByCorreoIgnoreCase(correoEstudiante).orElse(null);
        if (estudiante == null) return new ArrayList<>();

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlaceSesion).orElse(null);
        if (sesion == null) return new ArrayList<>();

        Participacion participacion = participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .orElse(null);
        if (participacion == null) return new ArrayList<>();

        List<Mensaje> mensajes = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(participacion);
        List<MensajeDto> historial = new ArrayList<>();

        for (Mensaje m : mensajes) {
            historial.add(MensajeDto.builder()
                    .emisor(Boolean.TRUE.equals(m.getEmisor()) ? "ESTUDIANTE" : "TUTOR")
                    .contenido(m.getContenido())
                    .build());
        }
        return historial;
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Construye la Capa 3 del prompt con el historial COMPLETO de la sesión actual (misma participación),
     * para que el tutor retome todo lo trabajado si el estudiante sale y vuelve a entrar.
     * No incluye progreso_md: es el diagnóstico del docente y no debe inyectarse a mitad de la tutoría.
     */
    private String construirCapa3(Participacion participacion) {
        List<Mensaje> historial = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(participacion);
        if (historial.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("## Historial de la sesión actual\n");
        for (Mensaje m : historial) {
            String rol = Boolean.TRUE.equals(m.getEmisor()) ? "ESTUDIANTE" : "TUTOR";
            sb.append("[").append(rol).append("]: ").append(m.getContenido()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Construye la Capa 2 del prompt usando la configuración CRAFT.
     * Prioridad: craftDocente (parámetro) > campos CRAFT almacenados en la Sesion.
     * Incluye preguntas guía si están definidas.
     */
    private String construirCapaDocente(Sesion sesion, CraftConfigDto craft) {
        String contexto  = craft != null ? craft.getContext()   : sesion.getCraftContexto();
        String rol       = craft != null ? craft.getRole()      : sesion.getCraftRol();
        String audiencia = craft != null ? craft.getAudience()  : sesion.getCraftAudiencia();
        String formato   = craft != null ? craft.getFormat()    : sesion.getCraftFormato();
        String tono      = craft != null ? craft.getTone()      : sesion.getCraftTono();
        String tema      = craft != null ? craft.getTemaActualPoo() : sesion.getTema();
        String preguntasGuia = craft != null ? craft.getPreguntasGuia() : sesion.getPreguntasGuia();

        String textoBase = """
            Contexto de la clase: %s
            Rol pedagógico: %s
            Audiencia: %s
            Formato deseado: %s
            Tono: %s
            Tema POO: %s
            """.formatted(
                orDefault(contexto,  "Curso de Programación Orientada a Objetos"),
                orDefault(rol,       "Tutor socrático"),
                orDefault(audiencia, "Estudiantes de ingeniería"),
                orDefault(formato,   "Preguntas orientadoras y analogías concisas"),
                orDefault(tono,      "Amable, empático y reflexivo"),
                orDefault(tema,      "Fundamentos de POO")
        );

        // Agregar preguntas guía si están definidas
        if (preguntasGuia != null && !preguntasGuia.isBlank()) {
            textoBase += "\n## Preguntas Guía del Docente\n"
                    + "Úsalas como hoja de ruta socrática obligatoria:\n"
                    + preguntasGuia;
        }

        return textoBase;
    }

    private String orDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Crea una nueva Participacion (vínculo estudiante ↔ sesión).
     * Público para que ChatController pueda invocarlo al registrar un estudiante.
     */
    public Participacion crearParticipacion(Estudiante estudiante, Sesion sesion) {
        Participacion nueva = Participacion.builder()
                .estudiante(estudiante)
                .sesion(sesion)
                .progresoMd("")
                .build();
        Participacion guardada = participacionRepository.save(nueva);

        if (sesion != null && sesion.getProfesor() != null && estudiante != null) {
            Profesor prof = sesion.getProfesor();
            if (!profesorEstudianteRepository.existsByProfesorAndEstudiante(prof, estudiante)) {
                profesorEstudianteRepository.save(ProfesorEstudiante.builder()
                        .profesor(prof)
                        .estudiante(estudiante)
                        .build());
            }
        }

        return guardada;
    }

    /**
     * Registra una métrica numérica asociada a una participación.
     * Todas las métricas son numéricas (no se usa valorBooleano).
     */
    private void registrarMetrica(Participacion participacion, Long codigo,
                                   String concepto, Long valorNumerico) {
        metricaRepository.save(Metrica.builder()
                .codigo(codigo)
                .concepto(concepto)
                .valorNumerico(valorNumerico)
                .participacion(participacion)
                .build());
    }
}
