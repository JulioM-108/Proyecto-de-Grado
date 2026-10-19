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
import org.springframework.core.task.TaskExecutor;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
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

    /** Meta-etiqueta con la que el tutor señala una solicitud de código/solución directa (M-03). */
    public static final String ETIQUETA_ATAJO_DETECTADO = "<!-- ATAJO_DETECTADO -->";

    /** Meta-etiqueta con la que el tutor certifica que el estudiante superó la Pregunta Guía n (M-02): {@code <!-- GUIA_SUPERADA:n -->}. */
    static final Pattern PATRON_GUIA_SUPERADA = Pattern.compile("<!--\\s*GUIA_SUPERADA\\s*:\\s*(\\d+)\\s*-->");

    /** Código de la tabla metrica para los turnos usados en cada pregunta guía (M-02). Concepto: "P3" o "P2-P4" si el tutor omitió marcas. */
    public static final long CODIGO_TURNOS_POR_GUIA = 21L;

    /** Meta-etiqueta con la que el tutor señala que plantea por primera vez la Pregunta Guía #1 (fin de la apertura). */
    public static final String ETIQUETA_GUIA_INICIADA = "<!-- GUIA_INICIADA -->";

    /** Código de la tabla metrica para los turnos de la apertura, antes de la Pregunta Guía #1 (no cuentan en P1 de M-02). */
    public static final long CODIGO_APERTURA = 20L;

    /** Código de la tabla metrica para cada solicitud de respuesta directa (M-03). */
    public static final long CODIGO_ATAJO = 10L;

    /** Concepto con el que se registran las solicitudes directas (los registros antiguos usan "RF-10"). */
    public static final String CONCEPTO_ATAJO = "M-03";

    // Valores que usa el tutor cuando el docente deja vacío un campo CRAFT (el dashboard los muestra tal cual)
    public static final String CRAFT_DEFECTO_CONTEXTO = "Curso de Programación Orientada a Objetos";
    public static final String CRAFT_DEFECTO_ROL = "Tutor socrático";
    public static final String CRAFT_DEFECTO_AUDIENCIA = "Estudiantes de ingeniería";
    public static final String CRAFT_DEFECTO_FORMATO = "Preguntas orientadoras y analogías concisas";
    public static final String CRAFT_DEFECTO_TONO = "Amable, empático y reflexivo";

    /** Turnos mínimos del estudiante antes de aceptar el cierre de la ruta, aunque haya pocas preguntas guía. */
    static final int MIN_TURNOS_CIERRE_RUTA = 4;

    // ─── Detección de atajos cognitivos (M-03) ───────────────────────────────
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
     * o la respuesta directa (alerta de atajo cognitivo, métrica M-03).
     */
    public static boolean esSolicitudAtajo(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return false;
        return PATRON_ATAJO_DIRECTO.matcher(normalizarTexto(mensaje)).find();
    }

    /** Minúsculas, sin tildes/diéresis/ñ y con la puntuación reemplazada por espacios. */
    /** Comentarios HTML (completos o sin cerrar) y nombres de las etiquetas ocultas que solo puede emitir el tutor. */
    private static final Pattern PATRON_COMENTARIO_HTML = Pattern.compile("(?s)<!--.*?(?:-->|$)");
    private static final Pattern PATRON_NOMBRE_ETIQUETA = Pattern.compile(
            "(?i)GUIA_SUPERADA|GUIA_INICIADA|RUTA_COMPLETADA|ATAJO_DETECTADO");

    /**
     * Prepara el mensaje del estudiante: quita comentarios HTML y nombres de etiquetas ocultas para que no pueda
     * inducir al tutor a repetirlas y falsear su avance (M-02) o el cierre de la ruta, y lo recorta a 2500 caracteres.
     */
    static String limpiarMensajeEstudiante(String mensajeTexto) {
        if (mensajeTexto == null) return null;
        String limpio = PATRON_NOMBRE_ETIQUETA.matcher(PATRON_COMENTARIO_HTML.matcher(mensajeTexto).replaceAll(""))
                .replaceAll("")
                .trim();
        if (limpio.isEmpty()) limpio = "…";
        return limpio.length() > 2500 ? limpio.substring(0, 2500) : limpio;
    }

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
    private final TaskExecutor informesExecutor;
    private final AnalyticsService analyticsService;

    /** Sesiones con una generación grupal de informes pendientes en curso (evita lanzarla dos veces). */
    private final Set<UUID> sesionesGenerandoInformes = ConcurrentHashMap.newKeySet();

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
                                 int totalPreguntasGuia, long turnosPreviosEstudiante,
                                 HitosRegistrados hitos) {}

    /** Avance ya registrado en M-02: última pregunta guía superada y turnos ya asignados a preguntas. */
    record HitosRegistrados(int ultimaSuperada, long turnosAsignados, boolean aperturaRegistrada) {
        static final HitosRegistrados NINGUNO = new HitosRegistrados(0, 0, false);

        HitosRegistrados(int ultimaSuperada, long turnosAsignados) {
            this(ultimaSuperada, turnosAsignados, false);
        }
    }

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

        final String mensajeLimpio = limpiarMensajeEstudiante(mensajeTexto);

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
                    mensajeRepository.countByParticipacionAndEmisor(participacion, true),
                    hitosRegistrados(metricaRepository.findByParticipacion(participacion)));
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
        int guiaMarcada = mayorGuiaSuperada(respuestaTutor);
        boolean etiquetaInicio = respuestaTutor.contains(ETIQUETA_GUIA_INICIADA);
        String contenidoLimpio = PATRON_GUIA_SUPERADA.matcher(respuestaTutor
                .replace(ETIQUETA_RUTA_COMPLETADA, "")
                .replace(ETIQUETA_ATAJO_DETECTADO, "")
                .replace(ETIQUETA_GUIA_INICIADA, ""))
                .replaceAll("")
                .trim();

        long turnosEstudiante = contexto.turnosPreviosEstudiante() + 1;
        boolean rutaCompletada = etiquetaRuta && validarCierreDeRuta(
                contexto.participacionId(), respuesta.auxiliar(), contexto.totalPreguntasGuia(), turnosEstudiante);
        boolean atajoDetectado = esSolicitudAtajo(mensajeLimpio) || etiquetaAtajo;
        // M-02: la apertura (saludo y diagnóstico) se registra aparte para no inflar los turnos de P1
        long turnosApertura = turnosApertura(contexto.hitos(), etiquetaInicio, contexto.totalPreguntasGuia(), turnosEstudiante);
        HitosRegistrados hitosPrevios = turnosApertura > 0
                ? new HitosRegistrados(0, contexto.hitos().turnosAsignados() + turnosApertura, true)
                : contexto.hitos();
        List<HitoNuevo> hitosNuevos = calcularHitosNuevos(hitosPrevios, guiaMarcada, respuesta.auxiliar(),
                contexto.totalPreguntasGuia(), turnosEstudiante, rutaCompletada);

        // Fase 3 (Transacción corta ~50ms): persistir el turno completo y sus métricas
        MensajeDto resultado = transactionTemplate.execute(status -> {
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

            // Atajo cognitivo / solicitud de código directo (M-03): regex sobre el mensaje o señal del tutor
            if (atajoDetectado) {
                registrarMetrica(participacion, CODIGO_ATAJO, CONCEPTO_ATAJO, 1L);
                log.info("Solicitud de respuesta directa (M-03) registrada para participación {} (fuente: {})",
                        participacion.getId(), etiquetaAtajo ? "señal del tutor" : "patrón de texto");
            }

            // M-02: turnos de la apertura (una sola vez) y de cada pregunta guía superada en este turno
            if (turnosApertura > 0) {
                registrarMetrica(participacion, CODIGO_APERTURA, "APERTURA", turnosApertura);
            }
            for (HitoNuevo hito : hitosNuevos) {
                registrarMetrica(participacion, CODIGO_TURNOS_POR_GUIA, hito.concepto(), hito.turnos());
            }

            mensajeRepository.save(Mensaje.builder()
                    .emisor(false)
                    .contenido(contenidoLimpio)
                    .participacion(participacion)
                    .build());

            // Completar la ruta cierra la tutoría del estudiante
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

        // Flujo 3: con el turno ya confirmado en BD, el informe se genera en segundo plano (el estudiante no espera)
        if (rutaCompletada) {
            generarInformeEnSegundoPlano(contexto.participacionId());
        }
        return resultado;
    }

    // ─── M-02: turnos por pregunta guía ──────────────────────────────────────

    /** Un registro nuevo de M-02: pregunta(s) superada(s) y turnos que tomó. */
    record HitoNuevo(String concepto, long turnos) {}

    /** Concepto con el que se guarda el hito: "P3", o "P2-P4" si el tutor no marcó las preguntas intermedias. */
    static String conceptoHito(int desde, int hasta) {
        return desde == hasta ? "P" + hasta : "P" + desde + "-P" + hasta;
    }

    /** Última pregunta del concepto ("P3" → 3, "P2-P4" → 4); 0 si no es un hito válido. */
    static int ultimaPreguntaDeHito(String concepto) {
        if (concepto == null) return 0;
        Matcher m = Pattern.compile("^P(\\d+)(?:-P(\\d+))?$").matcher(concepto.trim());
        if (!m.matches()) return 0;
        return Integer.parseInt(m.group(2) != null ? m.group(2) : m.group(1));
    }

    /** Suma lo ya registrado en M-02 (apertura, código 20, y preguntas guía, código 21) de una participación. */
    static HitosRegistrados hitosRegistrados(List<Metrica> metricas) {
        int ultima = 0;
        long turnos = 0;
        boolean apertura = false;
        for (Metrica m : metricas) {
            long valor = m.getValorNumerico() != null ? m.getValorNumerico() : 0;
            if (Long.valueOf(CODIGO_APERTURA).equals(m.getCodigo())) {
                apertura = true;
                turnos += valor;
            } else if (Long.valueOf(CODIGO_TURNOS_POR_GUIA).equals(m.getCodigo())) {
                ultima = Math.max(ultima, ultimaPreguntaDeHito(m.getConcepto()));
                turnos += valor;
            }
        }
        return ultima == 0 && turnos == 0 && !apertura ? HitosRegistrados.NINGUNO : new HitosRegistrados(ultima, turnos, apertura);
    }

    /**
     * Turnos de la apertura que se registran en este turno: solo la primera vez que el tutor marca GUIA_INICIADA,
     * antes de superar cualquier pregunta. Incluye el turno actual (el mensaje del estudiante que precede a la
     * Pregunta Guía #1). Devuelve 0 si no corresponde; sin la marca, P1 incluye la apertura, como antes.
     */
    static long turnosApertura(HitosRegistrados previos, boolean etiquetaInicio, int totalPreguntasGuia, long turnosEstudiante) {
        if (!etiquetaInicio || totalPreguntasGuia <= 0 || previos.aperturaRegistrada() || previos.ultimaSuperada() > 0) {
            return 0;
        }
        return Math.max(0, turnosEstudiante - previos.turnosAsignados());
    }

    /** Mayor número n de las etiquetas GUIA_SUPERADA:n de la respuesta; 0 si no hay. */
    static int mayorGuiaSuperada(String respuesta) {
        int mayor = 0;
        Matcher m = PATRON_GUIA_SUPERADA.matcher(respuesta);
        while (m.find()) {
            try {
                mayor = Math.max(mayor, Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignorado) {
                // número fuera de rango: se ignora la etiqueta
            }
        }
        return mayor;
    }

    /**
     * Decide qué hitos de M-02 se registran en este turno. La marca del tutor solo se acepta si avanza
     * (n mayor que la última registrada), no viene del modelo auxiliar y NO es la última pregunta: la última
     * solo se cierra con RUTA_COMPLETADA (validada en servidor), así que GUIA_SUPERADA:último se ignora para no
     * mostrar un avance falso. Si se saltó preguntas, los turnos se guardan juntos ("P2-P4"). Al completar la
     * ruta se cierran las preguntas restantes.
     */
    static List<HitoNuevo> calcularHitosNuevos(HitosRegistrados previos, int guiaMarcada, boolean modeloAuxiliar,
                                               int totalPreguntasGuia, long turnosEstudiante, boolean rutaCompletada) {
        List<HitoNuevo> nuevos = new ArrayList<>();
        if (totalPreguntasGuia <= 0) return nuevos;
        int ultima = previos.ultimaSuperada();
        long asignados = previos.turnosAsignados();

        if (!modeloAuxiliar && guiaMarcada > ultima && guiaMarcada < totalPreguntasGuia) {
            nuevos.add(new HitoNuevo(conceptoHito(ultima + 1, guiaMarcada), Math.max(0, turnosEstudiante - asignados)));
            asignados = Math.max(asignados, turnosEstudiante);
            ultima = guiaMarcada;
        }
        // Al completar la ruta se cierran las preguntas restantes (si tuvieron turnos propios)
        if (rutaCompletada && ultima < totalPreguntasGuia && turnosEstudiante > asignados) {
            nuevos.add(new HitoNuevo(conceptoHito(ultima + 1, totalPreguntasGuia), turnosEstudiante - asignados));
        }
        return nuevos;
    }

    // ─── Flujo 3: informes automáticos ───────────────────────────────────────

    /**
     * Encola la generación del informe (progreso_md) de una participación que completó la ruta. Lo regenera
     * aunque ya exista uno, porque pudo generarse a mitad de la conversación con "Generar informes pendientes".
     * Si Gemini falla o la cola está llena no pasa nada: el turno del estudiante ya quedó guardado y el docente
     * sigue teniendo "Generar informes pendientes" y el botón individual.
     */
    public void generarInformeEnSegundoPlano(UUID participacionId) {
        try {
            informesExecutor.execute(() -> generarInformeSeguro(participacionId, true));
        } catch (RuntimeException e) {
            log.warn("No se pudo encolar el informe de la participación {}: {}", participacionId, e.getMessage());
        }
    }

    private void generarInformeSeguro(UUID participacionId, boolean regenerar) {
        try {
            generarDiagnostico(participacionId, regenerar);
        } catch (Exception e) {
            log.warn("No se pudo generar automáticamente el informe de la participación {}: {}", participacionId, e.getMessage());
        }
    }

    /**
     * Genera, uno por uno y en segundo plano, los informes de los estudiantes de la sesión que escribieron
     * al tutor y aún no tienen informe.
     *
     * @return cuántos informes quedaron en cola; -1 si ya hay una generación en curso para esta sesión
     */
    public int generarInformesPendientes(Sesion sesion) {
        if (!sesionesGenerandoInformes.add(sesion.getId())) {
            return -1;
        }
        List<UUID> pendientes;
        try {
            pendientes = transactionTemplate.execute(status -> {
                List<Participacion> sinInforme = participacionRepository.findBySesion(sesion).stream()
                        .filter(p -> !GeminiService.esTextoGeneradoValido(p.getProgresoMd()))
                        .toList();
                if (sinInforme.isEmpty()) return List.<UUID>of();
                // Una sola consulta agrupada: participaciones con al menos un mensaje del estudiante
                Set<Object> conMensajes = mensajeRepository.findUltimoMensajeEstudiantePorParticipacion(sinInforme).stream()
                        .map(fila -> fila[0])
                        .collect(Collectors.toSet());
                return sinInforme.stream()
                        .map(Participacion::getId)
                        .filter(conMensajes::contains)
                        .toList();
            });
        } catch (RuntimeException e) {
            sesionesGenerandoInformes.remove(sesion.getId());
            throw e;
        }
        if (pendientes == null || pendientes.isEmpty()) {
            sesionesGenerandoInformes.remove(sesion.getId());
            return 0;
        }
        try {
            informesExecutor.execute(() -> {
                try {
                    pendientes.forEach(id -> generarInformeSeguro(id, false));
                    log.info("Informes pendientes generados para la sesión {} ({} participaciones).", sesion.getId(), pendientes.size());
                    // RF-07: con todos los informes listos, la síntesis grupal se actualiza sola
                    try {
                        analyticsService.generarYGuardarSintesisIa(sesion);
                        log.info("Síntesis grupal actualizada automáticamente para la sesión {}.", sesion.getId());
                    } catch (Exception e) {
                        log.warn("No se pudo actualizar automáticamente la síntesis de la sesión {}: {}", sesion.getId(), e.getMessage());
                    }
                } finally {
                    sesionesGenerandoInformes.remove(sesion.getId());
                }
            });
        } catch (RuntimeException e) {
            sesionesGenerandoInformes.remove(sesion.getId());
            throw e;
        }
        return pendientes.size();
    }

    /** Indica si hay una generación grupal de informes en curso para la sesión. */
    public boolean estaGenerandoInformes(UUID sesionId) {
        return sesionesGenerandoInformes.contains(sesionId);
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

    /**
     * Instrucción del informe individual (progreso_md). Para que M-01 agrupe el mismo error de distintos
     * estudiantes, se piden títulos cortos y estandarizados en "Errores recurrentes", nombrados con los conceptos
     * de las preguntas guía de la clase (que se incluyen como referencia).
     */
    static String construirInstruccionInforme(String preguntasGuia, String historialTexto) {
        String guias = (preguntasGuia != null && !preguntasGuia.isBlank())
                ? "PREGUNTAS GUÍA DE LA CLASE (objetivos; úsalas como referencia para nombrar los conceptos):\n"
                  + numerarPreguntasGuia(preguntasGuia) + "\n"
                : "";
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

                REGLAS PARA "Errores recurrentes" (los títulos se comparan entre estudiantes para contar errores comunes):
                - Cada viñeta empieza con un título corto en negrita, de 2 a 5 palabras, seguido de dos puntos y una \
                explicación breve. Ejemplo: - **Composición vs. agregación:** cree que siempre conviene la composición.
                - El título nombra SOLO el concepto mal entendido, con los términos de las preguntas guía. Debe ser \
                genérico y reutilizable: sin adjetivos y sin palabras como "sesgo", "error", "confusión" o "dificultad".
                - Un concepto por viñeta; no repitas el mismo concepto con otro título.
                - Si no hubo errores, escribe únicamente: - Ninguno.

                """ + guias + "HISTORIAL:\n" + historialTexto;
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
            return (p != null && GeminiService.esTextoGeneradoValido(p.getProgresoMd())) ? p.getProgresoMd() : null;
        });
        if (progresoExistente != null && !regenerar) {
            log.info("Participación {} ya cuenta con progreso_md en BD. Omitiendo invocación redundante a Gemini.", participacionId);
            return progresoExistente;
        }

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

            String preguntasGuia = p.getSesion() != null ? p.getSesion().getPreguntasGuia() : null;
            return construirInstruccionInforme(preguntasGuia, historialTexto);
        });

        // Fase 2: Llamada a Gemini fuera de transacción JDBC
        String progreso = geminiService.generarRespuesta(instruccion);
        if (!GeminiService.esTextoGeneradoValido(progreso)) {
            throw new IllegalStateException("El motor de IA no pudo generar el diagnóstico pedagógico en este momento.");
        }

        // Fase 3: Persistir el informe en transacción corta
        transactionTemplate.executeWithoutResult(status -> {
            Participacion p = participacionRepository.findById(participacionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + participacionId));
            p.setProgresoMd(progreso);
            participacionRepository.save(p);
            log.info("Flujo 3: síntesis pedagógica generada para participación {}", participacionId);
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
                orDefault(contexto,  CRAFT_DEFECTO_CONTEXTO),
                orDefault(rol,       CRAFT_DEFECTO_ROL),
                orDefault(audiencia, CRAFT_DEFECTO_AUDIENCIA),
                orDefault(formato,   CRAFT_DEFECTO_FORMATO),
                orDefault(tono,      CRAFT_DEFECTO_TONO),
                orDefault(tema,      "Fundamentos de POO")
        );

        // Agregar preguntas guía numeradas (el número es el que el tutor usa en GUIA_SUPERADA:n)
        if (preguntasGuia != null && !preguntasGuia.isBlank()) {
            textoBase += "\n## Preguntas Guía del Docente\n"
                    + "Úsalas como hoja de ruta socrática obligatoria, en este orden:\n"
                    + numerarPreguntasGuia(preguntasGuia)
                    + RECORDATORIO_ETIQUETAS_GUIA;
        }

        return textoBase;
    }

    /**
     * Recordatorio junto a las preguntas guía (M-02): el modelo cumple mejor la regla de la Capa 1 cuando la
     * instrucción está cerca de los datos a los que se refiere. Reduce las etiquetas GUIA_SUPERADA olvidadas.
     */
    static final String RECORDATORIO_ETIQUETAS_GUIA = """

            Recordatorio interno: cada vez que des por superada la pregunta guía n y pases a la n+1, cierra tu \
            mensaje con <!-- GUIA_SUPERADA:n --> en una línea independiente. Al superar la ÚLTIMA pregunta no uses \
            esa etiqueta: aplica la Culminación de la Ruta con <!-- RUTA_COMPLETADA -->.
            """;

    /** Una pregunta guía por línea, numeradas desde 1 (quita la numeración que haya escrito el docente). */
    static String numerarPreguntasGuia(String preguntasGuia) {
        List<String> lineas = preguntasGuia.lines().map(String::trim).filter(l -> !l.isBlank()).toList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            String texto = lineas.get(i).replaceFirst("^\\d+\\s*[.):-]\\s*", "");
            sb.append(i + 1).append(". ").append(texto).append("\n");
        }
        return sb.toString();
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
