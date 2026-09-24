package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import Prototipo.Proyecto_Grado.service.AnalyticsService;
import Prototipo.Proyecto_Grado.service.PromptAssistantService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para el Módulo de Administración de Clases y Configuración de Prompts (Docente).
 */
@Controller
@RequestMapping("/docente")
@RequiredArgsConstructor
@Slf4j
public class DocenteController {

    private final SesionRepository sesionRepository;
    private final ProfesorRepository profesorRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MetricaRepository metricaRepository;
    private final EstudianteRepository estudianteRepository;
    private final Prototipo.Proyecto_Grado.service.ChatService chatService;
    private final PromptAssistantService promptAssistantService;
    private final AnalyticsService analyticsService;
    private final Prototipo.Proyecto_Grado.service.TotpService totpService;

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    // ─── 1. Listado de Sesiones / Hub de Administración ───────────────────────

    @GetMapping({"", "/", "/sesiones"})
    public String listarSesiones(
            @RequestParam(value = "creada", required = false) Boolean creada,
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "eliminada", required = false) Boolean eliminada,
            @RequestParam(value = "perfilActualizado", required = false) Boolean perfilActualizado,
            @RequestParam(value = "2faDesactivado", required = false) Boolean dosFaDesactivado,
            @RequestParam(value = "2faActivado", required = false) Boolean dosFaActivado,
            HttpSession session,
            Model model) {

        Profesor profesor = obtenerProfesorActual(session);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);

        model.addAttribute("profesor", profesor);
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("sesionCreada", Boolean.TRUE.equals(creada));
        model.addAttribute("codigoGenerado", codigo);
        model.addAttribute("sesionEliminada", Boolean.TRUE.equals(eliminada));
        model.addAttribute("perfilActualizado", Boolean.TRUE.equals(perfilActualizado));
        model.addAttribute("dosFaDesactivado", Boolean.TRUE.equals(dosFaDesactivado));
        model.addAttribute("dosFaActivado", Boolean.TRUE.equals(dosFaActivado));

        return "docente/sesiones";
    }

    // ─── 2. Actualización de Información Personal del Docente ─────────────────

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam("nombre") String nombre,
            @RequestParam("correo") String correo,
            @RequestParam(value = "facultad", required = false) String facultad,
            @RequestParam(value = "nuevaContrasena", required = false) String nuevaContrasena,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);

        if (nombre != null && !nombre.isBlank()) {
            profesor.setNombre(nombre.trim());
        }

        if (correo != null && !correo.isBlank()) {
            String correoLimpio = correo.trim().toLowerCase();
            if (correoLimpio.endsWith("@javerianacali.edu.co") || correoLimpio.endsWith("@javeriana.edu.co")) {
                profesor.setCorreo(correoLimpio);
                session.setAttribute("usuarioLogueado", correoLimpio);
            }
        }

        if (facultad != null) {
            profesor.setFacultad(facultad.trim());
        }

        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            profesor.setContrasena(nuevaContrasena.trim());
        }

        profesorRepository.save(profesor);
        session.setAttribute("nombreUsuario", profesor.getNombre());
        log.info("Información personal del profesor actualizada: {}", profesor.getCorreo());

        return "redirect:/docente/sesiones?perfilActualizado=true";
    }

    // ─── 3. Formulario de Creación con Asistente IA de Prompts ───────────────

    @GetMapping("/sesiones/nueva")
    public String formularioNuevaSesion(
            @RequestParam(value = "tema", required = false) String tema,
            HttpSession session,
            Model model) {

        Profesor profesor = obtenerProfesorActual(session);

        CraftConfigDto craftInicial = CraftConfigDto.builder()
                .temaActualPoo((tema != null && !tema.isBlank()) ? tema : "")
                .context("Curso de Programación Orientada a Objetos - Semestre 2026-1")
                .role("Tutor socrático exigente y empático. Guía al estudiante mediante preguntas y analogías, sin entregar soluciones de código completas.")
                .audience("Estudiantes universitarios de tercer semestre de Ingeniería de Sistemas.")
                .format("1 analogía cotidiana que ilustre el concepto + 1 pregunta clave que invite al estudiante a formular el razonamiento.")
                .tone("Motivador, reflexivo, constructivo y académicamente riguroso.")
                .build();

        model.addAttribute("profesor", profesor);
        model.addAttribute("craft", craftInicial);

        return "docente/nueva_sesion";
    }

    // ─── 4. API Asistente IA de Prompts (AJAX) ───────────────────────────────

    public record PromptAssistantRequest(String tema) {}
    public record PromptAssistantResponse(boolean success, CraftConfigDto craft, String error) {}

    @PostMapping("/api/prompt-assistant")
    @ResponseBody
    public ResponseEntity<PromptAssistantResponse> invocarAsistentePrompt(
            @RequestBody PromptAssistantRequest request) {

        try {
            CraftConfigDto propuesta = promptAssistantService.generarDirectricesCraft(request.tema());
            return ResponseEntity.ok(new PromptAssistantResponse(true, propuesta, null));
        } catch (Exception e) {
            log.error("Error en endpoint de asistente IA: {}", e.getMessage(), e);
            return ResponseEntity.ok(new PromptAssistantResponse(false, null, "Error al generar la directriz con IA."));
        }
    }

    // ─── 4. Guardar y Activar Nueva Sesión ────────────────────────────────────

    @PostMapping("/sesiones/crear")
    public String crearSesion(
            @RequestParam("tema") String tema,
            @RequestParam(value = "craftContexto", required = false) String craftContexto,
            @RequestParam(value = "craftRol", required = false) String craftRol,
            @RequestParam(value = "craftAudiencia", required = false) String craftAudiencia,
            @RequestParam(value = "craftFormato", required = false) String craftFormato,
            @RequestParam(value = "craftTono", required = false) String craftTono,
            @RequestParam(value = "preguntasGuia", required = false) String preguntasGuia,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);

        // Generar código alfanumérico único para privacidad del estudiante
        String codigoUnico = generarCodigoAlfanumericoUnico();

        Sesion nuevaSesion = Sesion.builder()
                .profesor(profesor)
                .tema(tema.trim())
                .craftContexto(craftContexto != null ? craftContexto.trim() : "")
                .craftRol(craftRol != null ? craftRol.trim() : "")
                .craftAudiencia(craftAudiencia != null ? craftAudiencia.trim() : "")
                .craftFormato(craftFormato != null ? craftFormato.trim() : "")
                .craftTono(craftTono != null ? craftTono.trim() : "")
                .preguntasGuia(preguntasGuia != null ? preguntasGuia.trim() : "")
                .enlaceUnico(codigoUnico)
                .estado(true)
                .build();

        sesionRepository.save(nuevaSesion);
        log.info("Nueva sesión creada exitosamente con código alfanumérico: {}", codigoUnico);

        return "redirect:/docente/sesiones?creada=true&codigo=" + codigoUnico;
    }

    // ─── 5. Cambiar Estado de la Sesión (Activar / Pausar) ────────────────────

    @PostMapping("/sesiones/{id}/estado")
    public String alternarEstadoSesion(@PathVariable UUID id, HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(id).orElse(null);

        if (sesion != null && sesion.getProfesor().getId().equals(profesor.getId())) {
            sesion.setEstado(!Boolean.TRUE.equals(sesion.getEstado()));
            sesionRepository.save(sesion);
        }

        return "redirect:/docente/sesiones";
    }

    // ─── 6. Eliminar Sesión de Clase (con limpieza en cascada) ─────────────────

    @Transactional
    @PostMapping("/sesiones/{id}/eliminar")
    public String eliminarSesion(@PathVariable UUID id, HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(id).orElse(null);

        if (sesion != null && sesion.getProfesor().getId().equals(profesor.getId())) {
            List<Participacion> participaciones = participacionRepository.findBySesion(sesion);
            for (Participacion part : participaciones) {
                List<Mensaje> mensajes = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(part);
                mensajeRepository.deleteAll(mensajes);

                List<Metrica> metricas = metricaRepository.findByParticipacion(part);
                metricaRepository.deleteAll(metricas);

                participacionRepository.delete(part);
            }
            sesionRepository.delete(sesion);
            log.info("Sesión {} eliminada exitosamente por el profesor {}", id, profesor.getCorreo());
            return "redirect:/docente/sesiones?eliminada=true";
        }

        return "redirect:/docente/sesiones";
    }

    // ─── 6. Métricas y Analítica por Sesión ───────────────────────────────────

    @GetMapping("/sesiones/{id}/metricas")
    public String verMetricasSesion(
            @PathVariable UUID id,
            @RequestParam(value = "craftGuardado", required = false) Boolean craftGuardado,
            @RequestParam(value = "estudianteVinculado", required = false) Boolean estudianteVinculado,
            HttpSession session,
            Model model) {

        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        AnalyticsService.MetricasDashboard metricas = analyticsService.obtenerMetricasPorSesion(sesion);

        // Estudiantes del profesor que aún no forman parte de esta clase
        List<Estudiante> todosEstudiantesDocente = participacionRepository.findDistinctEstudiantesByProfesor(profesor);
        List<Participacion> participacionesSesion = participacionRepository.findBySesion(sesion);
        Set<UUID> idsEstudiantesEnSesion = participacionesSesion.stream()
                .map(p -> p.getEstudiante().getId())
                .collect(Collectors.toSet());

        List<Estudiante> estudiantesDisponibles = todosEstudiantesDocente.stream()
                .filter(est -> !idsEstudiantesEnSesion.contains(est.getId()))
                .collect(Collectors.toList());

        // Feedbacks y valoraciones de los estudiantes en esta sesión
        List<Participacion> feedbacksSesion = participacionesSesion.stream()
                .filter(p -> p.getFeedbackEstudiante() != null || p.getValoracionEstudiante() != null)
                .collect(Collectors.toList());

        double promedioValoracion = feedbacksSesion.stream()
                .filter(p -> p.getValoracionEstudiante() != null)
                .mapToInt(Participacion::getValoracionEstudiante)
                .average()
                .orElse(0.0);

        model.addAttribute("profesor", profesor);
        model.addAttribute("sesion", sesion);
        model.addAttribute("metricas", metricas);
        model.addAttribute("estudiantesDisponibles", estudiantesDisponibles);
        model.addAttribute("feedbacksSesion", feedbacksSesion);
        model.addAttribute("promedioValoracion", Math.round(promedioValoracion * 10.0) / 10.0);
        model.addAttribute("totalFeedbacks", feedbacksSesion.size());
        model.addAttribute("craftGuardado", Boolean.TRUE.equals(craftGuardado));
        model.addAttribute("estudianteVinculado", Boolean.TRUE.equals(estudianteVinculado));

        return "docente/dashboard";
    }

    @PostMapping("/sesiones/{id}/estudiantes/vincular")
    public String vincularEstudianteASesion(
            @PathVariable UUID id,
            @RequestParam("estudianteId") UUID estudianteId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        if (sesion.getProfesor().getId().equals(profesor.getId())) {
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                    .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

            if (!participacionRepository.existsByEstudianteAndSesion(estudiante, sesion)) {
                chatService.crearParticipacion(estudiante, sesion);
                log.info("Docente {} vinculó manualmente a estudiante {} en la clase {}",
                        profesor.getCorreo(), estudiante.getCorreo(), sesion.getTema());
            }
        }

        return "redirect:/docente/sesiones/" + id + "/metricas?estudianteVinculado=true";
    }

    @PostMapping("/sesiones/{id}/craft")
    public String actualizarCraftSesion(
            @PathVariable UUID id,
            @RequestParam(value = "craftContexto", required = false) String craftContexto,
            @RequestParam(value = "craftRol", required = false) String craftRol,
            @RequestParam(value = "craftAudiencia", required = false) String craftAudiencia,
            @RequestParam(value = "craftFormato", required = false) String craftFormato,
            @RequestParam(value = "craftTono", required = false) String craftTono,
            @RequestParam(value = "preguntasGuia", required = false) String preguntasGuia,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        if (sesion.getProfesor().getId().equals(profesor.getId())) {
            if (craftContexto != null) sesion.setCraftContexto(craftContexto.trim());
            if (craftRol != null) sesion.setCraftRol(craftRol.trim());
            if (craftAudiencia != null) sesion.setCraftAudiencia(craftAudiencia.trim());
            if (craftFormato != null) sesion.setCraftFormato(craftFormato.trim());
            if (craftTono != null) sesion.setCraftTono(craftTono.trim());
            if (preguntasGuia != null) sesion.setPreguntasGuia(preguntasGuia.trim());
            sesionRepository.save(sesion);
            log.info("Directrices pedagógicas CRAFT actualizadas para la sesión {}", id);
        }

        return "redirect:/docente/sesiones/" + id + "/metricas?craftGuardado=true";
    }

    // ─── 8. Doble Factor de Autenticación (2FA / TOTP) ───────────────────────

    public record Setup2FaResponse(boolean success, String secreto, String qrUri, String qrUrl) {}
    public record Activar2FaRequest(String codigo) {}
    public record Activar2FaResponse(boolean success, String error) {}

    @GetMapping("/api/2fa/setup")
    @ResponseBody
    public ResponseEntity<Setup2FaResponse> generarSetup2Fa(HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        String secreto = totpService.generarSecretoBase32();
        session.setAttribute("tempSecreto2fa", secreto);

        String qrUri = totpService.generarOtpAuthUri(profesor.getCorreo(), secreto);
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + java.net.URLEncoder.encode(qrUri, java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok(new Setup2FaResponse(true, secreto, qrUri, qrUrl));
    }

    @PostMapping("/api/2fa/activar")
    @ResponseBody
    public ResponseEntity<Activar2FaResponse> activar2Fa(
            @RequestBody Activar2FaRequest request,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        String tempSecreto = (String) session.getAttribute("tempSecreto2fa");

        if (tempSecreto == null) {
            return ResponseEntity.ok(new Activar2FaResponse(false, "No se encontró una sesión activa de configuración 2FA. Abre de nuevo el modal."));
        }

        boolean valido = totpService.validarCodigo(tempSecreto, request.codigo());
        if (!valido) {
            return ResponseEntity.ok(new Activar2FaResponse(false, "El código de 6 dígitos no coincide o expiró. Asegúrate de haber escaneado el QR actual."));
        }

        profesor.setDosFactorActivo(true);
        profesor.setSecreto2fa(tempSecreto);
        profesorRepository.save(profesor);
        session.removeAttribute("tempSecreto2fa");
        log.info("Doble factor 2FA activado con éxito para el profesor: {}", profesor.getCorreo());

        return ResponseEntity.ok(new Activar2FaResponse(true, null));
    }

    @PostMapping("/2fa/desactivar")
    public String desactivar2Fa(HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        profesor.setDosFactorActivo(false);
        profesor.setSecreto2fa(null);
        profesorRepository.save(profesor);
        log.info("Doble factor 2FA desactivado para el profesor: {}", profesor.getCorreo());

        return "redirect:/docente?2faDesactivado=true";
    }

    // Redirección de compatibilidad para el dashboard global
    // ─── 9. Directorio General de Estudiantes ───────────────────────────────

    public record EstudianteDirectorioDto(
            UUID id,
            String nombre,
            String correo,
            long totalClases,
            String ultimaActividadFormateada
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/estudiantes")
    public String listarEstudiantes(
            @RequestParam(value = "estudianteAsignado", required = false) Boolean estudianteAsignado,
            HttpSession session, Model model) {
        Profesor profesor = obtenerProfesorActual(session);

        List<Participacion> participaciones = participacionRepository.findByProfesorWithEstudianteAndSesion(profesor);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);

        Map<UUID, List<Participacion>> porEstudiante = participaciones.stream()
                .filter(p -> p.getEstudiante() != null)
                .collect(Collectors.groupingBy(p -> p.getEstudiante().getId(), LinkedHashMap::new, Collectors.toList()));

        List<EstudianteDirectorioDto> directorio = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Map.Entry<UUID, List<Participacion>> entry : porEstudiante.entrySet()) {
            List<Participacion> parts = entry.getValue();
            Estudiante est = parts.get(0).getEstudiante();

            String ultimaActividad = "Sin registro";
            if (!parts.isEmpty() && parts.get(0).getCreatedAt() != null) {
                ultimaActividad = parts.get(0).getCreatedAt().format(dtf);
            }

            directorio.add(new EstudianteDirectorioDto(
                    est.getId(),
                    est.getNombre() != null && !est.getNombre().isBlank() ? est.getNombre() : "Estudiante",
                    est.getCorreo(),
                    parts.size(),
                    ultimaActividad
            ));
        }

        model.addAttribute("profesor", profesor);
        model.addAttribute("directorio", directorio);
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("estudianteAsignado", Boolean.TRUE.equals(estudianteAsignado));
        return "docente/estudiantes";
    }

    @PostMapping("/estudiantes/{estudianteId}/asignar-sesion")
    public String asignarEstudianteASesion(
            @PathVariable UUID estudianteId,
            @RequestParam UUID sesionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        Sesion sesion = sesionRepository.findById(sesionId).orElse(null);
        Estudiante estudiante = estudianteRepository.findById(estudianteId).orElse(null);

        if (sesion != null && estudiante != null && sesion.getProfesor().getId().equals(profesor.getId())) {
            if (!participacionRepository.existsByEstudianteAndSesion(estudiante, sesion)) {
                chatService.crearParticipacion(estudiante, sesion);
                log.info("Docente {} asignó manualmente al estudiante {} en la clase {}",
                        profesor.getCorreo(), estudiante.getCorreo(), sesion.getTema());
            }
            return "redirect:/docente/estudiantes?estudianteAsignado=true";
        }

        return "redirect:/docente/estudiantes";
    }

    // ─── 10. Módulo de Feedback Consolidado de Estudiantes ────────────────────

    public record FeedbackItemDto(
            UUID participacionId,
            String nombreEstudiante,
            String correoEstudiante,
            String temaSesion,
            String enlaceSesion,
            Integer valoracion,
            String comentario,
            String fechaFormateada
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/feedback")
    public String verFeedbackGlobal(
            @RequestParam(value = "sesionId", required = false) UUID sesionId,
            HttpSession session,
            Model model) {

        Profesor profesor = obtenerProfesorActual(session);
        List<Participacion> participaciones = participacionRepository.findByProfesorWithEstudianteAndSesion(profesor);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<FeedbackItemDto> items = participaciones.stream()
                .filter(p -> p.getFeedbackEstudiante() != null || p.getValoracionEstudiante() != null)
                .filter(p -> sesionId == null || (p.getSesion() != null && p.getSesion().getId().equals(sesionId)))
                .map(p -> new FeedbackItemDto(
                        p.getId(),
                        p.getEstudiante() != null ? p.getEstudiante().getNombre() : "Estudiante",
                        p.getEstudiante() != null ? p.getEstudiante().getCorreo() : "",
                        p.getSesion() != null ? p.getSesion().getTema() : "Clase",
                        p.getSesion() != null ? p.getSesion().getEnlaceUnico() : "",
                        p.getValoracionEstudiante() != null ? p.getValoracionEstudiante() : 0,
                        p.getFeedbackEstudiante() != null ? p.getFeedbackEstudiante() : "Sin comentario escrito.",
                        p.getCreatedAt() != null ? p.getCreatedAt().format(dtf) : "Reciente"
                ))
                .collect(Collectors.toList());

        double promedioGlobal = items.stream()
                .filter(f -> f.valoracion() > 0)
                .mapToInt(FeedbackItemDto::valoracion)
                .average()
                .orElse(0.0);

        model.addAttribute("profesor", profesor);
        model.addAttribute("feedbacks", items);
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("sesionSeleccionada", sesionId);
        model.addAttribute("promedioGlobal", Math.round(promedioGlobal * 10.0) / 10.0);
        model.addAttribute("totalFeedbacks", items.size());

        return "docente/feedback";
    }

    @GetMapping("/dashboard")
    public String dashboardGlobal() {
        return "redirect:/docente/sesiones";
    }

    // ─── Métodos Auxiliares ───────────────────────────────────────────────────

    private Profesor obtenerProfesorActual(HttpSession session) {
        String correoSession = (String) session.getAttribute("usuarioLogueado");
        if (correoSession != null) {
            Optional<Profesor> profOpt = profesorRepository.findByCorreoIgnoreCase(correoSession);
            if (profOpt.isPresent()) return profOpt.get();
        }

        // Fallback para pruebas de desarrollo si no ha iniciado sesión
        return profesorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Profesor demo = Profesor.builder()
                    .correo("profesor.demo@javerianacali.edu.co")
                    .nombre("Profesor Demo Javeriano")
                    .facultad("Facultad de Ingeniería")
                    .contrasena("demo1234")
                    .build();
            return profesorRepository.save(demo);
        });
    }

    private String generarCodigoAlfanumericoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES_CODIGO.charAt(random.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (sesionRepository.findByEnlaceUnico(codigo).isPresent());
        return codigo;
    }
}
