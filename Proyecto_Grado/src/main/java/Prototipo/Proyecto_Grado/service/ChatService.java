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
 * Coordina la arquitectura de 3 capas de prompts, el llamado a Gemini,
 * la auditoría RF-10 y la persistencia en las tablas reales de Supabase:
 *   participacion → mensaje (historial) y metrica (indicadores de desempeño).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final GeminiService geminiService;
    private final SocraticAuditorService socraticAuditorService;
    private final EstudianteRepository estudianteRepository;
    private final SesionRepository sesionRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;

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

    /**
     * Procesa una consulta del estudiante y genera la respuesta socrática auditada.
     * Flujo: obtener Participacion → construir prompt 3 capas → Gemini → RF-10 → persistir.
     *
     * @param correoEstudiante correo institucional del estudiante
     * @param enlaceSesion     enlace único de la sesión (identifica la Sesion activa)
     * @param mensajeTexto     texto enviado por el estudiante
     * @param craftDocente     configuración CRAFT del docente (puede ser null para usar la de la Sesion)
     */
    @Transactional
    public MensajeDto procesarMensajeEstudiante(String correoEstudiante,
                                                String enlaceSesion,
                                                String mensajeTexto,
                                                CraftConfigDto craftDocente) {

        // 1. Resolver Estudiante y Sesion
        Estudiante estudiante = estudianteRepository.findByCorreoIgnoreCase(correoEstudiante)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estudiante no encontrado: " + correoEstudiante));
        Sesion sesion = sesionRepository.findByEnlaceUnico(enlaceSesion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sesión no encontrada con enlace: " + enlaceSesion));

        // 2. Obtener o crear la Participacion (vínculo estudiante ↔ sesión)
        Participacion participacion = participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .orElseGet(() -> crearParticipacion(estudiante, sesion));

        // 3. Persistir el mensaje del estudiante (emisor = true)
        mensajeRepository.save(Mensaje.builder()
                .emisor(true)
                .contenido(mensajeTexto)
                .participacion(participacion)
                .build());

        // 4. Registrar métrica M-02 (turnos del estudiante en esta sesión)
        long turnosEstudiante = mensajeRepository.countByParticipacion(participacion);
        registrarMetrica(participacion, 2L, sesion.getTema(), turnosEstudiante);

        // 5. Construir prompt en 3 capas
        PromptCapasDto promptCapas = PromptCapasDto.builder()
                .capa1Sistema(capa1ReglasSistema)
                .capa2DocenteCraft(construirCapaDocente(sesion, craftDocente))
                .capa3EstudianteDinamica(construirCapa3(participacion))
                .build();

        String promptFinal = promptCapas.compilarPromptFinal(mensajeTexto);

        // 6. Generar respuesta con Gemini
        String respuestaOriginal = geminiService.generarRespuesta(promptFinal);

        // 7. Doble Verificación RF-10 (Auditor Socrático)
        SocraticAuditorService.ResultadoAuditoria auditoria =
                socraticAuditorService.auditarYCorregir(respuestaOriginal, mensajeTexto);

        // 8. Persistir la respuesta del tutor (emisor = false)
        mensajeRepository.save(Mensaje.builder()
                .emisor(false)
                .contenido(auditoria.respuestaFinal())
                .participacion(participacion)
                .build());

        // 9. Registrar métricas de auditoría
        if (auditoria.fueReformulada()) {
            log.info("RF-10: respuesta reformulada por el auditor socrático para estudiante '{}'.",
                    correoEstudiante);
            registrarMetrica(participacion, 10L, "RF-10", 1L);
            registrarMetrica(participacion, 4L, "solicitud_directa", 1L);
        }

        return MensajeDto.builder()
                .emisor("TUTOR")
                .contenido(auditoria.respuestaFinal())
                .reformuladoPorAuditor(auditoria.fueReformulada())
                .build();
    }

    // ─── Flujo 3: Cierre de Sesión y Síntesis Pedagógica ─────────────────────

    /**
     * Genera la síntesis pedagógica al cerrar la sesión.
     * Llama a Gemini con el historial completo y persiste el Markdown de 4 secciones
     * en participacion.progreso_md. Solo se invoca una vez por sesión al finalizarla.
     */
    @Transactional
    public void cerrarSesionYGenerarProgreso(UUID participacionId) {
        Participacion p = participacionRepository.findById(participacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Participación no encontrada: " + participacionId));

        List<Mensaje> historial = mensajeRepository
                .findByParticipacionOrderByCreatedAtAsc(p);

        String historialTexto = historial.stream()
                .map(m -> (Boolean.TRUE.equals(m.getEmisor()) ? "[ESTUDIANTE]" : "[TUTOR]")
                        + ": " + m.getContenido())
                .collect(Collectors.joining("\n"));

        String instruccion = """
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

        String progreso = geminiService.generarRespuesta(instruccion);
        p.setProgresoMd(progreso);
        participacionRepository.save(p);

        // Registrar métricas de cierre
        registrarMetrica(p, 3L, "sesion_cerrada", (long) historial.size());
        registrarMetrica(p, 5L, "turnos_totales", (long) historial.size());

        log.info("Flujo 3: síntesis pedagógica generada para participación {} ({} turnos)",
                participacionId, historial.size());
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
