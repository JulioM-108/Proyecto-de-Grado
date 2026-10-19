package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.dto.CraftConfigDto;
import Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.MetricaRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.model.entity.ProfesorEstudiante;
import Prototipo.Proyecto_Grado.repository.ProfesorEstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import Prototipo.Proyecto_Grado.service.AnalyticsService;
import Prototipo.Proyecto_Grado.service.AuthService;
import Prototipo.Proyecto_Grado.service.ChatService;
import Prototipo.Proyecto_Grado.service.GeminiService;
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
    private final ProfesorEstudianteRepository profesorEstudianteRepository;
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
            @RequestParam(value = "error2fa", required = false) String error2fa,
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
        model.addAttribute("error2fa", "contrasenaIncorrecta".equals(error2fa));

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

        // Los campos CRAFT empiezan vacíos: la plantilla muestra ejemplos como placeholder
        CraftConfigDto craftInicial = CraftConfigDto.builder()
                .temaActualPoo((tema != null && !tema.isBlank()) ? tema : "")
                .build();

        model.addAttribute("profesor", profesor);
        model.addAttribute("craft", craftInicial);

        return "docente/nueva_sesion";
    }

    // ─── 4. API Asistente IA de Prompts (AJAX) ───────────────────────────────

    public record PromptAssistantRequest(String tema) {}
    /** aviso != null cuando la IA no respondió y se devolvió la plantilla base (no es una propuesta de la IA). */
    public record PromptAssistantResponse(boolean success, CraftConfigDto craft, String error, String aviso) {}

    @PostMapping("/api/prompt-assistant")
    @ResponseBody
    public ResponseEntity<PromptAssistantResponse> invocarAsistentePrompt(
            @RequestBody PromptAssistantRequest request,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PromptAssistantResponse(false, null, "No autorizado. Debes iniciar sesión como docente.", null));
        }

        try {
            CraftConfigDto propuesta = promptAssistantService.generarDirectricesCraft(request.tema());
            String aviso = propuesta.isGeneradoPorIa() ? null
                    : "La IA no respondió en este momento: se cargó una plantilla base para que la revises y edites.";
            return ResponseEntity.ok(new PromptAssistantResponse(true, propuesta, null, aviso));
        } catch (Exception e) {
            log.error("Error en endpoint de asistente IA: {}", e.getMessage(), e);
            return ResponseEntity.ok(new PromptAssistantResponse(false, null, "Error al generar la directriz con IA.", null));
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
            log.info("Sesión {} cambió su estado a: {}", id, nuevoEstado ? "ACTIVA" : "PAUSADA");
        }

        return "redirect:/docente/sesiones";
    }

    // ─── 5.1 Diagnóstico Pedagógico Bajo Demanda (Lazy Synthesis) ─────────────

    public record GenerarDiagnosticoResponse(boolean success, String progresoMd, String error) {}

    @PostMapping("/api/participaciones/{participacionId}/generar-diagnostico")
    @ResponseBody
    public ResponseEntity<GenerarDiagnosticoResponse> generarDiagnosticoBajoDemanda(
            @PathVariable UUID participacionId,
            @RequestParam(value = "regenerar", defaultValue = "false") boolean regenerar,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GenerarDiagnosticoResponse(false, null, "No autorizado."));
        }

        Participacion part = participacionRepository.findByIdWithSesionAndProfesor(participacionId).orElse(null);
        if (part == null || part.getSesion() == null || !part.getSesion().getProfesor().getId().equals(profesor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenerarDiagnosticoResponse(false, null, "Participación no encontrada o no pertenece a sus clases."));
        }

        // Si ya cuenta con diagnóstico válido y no se pidió uno nuevo, retornarlo sin llamar a Gemini
        if (!regenerar && GeminiService.esTextoGeneradoValido(part.getProgresoMd())) {
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(true, part.getProgresoMd(), null));
        }

        long count = mensajeRepository.countByParticipacion(part);
        if (count == 0) {
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(false, null, "El estudiante aún no registra consultas en esta sesión. Se requiere al menos un intercambio socrático para generar el diagnóstico."));
        }

        // El diagnóstico se genera sin cerrar la tutoría: el estudiante puede seguir conversando.
        try {
            // regenerar = true reemplaza el diagnóstico anterior con uno basado en la conversación actual
            String md = chatService.generarDiagnostico(participacionId, regenerar);
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(true, md, null));
        } catch (IllegalStateException e) {
            log.warn("Diagnóstico no generado para participación {}: {}", participacionId, e.getMessage());
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(false, null, "El motor de IA reportó saturación temporal. Por favor presiona Reintentar en unos segundos."));
        } catch (Exception e) {
            log.error("Error al generar diagnóstico bajo demanda con IA: {}", e.getMessage(), e);
            return ResponseEntity.ok(new GenerarDiagnosticoResponse(false, null, "Error al procesar la síntesis con el motor de IA. Por favor reintenta nuevamente."));
        }
    }

    // ─── 5.1b Informes Pendientes del Grupo (Flujo 3) ─────────────────────────

    public record GenerarInformesPendientesResponse(boolean success, int pendientes, String mensaje) {}

    /**
     * Genera en segundo plano los informes de quienes escribieron al tutor y aún no tienen informe
     * (los que completan la ruta ya lo reciben automáticamente).
     */
    @PostMapping("/api/sesiones/{sesionId}/generar-informes-pendientes")
    @ResponseBody
    public ResponseEntity<GenerarInformesPendientesResponse> generarInformesPendientes(
            @PathVariable UUID sesionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GenerarInformesPendientesResponse(false, 0, "No autorizado."));
        }

        Sesion sesion = sesionRepository.findById(sesionId).orElse(null);
        if (sesion == null || !sesion.getProfesor().getId().equals(profesor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenerarInformesPendientesResponse(false, 0, "Sesión no encontrada o no pertenece a sus clases."));
        }

        try {
            int pendientes = chatService.generarInformesPendientes(sesion);
            if (pendientes < 0) {
                return ResponseEntity.ok(new GenerarInformesPendientesResponse(false, 0,
                        "Ya se están generando los informes de esta clase. Recarga la página en unos minutos."));
            }
            if (pendientes == 0) {
                return ResponseEntity.ok(new GenerarInformesPendientesResponse(true, 0, "No hay informes pendientes."));
            }
            return ResponseEntity.ok(new GenerarInformesPendientesResponse(true, pendientes,
                    "Se están generando " + pendientes + (pendientes == 1 ? " informe" : " informes")
                            + " en segundo plano (unos " + Math.max(1, Math.round(pendientes * 15 / 60.0))
                            + " min). Recarga la página para verlos."));
        } catch (Exception e) {
            log.error("Error al encolar los informes pendientes de la sesión {}: {}", sesionId, e.getMessage(), e);
            return ResponseEntity.ok(new GenerarInformesPendientesResponse(false, 0,
                    "No se pudieron iniciar los informes. Intenta de nuevo en unos segundos."));
        }
    }

    // ─── 5.2 Síntesis Pedagógica Consolidada Grupal Bajo Demanda ──────────────

    public record GenerarSintesisGrupalResponse(boolean success, String sintesis, String error) {}

    @PostMapping("/api/sesiones/{sesionId}/generar-sintesis")
    @ResponseBody
    public ResponseEntity<GenerarSintesisGrupalResponse> generarSintesisGrupal(
            @PathVariable UUID sesionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GenerarSintesisGrupalResponse(false, null, "No autorizado."));
        }

        Sesion sesion = sesionRepository.findById(sesionId).orElse(null);
        if (sesion == null || !sesion.getProfesor().getId().equals(profesor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenerarSintesisGrupalResponse(false, null, "Sesión no encontrada o no pertenece a sus clases."));
        }

        try {
            String sintesis = analyticsService.generarYGuardarSintesisIa(sesion);
            return ResponseEntity.ok(new GenerarSintesisGrupalResponse(true, sintesis, null));
        } catch (IllegalStateException e) {
            // Sin interacciones o IA no disponible: se informa sin mostrar ningún texto prefabricado
            return ResponseEntity.ok(new GenerarSintesisGrupalResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Error al generar síntesis grupal con IA para sesión {}: {}", sesionId, e.getMessage(), e);
            return ResponseEntity.ok(new GenerarSintesisGrupalResponse(false, null, "No se pudo generar la síntesis grupal con IA."));
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
            if (!participaciones.isEmpty()) {
                mensajeRepository.deleteByParticipacionIn(participaciones);
                metricaRepository.deleteByParticipacionIn(participaciones);
                participacionRepository.deleteAll(participaciones);
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
            @RequestParam(value = "estudianteDesvinculado", required = false) Boolean estudianteDesvinculado,
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

        // Estudiantes de la cartera del docente que aún no forman parte de esta clase
        List<ProfesorEstudiante> vinculosDocente = profesorEstudianteRepository.findByProfesorWithEstudiante(profesor);
        List<Participacion> participacionesSesion = participacionRepository.findBySesion(sesion);
        Set<UUID> idsEstudiantesEnSesion = participacionesSesion.stream()
                .filter(p -> p.getEstudiante() != null)
                .map(p -> p.getEstudiante().getId())
                .collect(Collectors.toSet());

        List<Estudiante> estudiantesDisponibles = vinculosDocente.stream()
                .map(ProfesorEstudiante::getEstudiante)
                .filter(est -> est != null && !idsEstudiantesEnSesion.contains(est.getId()))
                .sorted(Comparator.comparing(e -> e.getNombre() != null ? e.getNombre() : ""))
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

        List<String> preguntasGuiaList = new ArrayList<>();
        if (sesion.getPreguntasGuia() != null && !sesion.getPreguntasGuia().isBlank()) {
            preguntasGuiaList = Arrays.stream(sesion.getPreguntasGuia().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        model.addAttribute("profesor", profesor);
        model.addAttribute("sesion", sesion);
        model.addAttribute("preguntasGuiaList", preguntasGuiaList);
        model.addAttribute("metricas", metricas);
        model.addAttribute("informesEnProceso", chatService.estaGenerandoInformes(sesion.getId()));
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
        model.addAttribute("estudianteDesvinculado", Boolean.TRUE.equals(estudianteDesvinculado));

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

    @Transactional
    @PostMapping("/sesiones/{id}/participaciones/{participacionId}/desvincular")
    public String desvincularEstudianteDeSesion(
            @PathVariable UUID id,
            @PathVariable UUID participacionId,
            HttpSession session) {

        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        Sesion sesion = sesionRepository.findById(id).orElse(null);
        if (sesion == null || !sesion.getProfesor().getId().equals(profesor.getId())) {
            return "redirect:/docente/sesiones";
        }

        Participacion part = participacionRepository.findByIdWithEstudianteAndSesionAndProfesor(participacionId).orElse(null);
        if (part != null && part.getSesion() != null && part.getSesion().getId().equals(sesion.getId())) {
            String correoEst = (part.getEstudiante() != null && part.getEstudiante().getCorreo() != null)
                    ? part.getEstudiante().getCorreo() : "desconocido";
            String temaSesion = sesion.getTema();

            List<Participacion> listaPart = List.of(part);
            mensajeRepository.deleteByParticipacionIn(listaPart);
            metricaRepository.deleteByParticipacionIn(listaPart);
            participacionRepository.deleteAll(listaPart);
            participacionRepository.flush();

            log.info("Docente {} desvinculó al estudiante {} de la sesión {}",
                    profesor.getCorreo(), correoEst, temaSesion);
            return "redirect:/docente/sesiones/" + id + "/metricas?estudianteDesvinculado=true";
        }

        return "redirect:/docente/sesiones/" + id + "/metricas";
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

        if (!authService.verificarPasswordDocente(contrasena, profesor.getContrasena())) {
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
            String ultimaActividadFormateada,
            /** M-04: clases con la ruta completada */
            long clasesCompletadas,
            /** M-04: tiempo activo promedio por clase en la que escribió */
            String tiempoActivoPromedio
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/estudiantes")
    public String listarEstudiantes(
            @RequestParam(value = "estudianteAsignado", required = false) Boolean estudianteAsignado,
            HttpSession session, Model model) {
        Profesor profesor = obtenerProfesorActual(session);
        if (profesor == null) return "redirect:/login";

        // Consultar estudiantes de la cartera permanente del docente autenticado
        List<ProfesorEstudiante> vinculosDocente = profesorEstudianteRepository.findByProfesorWithEstudiante(profesor);
        List<Participacion> participaciones = participacionRepository.findByProfesorWithEstudianteAndSesion(profesor);
        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);

        Map<UUID, List<Participacion>> porEstudiante = participaciones.stream()
                .filter(p -> p.getEstudiante() != null)
                .collect(Collectors.groupingBy(p -> p.getEstudiante().getId(), LinkedHashMap::new, Collectors.toList()));

        // Fecha del último mensaje del estudiante en cada participación (una sola consulta agrupada)
        Map<UUID, java.time.OffsetDateTime> ultimoMensajePorParticipacion = new HashMap<>();
        if (!participaciones.isEmpty()) {
            for (Object[] fila : mensajeRepository.findUltimoMensajeEstudiantePorParticipacion(participaciones)) {
                if (fila[0] != null && fila[1] != null) {
                    ultimoMensajePorParticipacion.put((UUID) fila[0], (java.time.OffsetDateTime) fila[1]);
                }
            }
        }

        // M-04: horas de todos los mensajes por participación, para el tiempo activo
        Map<UUID, List<java.time.OffsetDateTime>> horasPorParticipacion = new HashMap<>();
        if (!participaciones.isEmpty()) {
            for (Object[] fila : mensajeRepository.findHorasMensajesPorParticipacion(participaciones)) {
                if (fila[0] != null && fila[1] != null) {
                    horasPorParticipacion.computeIfAbsent((UUID) fila[0], k -> new ArrayList<>()).add((java.time.OffsetDateTime) fila[1]);
                }
            }
        }

        List<EstudianteDirectorioDto> directorio = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (ProfesorEstudiante pe : vinculosDocente) {
            Estudiante est = pe.getEstudiante();
            if (est == null) continue;

            List<Participacion> parts = porEstudiante.get(est.getId());
            long totalClases = (parts != null) ? parts.size() : 0;
            long clasesActivas = (parts != null) ? parts.stream()
                    .filter(p -> p.getSesion() != null && Boolean.TRUE.equals(p.getSesion().getEstado()))
                    .count() : 0;

            // Última actividad = último mensaje del estudiante en las clases de este docente
            String ultimaActividad;
            java.time.OffsetDateTime ultimoMensaje = (parts == null) ? null : parts.stream()
                    .map(p -> ultimoMensajePorParticipacion.get(p.getId()))
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            if (ultimoMensaje != null) {
                ultimaActividad = ultimoMensaje.format(dtf);
            } else if (parts != null && !parts.isEmpty()) {
                ultimaActividad = "Sin mensajes aún";
            } else {
                ultimaActividad = "Sin clases asignadas";
            }

            long clasesCompletadas = (parts != null) ? parts.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getRutaCompletada())).count() : 0;
            List<Long> minutosPorClase = (parts == null) ? List.of() : parts.stream()
                    .map(p -> horasPorParticipacion.getOrDefault(p.getId(), List.of()))
                    .filter(h -> !h.isEmpty())
                    .map(AnalyticsService::minutosActivos)
                    .toList();
            String tiempoActivoPromedio = minutosPorClase.isEmpty() ? "—"
                    : AnalyticsService.formatearMinutos(Math.round(minutosPorClase.stream().mapToLong(Long::longValue).average().orElse(0)));

            directorio.add(new EstudianteDirectorioDto(
                    est.getId(),
                    est.getNombre() != null && !est.getNombre().isBlank() ? est.getNombre() : "Estudiante",
                    est.getCorreo(),
                    totalClases,
                    clasesActivas,
                    ultimaActividad,
                    clasesCompletadas,
                    tiempoActivoPromedio
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
            String nombreEst = estudiante.getNombre();
            String correoEst = estudiante.getCorreo();

            List<ProfesorEstudiante> todosVinculos = profesorEstudianteRepository.findByEstudiante(estudiante);
            List<Participacion> todasParticipaciones = participacionRepository.findByEstudianteOrderByCreatedAtDesc(estudiante);

            // Participaciones del estudiante en sesiones de ESTE docente
            List<Participacion> participacionesConDocente = todasParticipaciones.stream()
                    .filter(p -> p.getSesion() != null && p.getSesion().getProfesor() != null && p.getSesion().getProfesor().getId().equals(profesor.getId()))
                    .collect(Collectors.toList());

            // Autorización: solo puede eliminar a estudiantes de su cartera o con participaciones en sus sesiones
            boolean perteneceAEsteDocente = !participacionesConDocente.isEmpty()
                    || todosVinculos.stream().anyMatch(pe -> pe.getProfesor() != null && pe.getProfesor().getId().equals(profesor.getId()));
            if (!perteneceAEsteDocente) {
                log.warn("Acceso denegado: el docente {} intentó eliminar al estudiante {} que no pertenece a su cartera",
                        profesor.getCorreo(), estudianteId);
                return "redirect:/docente/estudiantes";
            }

            // La cuenta solo se borra de la plataforma si no pertenece a otro docente (cartera o participaciones)
            boolean perteneceAOtroDocente = todasParticipaciones.size() > participacionesConDocente.size()
                    || todosVinculos.stream()
                    .anyMatch(pe -> pe.getProfesor() != null && !pe.getProfesor().getId().equals(profesor.getId()));

            if (!participacionesConDocente.isEmpty()) {
                mensajeRepository.deleteByParticipacionIn(participacionesConDocente);
                metricaRepository.deleteByParticipacionIn(participacionesConDocente);
                participacionRepository.deleteAll(participacionesConDocente);
                participacionRepository.flush();
            }

            // Eliminar de la cartera de este docente los vínculos gestionados en sesión
            List<ProfesorEstudiante> vinculosAEliminar = todosVinculos.stream()
                    .filter(pe -> pe.getProfesor() != null && pe.getProfesor().getId().equals(profesor.getId()))
                    .collect(Collectors.toList());

            if (!vinculosAEliminar.isEmpty()) {
                profesorEstudianteRepository.deleteAll(vinculosAEliminar);
                profesorEstudianteRepository.flush();
            } else {
                profesorEstudianteRepository.deleteByProfesorAndEstudiante(profesor, estudiante);
                profesorEstudianteRepository.flush();
            }

            if (perteneceAOtroDocente) {
                log.info("Docente {} desvinculó de su cartera al estudiante {} ({}) que también pertenece a otros docentes",
                        profesor.getCorreo(), nombreEst, correoEst);
                return "redirect:/docente/estudiantes?estudianteEliminado=true";
            }

            // Si solo pertenecía a este docente, eliminar cuenta de la plataforma
            estudianteRepository.delete(estudiante);
            estudianteRepository.flush();
            log.info("Docente {} eliminó al estudiante {} ({}) de la plataforma",
                    profesor.getCorreo(), nombreEst, correoEst);
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
