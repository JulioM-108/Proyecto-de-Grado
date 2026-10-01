package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO;
import Prototipo.Proyecto_Grado.model.dto.MensajeDto;
import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import Prototipo.Proyecto_Grado.service.AuthService;
import Prototipo.Proyecto_Grado.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador MVC para el Módulo de Tutoría Socrática (Estudiante).
 * Implementa RF-01 (registro mínimo por enlace único) y el Flujo 3 (cierre de sesión).
 *
 * GET  /chat/{enlace}            → muestra registro o chat
 * POST /chat/{enlace}/registro   → registro con contraseña del estudiante
 * POST /chat/{enlace}/mensaje    → procesa un nuevo mensaje
 * POST /chat/{enlace}/cerrar     → cierra la sesión y genera síntesis (Flujo 3)
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SesionRepository sesionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    /**
     * Muestra la interfaz de chat o redirige al formulario de registro (RF-01).
     * Si el estudiante ya tiene sesión HTTP activa, accede directamente al chat.
     */
    @GetMapping("/{enlace}")
    public String verChat(@PathVariable String enlace,
                          @RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "feedbackEnviado", required = false) Boolean feedbackEnviado,
                          @RequestParam(value = "evaluacionExitosa", required = false) Boolean evaluacionExitosa,
                          HttpSession httpSession,
                          Model model) {

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sesión no encontrada"));

        // RF-01: Verificar si hay estudiante en sesión HTTP
        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        if (estudiante == null) {
            model.addAttribute("enlace", enlace);
            model.addAttribute("tituloSesion", sesion.getTema());
            // Mostrar error de credenciales si aplica
            if ("credenciales".equals(error)) {
                model.addAttribute("error", "Contraseña incorrecta. Verifica tus credenciales e intenta de nuevo.");
            }
            return "estudiante/registro";
        }

        // Obtener o crear la participación del estudiante en esta sesión
        Participacion participacion = participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .orElseGet(() -> chatService.crearParticipacion(estudiante, sesion));

        // Cargar historial de mensajes
        java.util.List<Mensaje> mensajes = mensajeRepository
                .findByParticipacionOrderByCreatedAtAsc(participacion);

        EvaluacionSusTamDTO evaluacionDTO = participacion.getEvaluacionSusTamDTO();
        model.addAttribute("sesion", sesion);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("participacionId", participacion.getId());
        model.addAttribute("participacion", participacion);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("feedbackEnviado", Boolean.TRUE.equals(feedbackEnviado));
        boolean sesionCompletada = participacion.getProgresoMd() != null && !participacion.getProgresoMd().isBlank();
        model.addAttribute("sesionCompletada", sesionCompletada);
        model.addAttribute("evaluacionSusTam", evaluacionDTO);
        model.addAttribute("evaluacionCompletada", evaluacionDTO != null);
        model.addAttribute("evaluacionExitosa", Boolean.TRUE.equals(evaluacionExitosa));
        return "estudiante/chat";
    }

    /**
     * RF-01: Registro/autenticación del estudiante al acceder por enlace único.
     * Si ya existe un estudiante con ese correo, valida su contraseña.
     * Si es nuevo, lo registra con la contraseña hasheada con BCrypt.
     * Sincroniza todas las variables de sesión HTTP para compatibilidad con EstudianteController.
     */
    @PostMapping("/{enlace}/registro")
    public String registrarEstudiante(@PathVariable String enlace,
                                      @RequestParam String nombre,
                                      @RequestParam String correo,
                                      @RequestParam String contrasena,
                                      jakarta.servlet.http.HttpServletRequest request,
                                      HttpSession httpSession) {

        String correoLimpio = correo != null ? correo.trim().toLowerCase() : "";
        if (!authService.esDominioJaveriano(correoLimpio)) {
            return "redirect:/chat/" + enlace + "?error=dominio";
        }

        java.util.Optional<Estudiante> existente = estudianteRepository.findByCorreoIgnoreCase(correoLimpio);

        Estudiante estudiante;
        if (existente.isPresent()) {
            // Estudiante ya registrado: validar contraseña mediante AuthService (elimina duplicación)
            estudiante = existente.get();
            String passwordAlmacenada = estudiante.getContrasena();

            boolean coincide = authService.verificarPassword(contrasena, passwordAlmacenada);

            if (!coincide) {
                return "redirect:/chat/" + enlace + "?error=credenciales";
            }

            // Migración segura y transparente a BCrypt si estaba en texto plano
            if (passwordAlmacenada != null && !passwordAlmacenada.startsWith("$2a$") && !passwordAlmacenada.startsWith("$2b$")) {
                estudiante.setContrasena(passwordEncoder.encode(contrasena));
                estudianteRepository.save(estudiante);
            }
        } else {
            // Estudiante nuevo: registrar con contraseña hasheada
            estudiante = estudianteRepository.save(
                    Estudiante.builder()
                            .nombre(nombre.trim())
                            .correo(correoLimpio)
                            .contrasena(passwordEncoder.encode(contrasena))
                            .build()
            );
        }

        // Rotar ID de sesión para prevenir Session Fixation
        request.changeSessionId();

        // Sincronizar todas las variables de sesión HTTP
        httpSession.setAttribute("estudianteActivo", estudiante);
        httpSession.setAttribute("usuarioLogueado", estudiante.getCorreo());
        httpSession.setAttribute("nombreUsuario", estudiante.getNombre());
        httpSession.setAttribute("usuarioId", estudiante.getId().toString());
        httpSession.setAttribute("rolUsuario", "ESTUDIANTE");

        return "redirect:/chat/" + enlace;
    }

    /**
     * Procesa el mensaje enviado por el estudiante en el chat socrático.
     */
    @PostMapping("/{enlace}/mensaje")
    public ResponseEntity<?> enviarMensaje(@PathVariable String enlace,
                                           @RequestParam("mensaje") String mensaje,
                                           HttpServletRequest request,
                                           HttpSession httpSession) {

        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        boolean esAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (estudiante == null) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Sesión de estudiante expirada"));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace)).build();
        }

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace).orElse(null);
        if (sesion != null && Boolean.FALSE.equals(sesion.getEstado())) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Esta sesión ha sido pausada por el profesor"));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?pausada=true")).build();
        }

        MensajeDto respuestaTutor = null;
        if (mensaje != null && !mensaje.trim().isEmpty()) {
            respuestaTutor = chatService.procesarMensajeEstudiante(
                    estudiante.getCorreo(), enlace, mensaje.trim(), null);
        }

        if (esAjax) {
            String horaActual = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .format(java.time.LocalTime.now());
            java.util.Map<String, Object> respMap = new java.util.HashMap<>();
            respMap.put("success", true);
            respMap.put("tutorContenido", respuestaTutor != null ? respuestaTutor.getContenido() : "");
            respMap.put("hora", horaActual);
            respMap.put("sesionCompletada", respuestaTutor != null && respuestaTutor.isSesionCompletada());
            return ResponseEntity.ok(respMap);
        }

        if (respuestaTutor != null && respuestaTutor.isSesionCompletada()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?completada=true")).build();
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace)).build();
    }

    /**
     * Flujo 3: Cierra la sesión activa y genera la síntesis pedagógica.
     * Llama a Gemini para producir el resumen de 4 secciones en Markdown.
     */
    @PostMapping("/{enlace}/cerrar")
    public String cerrarSesion(@PathVariable String enlace,
                               HttpSession httpSession) {

        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        if (estudiante == null) {
            return "redirect:/chat/" + enlace;
        }

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .ifPresent(p -> chatService.cerrarSesionYGenerarProgreso(p.getId()));

        return "redirect:/chat/" + enlace + "?cerrada=true";
    }

    /**
     * Registro / actualización de retroalimentación cualitativa y valoración (1 a 5)
     * del estudiante para el profesor sobre la sesión.
     */
    @PostMapping("/{enlace}/feedback")
    public String guardarFeedback(@PathVariable String enlace,
                                  @RequestParam(value = "valoracion", required = false) Integer valoracion,
                                  @RequestParam(value = "comentario", required = false) String comentario,
                                  HttpSession httpSession) {

        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        if (estudiante == null) {
            return "redirect:/chat/" + enlace;
        }

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .ifPresent(p -> {
                    if (valoracion != null && valoracion >= 1 && valoracion <= 5) {
                        p.setValoracionEstudiante(valoracion);
                    }
                    if (comentario != null && !comentario.isBlank()) {
                        p.setFeedbackEstudiante(comentario.trim());
                    }
                    participacionRepository.save(p);
                });

        return "redirect:/chat/" + enlace + "?feedbackEnviado=true";
    }

    /**
     * Registro de la evaluación científica de usabilidad (SUS) y aceptación socrática (TAM).
     * Recibe los 10 ítems SUS, 6 ítems TAM y comentario cualitativo, calcula el puntaje
     * estandarizado de Brooke (1986) y persiste la estructura en formato JSON.
     */
    @PostMapping("/{enlace}/evaluacion-sus-tam")
    public String guardarEvaluacionSusTam(
            @PathVariable String enlace,
            @RequestParam("sus1") Integer sus1,
            @RequestParam("sus2") Integer sus2,
            @RequestParam("sus3") Integer sus3,
            @RequestParam("sus4") Integer sus4,
            @RequestParam("sus5") Integer sus5,
            @RequestParam("sus6") Integer sus6,
            @RequestParam("sus7") Integer sus7,
            @RequestParam("sus8") Integer sus8,
            @RequestParam("sus9") Integer sus9,
            @RequestParam("sus10") Integer sus10,
            @RequestParam("tamUtilidad") Integer tamUtilidad,
            @RequestParam("tamAutonomia") Integer tamAutonomia,
            @RequestParam("tamFrustracion") Integer tamFrustracion,
            @RequestParam("tamDialogo") Integer tamDialogo,
            @RequestParam("tamVentaja") Integer tamVentaja,
            @RequestParam("tamAdopcion") Integer tamAdopcion,
            @RequestParam(value = "comentario", required = false) String comentario,
            HttpSession httpSession) {

        if (!validarRangoLikert(sus1, sus2, sus3, sus4, sus5, sus6, sus7, sus8, sus9, sus10,
                tamUtilidad, tamAutonomia, tamFrustracion, tamDialogo, tamVentaja, tamAdopcion)) {
            return "redirect:/chat/" + enlace + "?errorEvaluacion=rangoInvalido";
        }

        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        if (estudiante == null) {
            return "redirect:/chat/" + enlace;
        }

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .ifPresent(p -> {
                    List<Integer> susRespuestas = List.of(sus1, sus2, sus3, sus4, sus5, sus6, sus7, sus8, sus9, sus10);
                    Double susScore = EvaluacionSusTamDTO.calcularSusScore(susRespuestas);

                    Map<String, Integer> tamRespuestas = Map.of(
                            "utilidadPoo", tamUtilidad,
                            "autonomia", tamAutonomia,
                            "frustracion", tamFrustracion,
                            "claridadDialogo", tamDialogo,
                            "ventajaIa", tamVentaja,
                            "adopcionFutura", tamAdopcion
                    );

                    EvaluacionSusTamDTO dto = EvaluacionSusTamDTO.builder()
                            .susScore(susScore)
                            .susRespuestas(susRespuestas)
                            .tamRespuestas(tamRespuestas)
                            .comentario(comentario != null ? comentario.trim() : "")
                            .fechaRegistro(OffsetDateTime.now().toString())
                            .build();

                    p.setEvaluacionSusTam(dto.toJson());
                    participacionRepository.save(p);
                });

        return "redirect:/chat/" + enlace + "?evaluacionExitosa=true";
    }

    private boolean validarRangoLikert(Integer... valores) {
        for (Integer v : valores) {
            if (v == null || v < 1 || v > 5) {
                return false;
            }
        }
        return true;
    }
}
