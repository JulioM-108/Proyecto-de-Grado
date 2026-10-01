package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import Prototipo.Proyecto_Grado.service.AnalyticsService;
import Prototipo.Proyecto_Grado.service.AuthService;
import Prototipo.Proyecto_Grado.service.ChatService;
import Prototipo.Proyecto_Grado.service.PromptAssistantService;
import Prototipo.Proyecto_Grado.service.TotpService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    private final ChatService chatService;
    private final PromptAssistantService promptAssistantService;
    private final AnalyticsService analyticsService;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

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
        if (profesor == null) return "redirect:/login";

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
        if (profesor == null) return "redirect:/login";

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
            profesor.setContrasena(passwordEncoder.encode(nuevaContrasena.trim()));
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
        if (profesor == null) return "redirect:/login";

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
            @RequestBody PromptAssistantRequest request,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PromptAssistantResponse(false, null, "No autorizado. Debes iniciar sesión como docente."));
        }

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
        if (profesor == null) return "redirect:/login";

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
        if (profesor == null) return "redirect:/login";

        Sesion sesion = sesionRepository.findById(id).orElse(null);

        if (sesion != null && sesion.getProfesor().getId().equals(profesor.getId())) {
            boolean nuevoEstado = !Boolean.TRUE.equals(sesion.getEstado());
            sesion.setEstado(nuevoEstado);
            sesionRepository.save(sesion);

            // Respaldo 2: Al pausar la sesión (cierre de la clase presencial), consolidar
            // automáticamente a todos los estudiantes con mensajes cuyo diagnóstico no se haya generado
            if (!nuevoEstado) {
                List<Participacion> participaciones = participacionRepository.findBySesion(sesion);
                for (Participacion part : participaciones) {
                    if (part.getProgresoMd() == null || part.getProgresoMd().isBlank()) {
                        long totalMsgs = mensajeRepository.countByParticipacion(part);
                        if (totalMsgs >= 2) {
                            try {
                                chatService.cerrarSesionYGenerarProgreso(part.getId());
                                log.info("Consolidación masiva: progreso_md generado para participación {} al pausar clase", part.getId());
                            } catch (Exception e) {
                                log.warn("Error al consolidar participación {} al pausar sesión: {}", part.getId(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return "redirect:/docente/sesiones";
    }

    // ─── 5.1 Diagnóstico Pedagógico Bajo Demanda (Lazy Synthesis) ─────────────

    public record GenerarDiagnosticoResponse(boolean success, String progresoMd, String error) {}

    @PostMapping("/api/participaciones/{participacionId}/generar-diagnostico")
    @ResponseBody
    public ResponseEntity<GenerarDiagnosticoResponse> generarDiagnosticoBajoDemanda(
            @PathVariable UUID participacionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GenerarDiagnosticoResponse(false, null, "No autorizado."));
        }

        Participacion part = participacionRepository.findById(participacionId).orElse(null);
        if (part == null || part.getSesion() == null || !part.getSesion().getProfesor().getId().equals(profesor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenerarDiagnosticoResponse(false, null, "Participación no encontrada o no pertenece a sus clases."));
        }

        long count = mensajeRepository.countByParticipacion(part);
        if (count == 0) {
            return ResponseEntity.badRequest()
                    .body(new GenerarDiagnosticoResponse(false, null, "El estudiante no registra mensajes en esta sesión."));
        }

        try {
            chatService.cerrarSesionYGenerarProgreso(participacionId);
            Participacion actualizada = participacionRepository.findById(participacionId).orElse(part);
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(true, actualizada.getProgresoMd(), null));
        } catch (Exception e) {
            log.error("Error al generar diagnóstico bajo demanda con IA: {}", e.getMessage(), e);
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(false, null, "Error al procesar la síntesis con el motor de IA."));
        }
    }

    // ─── 6. Eliminar Sesión de Clase (con limpieza en cascada) ─────────────────

    @Transactional
    @PostMapping("/sesiones/{id}/eliminar")
    public String eliminarSesion(@PathVariable UUID id, HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

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
        if (profesor == null) return "redirect:/login";

        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        // Verificación IDOR: garantizar que la sesión pertenezca al docente en sesión
        if (!sesion.getProfesor().getId().equals(profesor.getId())) {
            log.warn("Acceso denegado: el docente {} intentó acceder a la sesión {}", profesor.getCorreo(), id);
            return "redirect:/docente/sesiones?error=No+autorizado";
        }

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

        // Evaluaciones psicométricas SUS + TAM de los estudiantes en esta sesión
        List<Participacion> evaluacionesSesion = participacionesSesion.stream()
                .filter(p -> p.getEvaluacionSusTam() != null)
                .collect(Collectors.toList());

        double promedioSusSesion = evaluacionesSesion.stream()
                .map(Participacion::getEvaluacionSusTamDTO)
                .filter(dto -> dto != null && dto.getSusScore() != null)
                .mapToDouble(EvaluacionSusTamDTO::getSusScore)
                .average()
                .orElse(0.0);

        model.addAttribute("profesor", profesor);
        model.addAttribute("sesion", sesion);
        model.addAttribute("metricas", metricas);
        model.addAttribute("estudiantesDisponibles", estudiantesDisponibles);
        model.addAttribute("feedbacksSesion", feedbacksSesion);
        model.addAttribute("evaluacionesSesion", evaluacionesSesion);
        model.addAttribute("promedioValoracion", Math.round(promedioValoracion * 10.0) / 10.0);
        model.addAttribute("totalFeedbacks", feedbacksSesion.size());
        model.addAttribute("promedioSusSesion", Math.round(promedioSusSesion * 10.0) / 10.0);
        model.addAttribute("totalEvaluacionesSusSesion", evaluacionesSesion.size());
        model.addAttribute("clasificacionSusSesion", EvaluacionSusTamDTO.interpretarSus(promedioSusSesion));
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
        if (profesor == null) return "redirect:/login";

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
        if (profesor == null) return "redirect:/login";

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
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Setup2FaResponse(false, null, null, null));
        }

        String secreto = totpService.generarSecretoBase32();
        session.setAttribute("tempSecreto2fa", secreto);

        String qrUri = totpService.generarOtpAuthUri(profesor.getCorreo(), secreto);
        String qrUrl = totpService.generarQrBase64(qrUri, 200, 200);
        if (qrUrl == null) {
            qrUrl = "";
        }

        return ResponseEntity.ok(new Setup2FaResponse(true, secreto, qrUri, qrUrl));
    }

    @PostMapping("/api/2fa/activar")
    @ResponseBody
    public ResponseEntity<Activar2FaResponse> activar2Fa(
            @RequestBody Activar2FaRequest request,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Activar2FaResponse(false, "No autorizado."));
        }

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
    public String desactivar2Fa(@RequestParam("contrasena") String contrasena, HttpSession session) {
        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        if (!authService.verificarPassword(contrasena, profesor.getContrasena())) {
            return "redirect:/docente?error2fa=contrasenaIncorrecta";
        }

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
            long clasesActivas,
            String ultimaActividadFormateada
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/estudiantes")
    public String listarEstudiantes(
            @RequestParam(value = "estudianteAsignado", required = false) Boolean estudianteAsignado,
            HttpSession session, Model model) {
        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        // Consultar solo estudiantes vinculados a sesiones del docente autenticado (principio de mínimo privilegio)
        List<Participacion> participaciones = participacionRepository.findByProfesorWithEstudianteAndSesion(profesor);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);

        Map<UUID, List<Participacion>> porEstudiante = participaciones.stream()
                .filter(p -> p.getEstudiante() != null)
                .collect(Collectors.groupingBy(p -> p.getEstudiante().getId(), LinkedHashMap::new, Collectors.toList()));

        // Extraer estudiantes únicos desde las participaciones del docente
        Map<UUID, Estudiante> estudiantesUnicos = new LinkedHashMap<>();
        for (Participacion p : participaciones) {
            if (p.getEstudiante() != null) {
                estudiantesUnicos.putIfAbsent(p.getEstudiante().getId(), p.getEstudiante());
            }
        }
        List<Estudiante> estudiantesDelDocente = new ArrayList<>(estudiantesUnicos.values());
        estudiantesDelDocente.sort(Comparator.comparing(e -> e.getNombre() != null ? e.getNombre() : ""));

        List<EstudianteDirectorioDto> directorio = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Estudiante est : estudiantesDelDocente) {
            List<Participacion> parts = porEstudiante.get(est.getId());
            long totalClases = (parts != null) ? parts.size() : 0;
            long clasesActivas = (parts != null) ? parts.stream()
                    .filter(p -> p.getSesion() != null && Boolean.TRUE.equals(p.getSesion().getEstado()))
                    .count() : 0;
            String ultimaActividad = "Sin clases asignadas";

            if (parts != null && !parts.isEmpty() && parts.get(0).getCreatedAt() != null) {
                ultimaActividad = parts.get(0).getCreatedAt().format(dtf);
            }

            directorio.add(new EstudianteDirectorioDto(
                    est.getId(),
                    est.getNombre() != null && !est.getNombre().isBlank() ? est.getNombre() : "Estudiante",
                    est.getCorreo(),
                    totalClases,
                    clasesActivas,
                    ultimaActividad
            ));
        }

        long estudiantesActivos = directorio.stream().filter(d -> d.clasesActivas() > 0).count();
        long totalClasesActivas = sesiones.stream().filter(s -> Boolean.TRUE.equals(s.getEstado())).count();
        long totalParticipaciones = participaciones.size();

        model.addAttribute("profesor", profesor);
        model.addAttribute("directorio", directorio);
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("estudiantesActivos", estudiantesActivos);
        model.addAttribute("totalClasesActivas", totalClasesActivas);
        model.addAttribute("totalParticipaciones", totalParticipaciones);
        model.addAttribute("estudianteAsignado", Boolean.TRUE.equals(estudianteAsignado));
        return "docente/estudiantes";
    }

    @PostMapping("/estudiantes/{estudianteId}/asignar-sesion")
    public String asignarEstudianteASesion(
            @PathVariable UUID estudianteId,
            @RequestParam UUID sesionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

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

    @Transactional
    @PostMapping("/estudiantes/{estudianteId}/eliminar")
    public String eliminarEstudiante(
            @PathVariable UUID estudianteId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        Estudiante estudiante = estudianteRepository.findById(estudianteId).orElse(null);
        if (estudiante != null) {
            // Verificar que el estudiante SOLO tenga participaciones en sesiones de este docente (anti-IDOR)
            List<Participacion> todasParticipaciones = participacionRepository.findByEstudianteOrderByCreatedAtDesc(estudiante);
            boolean perteneceAOtroDocente = todasParticipaciones.stream()
                    .anyMatch(p -> p.getSesion() != null
                            && p.getSesion().getProfesor() != null
                            && !p.getSesion().getProfesor().getId().equals(profesor.getId()));

            if (perteneceAOtroDocente) {
                log.warn("El docente {} intentó eliminar al estudiante {} que tiene participaciones en sesiones de otros docentes",
                        profesor.getCorreo(), estudiante.getCorreo());
                return "redirect:/docente/estudiantes?errorEliminacion=true";
            }

            // Eliminar en cascada solo las participaciones en sesiones de este docente
            for (Participacion part : todasParticipaciones) {
                List<Mensaje> mensajes = mensajeRepository.findByParticipacionOrderByCreatedAtAsc(part);
                mensajeRepository.deleteAll(mensajes);

                List<Metrica> metricas = metricaRepository.findByParticipacion(part);
                metricaRepository.deleteAll(metricas);

                participacionRepository.delete(part);
            }

            estudianteRepository.delete(estudiante);
            log.info("Docente {} eliminó al estudiante {} ({}) de la plataforma",
                    profesor.getCorreo(), estudiante.getNombre(), estudiante.getCorreo());
            return "redirect:/docente/estudiantes?estudianteEliminado=true";
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
            String fechaFormateada,
            Double susScore,
            EvaluacionSusTamDTO evaluacionSusTam
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/feedback")
    public String verFeedbackGlobal(
            @RequestParam(value = "sesionId", required = false) UUID sesionId,
            HttpSession session,
            Model model) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        List<Participacion> participaciones = participacionRepository.findByProfesorWithEstudianteAndSesion(profesor);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<FeedbackItemDto> items = participaciones.stream()
                .filter(p -> p.getFeedbackEstudiante() != null || p.getValoracionEstudiante() != null || p.getEvaluacionSusTam() != null)
                .filter(p -> sesionId == null || (p.getSesion() != null && p.getSesion().getId().equals(sesionId)))
                .map(p -> {
                    EvaluacionSusTamDTO evalDTO = p.getEvaluacionSusTamDTO();
                    return new FeedbackItemDto(
                            p.getId(),
                            p.getEstudiante() != null ? p.getEstudiante().getNombre() : "Estudiante",
                            p.getEstudiante() != null ? p.getEstudiante().getCorreo() : "",
                            p.getSesion() != null ? p.getSesion().getTema() : "Clase",
                            p.getSesion() != null ? p.getSesion().getEnlaceUnico() : "",
                            p.getValoracionEstudiante() != null ? p.getValoracionEstudiante() : 0,
                            p.getFeedbackEstudiante() != null ? p.getFeedbackEstudiante() : "Sin comentario escrito.",
                            p.getCreatedAt() != null ? p.getCreatedAt().format(dtf) : "Reciente",
                            evalDTO != null ? evalDTO.getSusScore() : null,
                            evalDTO
                    );
                })
                .collect(Collectors.toList());

        double promedioGlobal = items.stream()
                .filter(f -> f.valoracion() > 0)
                .mapToInt(FeedbackItemDto::valoracion)
                .average()
                .orElse(0.0);

        double promedioSus = items.stream()
                .filter(f -> f.susScore() != null)
                .mapToDouble(FeedbackItemDto::susScore)
                .average()
                .orElse(0.0);

        long totalEvaluacionesSus = items.stream().filter(f -> f.susScore() != null).count();

        model.addAttribute("profesor", profesor);
        model.addAttribute("feedbacks", items);
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("sesionSeleccionada", sesionId);
        model.addAttribute("promedioGlobal", Math.round(promedioGlobal * 10.0) / 10.0);
        model.addAttribute("totalFeedbacks", items.size());
        model.addAttribute("promedioSusGlobal", Math.round(promedioSus * 10.0) / 10.0);
        model.addAttribute("totalEvaluacionesSus", totalEvaluacionesSus);
        model.addAttribute("interpretacionSusGlobal", EvaluacionSusTamDTO.interpretarSus(promedioSus));

        return "docente/feedback";
    }

    @GetMapping("/dashboard")
    public String dashboardGlobal() {
        return "redirect:/docente/sesiones";
    }

    // ─── Métodos Auxiliares ───────────────────────────────────────────────────

    private Profesor obtenerProfesorActual(HttpSession session) {
        if (session == null) return null;
        String rol = (String) session.getAttribute("rolUsuario");
        String correoSession = (String) session.getAttribute("usuarioLogueado");
        if (correoSession != null && "DOCENTE".equalsIgnoreCase(rol)) {
            return profesorRepository.findByCorreoIgnoreCase(correoSession).orElse(null);
        }
        return null;
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
