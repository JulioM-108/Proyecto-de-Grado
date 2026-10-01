package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import Prototipo.Proyecto_Grado.model.dto.MensajeDto;
import Prototipo.Proyecto_Grado.model.dto.PromptCapasDto;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    private final GeminiService geminiService;
    private final EstudianteRepository estudianteRepository;
    private final SesionRepository sesionRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
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

    private record PromptContext(String promptFinal, UUID participacionId) {}

    /**
     * Procesa una consulta del estudiante y genera la respuesta socrática.
     * Flujo desacoplado:
     * 1. Transacción corta: guarda mensaje del estudiante y compila prompt.
     * 2. Sin transacción: llamada HTTP externa a Gemini (evita agotar el pool HikariCP).
     * 3. Transacción corta: guarda respuesta del tutor.
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

        // Fase 1 (Transacción corta ~50ms): persistir mensaje de estudiante y ensamblar prompt
        PromptContext contexto = transactionTemplate.execute(status -> {
            Estudiante estudiante = estudianteRepository.findByCorreoIgnoreCase(correoEstudiante)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Estudiante no encontrado: " + correoEstudiante));
            Sesion sesion = sesionRepository.findByEnlaceUnico(enlaceSesion)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Sesión no encontrada con enlace: " + enlaceSesion));

            Participacion participacion = participacionRepository
                    .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                    .orElseGet(() -> crearParticipacion(estudiante, sesion));

            mensajeRepository.save(Mensaje.builder()
                    .emisor(true)
                    .contenido(mensajeTexto)
                    .participacion(participacion)
                    .build());

            long turnosEstudiante = mensajeRepository.countByParticipacion(participacion);
            registrarMetrica(participacion, 2L, sesion.getTema(), turnosEstudiante);

            PromptCapasDto promptCapas = PromptCapasDto.builder()
                    .capa1Sistema(capa1ReglasSistema)
                    .capa2DocenteCraft(construirCapaDocente(sesion, craftDocente))
                    .capa3EstudianteDinamica(construirCapa3(participacion))
                    .build();

            String promptFinal = promptCapas.compilarPromptFinal(mensajeTexto);
            return new PromptContext(promptFinal, participacion.getId());
        });

        if (contexto == null) {
            throw new IllegalStateException("Error al inicializar el contexto de la consulta socrática.");
        }

        // Fase 2 (FUERA de transacción JDBC, 5-35s): llamada HTTP a Gemini
        String respuestaTutor = geminiService.generarRespuesta(contexto.promptFinal());

        // Detección automática del fin de la ruta pedagógica (Preguntas Guía completadas)
        boolean rutaCompletada = respuestaTutor != null && respuestaTutor.contains("<!-- RUTA_COMPLETADA -->");
        String contenidoLimpio = respuestaTutor != null
                ? respuestaTutor.replace("<!-- RUTA_COMPLETADA -->", "").trim()
                : "";

        // Fase 3 (Transacción corta ~50ms): persistir respuesta del tutor
        MensajeDto dto = transactionTemplate.execute(status -> {
            Participacion participacion = participacionRepository.findById(contexto.participacionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + contexto.participacionId()));

            mensajeRepository.save(Mensaje.builder()
                    .emisor(false)
                    .contenido(contenidoLimpio)
                    .participacion(participacion)
                    .build());

            return MensajeDto.builder()
                    .emisor("TUTOR")
                    .contenido(contenidoLimpio)
                    .reformuladoPorAuditor(false)
                    .sesionCompletada(rutaCompletada)
                    .build();
        });

        // Si el tutor determinó que se completaron todas las preguntas guía, generar la síntesis inmediatamente
        if (rutaCompletada) {
            try {
                log.info("Ruta de aprendizaje socrática completada para participación {}. Generando progreso_md automáticamente.",
                        contexto.participacionId());
                cerrarSesionYGenerarProgreso(contexto.participacionId());
            } catch (Exception e) {
                log.warn("Error al generar automáticamente progreso_md tras completar ruta socrática: {}", e.getMessage());
            }
        }

        return dto;
    }

    // ─── Flujo 3: Cierre de Sesión y Síntesis Pedagógica ─────────────────────

    /**
     * Genera la síntesis pedagógica al cerrar la sesión.
     * Llama a Gemini con el historial completo y persiste el Markdown de 4 secciones
     * en participacion.progreso_md. La llamada a Gemini se ejecuta fuera de transacción.
     */
    public void cerrarSesionYGenerarProgreso(UUID participacionId) {
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

        // Fase 3: Persistir resumen y métricas en transacción corta
        transactionTemplate.executeWithoutResult(status -> {
            Participacion p = participacionRepository.findById(participacionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Participación no encontrada: " + participacionId));

            List<Mensaje> historial = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(p);
            p.setProgresoMd(progreso);
            participacionRepository.save(p);

            registrarMetrica(p, 3L, "sesion_cerrada", (long) historial.size());
            registrarMetrica(p, 5L, "turnos_totales", (long) historial.size());

            log.info("Flujo 3: síntesis pedagógica generada para participación {} ({} turnos)",
                    participacionId, historial.size());
        });
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
                    .reformuladoPorAuditor(false)
                    .build());
        }
        return historial;
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Construye la Capa 3 del prompt con dos fuentes ortogonales:
     * A) Síntesis de sesiones anteriores (progreso_md) si existe.
     * B) Historial real de mensajes de la sesión activa desde BD.
     */
    private String construirCapa3(Participacion participacion) {
        StringBuilder sb = new StringBuilder();

        // A) Síntesis de sesiones anteriores (solo si ya se cerró una sesión previa)
        if (participacion.getProgresoMd() != null
                && !participacion.getProgresoMd().isBlank()) {
            sb.append("## Progreso acumulado de sesiones anteriores\n");
            sb.append(participacion.getProgresoMd()).append("\n\n");
        }

        // B) Historial completo de la sesión activa (recuperado de BD)
        List<Mensaje> historial = mensajeRepository
                .findByParticipacionOrderByCreatedAtAsc(participacion);
        if (!historial.isEmpty()) {
            sb.append("## Historial de la sesión actual\n");
            for (Mensaje m : historial) {
                String rol = Boolean.TRUE.equals(m.getEmisor()) ? "ESTUDIANTE" : "TUTOR";
                sb.append("[").append(rol).append("]: ")
                        .append(m.getContenido()).append("\n");
            }
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
        return participacionRepository.save(nueva);
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
