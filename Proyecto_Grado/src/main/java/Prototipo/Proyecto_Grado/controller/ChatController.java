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
import Prototipo.Proyecto_Grado.service.LoginRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SesionRepository sesionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ParticipacionRepository participacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginRateLimiterService rateLimiterService;

    /**
     * Muestra la interfaz de chat o redirige al formulario de registro (RF-01).
     * Si el estudiante ya tiene sesión HTTP activa, accede directamente al chat.
     */
    @GetMapping("/{enlace}")
    public String verChat(@PathVariable String enlace,
                          @RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "modo", required = false) String modo,
                          @RequestParam(value = "feedbackEnviado", required = false) Boolean feedbackEnviado,
                          @RequestParam(value = "evaluacionExitosa", required = false) Boolean evaluacionExitosa,
                          @RequestParam(value = "cerrada", required = false) Boolean cerrada,
                          @RequestParam(value = "completada", required = false) Boolean completada,
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
            model.addAttribute("modoActivo", "login".equalsIgnoreCase(modo) ? "login" : "registro");

            if ("bloqueado".equals(error)) {
                model.addAttribute("error", "Demasiados intentos fallidos. Por seguridad, el acceso ha sido bloqueado temporalmente durante 10 minutos.");
            } else if ("credenciales".equals(error)) {
                model.addAttribute("error", "Contraseña incorrecta. Verifica tus credenciales e intenta de nuevo.");
            } else if ("dominio".equals(error)) {
                model.addAttribute("error", "Debes ingresar con un correo institucional (@javerianacali.edu.co o @javeriana.edu.co).");
            } else if ("no_registrado".equals(error)) {
                model.addAttribute("error", "No existe una cuenta registrada con este correo. Por favor selecciona 'Soy nuevo estudiante' para registrarte.");
                model.addAttribute("modoActivo", "registro");
            } else if ("ya_registrado".equals(error)) {
                model.addAttribute("error", "Este correo ya está registrado en la plataforma. Por favor selecciona 'Ya tengo cuenta' e ingresa tu contraseña.");
                model.addAttribute("modoActivo", "login");
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
        // La existencia de progreso_md (p. ej. generado por el docente) NO cierra la tutoría del estudiante.
        boolean sesionCompletada = participacion.isTutoriaFinalizada()
                || Boolean.TRUE.equals(cerrada)
                || Boolean.TRUE.equals(completada);
        model.addAttribute("sesionCompletada", sesionCompletada);
        model.addAttribute("rutaCompletada", Boolean.TRUE.equals(participacion.getRutaCompletada()));
        model.addAttribute("sesionCerrada", Boolean.TRUE.equals(cerrada) || participacion.getEvaluacionSusTam() != null || participacion.getValoracionEstudiante() != null);
        model.addAttribute("evaluacionSusTam", evaluacionDTO);
        model.addAttribute("evaluacionCompletada", evaluacionDTO != null);
        model.addAttribute("evaluacionExitosa", Boolean.TRUE.equals(evaluacionExitosa));
        return "estudiante/chat";
    }

    /**
     * RF-01: Registro/autenticación del estudiante al acceder por enlace único.
     * Soporta dos modos dentro de la misma interfaz:
     * - "login": Estudiante con cuenta previa; solo requiere correo institucional y contraseña.
     * - "registro": Estudiante nuevo; requiere nombre completo, correo institucional y contraseña.
     */
    @PostMapping("/{enlace}/registro")
    public String registrarEstudiante(@PathVariable String enlace,
                                      @RequestParam(value = "nombre", required = false) String nombre,
                                      @RequestParam("correo") String correo,
                                      @RequestParam("contrasena") String contrasena,
                                      @RequestParam(value = "modo", required = false, defaultValue = "registro") String modo,
                                      jakarta.servlet.http.HttpServletRequest request,
                                      HttpSession httpSession) {

        String correoLimpio = correo != null ? correo.trim().toLowerCase() : "";
        if (!authService.esDominioJaveriano(correoLimpio)) {
            return "redirect:/chat/" + enlace + "?error=dominio&modo=" + modo;
        }

        // Mismo límite de intentos que el login principal (5 fallos → bloqueo de 10 min por correo + IP)
        String claveRateLimit = correoLimpio + "_" + request.getRemoteAddr();
        if (rateLimiterService.estaBloqueado(claveRateLimit)) {
            return "redirect:/chat/" + enlace + "?error=bloqueado&modo=" + modo;
        }

        java.util.Optional<Estudiante> existente = estudianteRepository.findByCorreoIgnoreCase(correoLimpio);

        Estudiante estudiante;
        if (existente.isPresent()) {
            estudiante = existente.get();
            if (!authService.verificarPasswordEstudiante(contrasena, estudiante.getContrasena())) {
                rateLimiterService.registrarIntentoFallido(claveRateLimit);
                return "redirect:/chat/" + enlace + "?error=credenciales&modo=" + modo;
            }
            rateLimiterService.limpiarIntentos(claveRateLimit);
        } else {
            // Si intentó ingresar en modo login con correo inexistente
            if ("login".equalsIgnoreCase(modo)) {
                return "redirect:/chat/" + enlace + "?error=no_registrado&modo=login";
            }

            // Registro de nuevo estudiante
            String nombreValido = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "Estudiante";
            estudiante = estudianteRepository.save(
                    Estudiante.builder()
                            .nombre(nombreValido)
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
        if (sesion == null) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La sesión de clase ya no existe."));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/estudiante")).build();
        }
        if (Boolean.FALSE.equals(sesion.getEstado())) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Esta sesión ha sido pausada por el profesor"));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?pausada=true")).build();
        }

        // Validación de sesión cerrada / finalizada para el estudiante
        Participacion partActiva = participacionRepository
                .findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion)
                .orElse(null);

        if (partActiva != null && partActiva.isTutoriaFinalizada()) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "La sesión de tutoría ha finalizado. No es posible enviar más preguntas."
                ));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?cerrada=true")).build();
        }

        // Validación de longitud máxima del payload (2500 caracteres)
        if (mensaje != null && mensaje.trim().length() > 2500) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El mensaje excede el límite máximo permitido (2500 caracteres)."));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?errorLongitud=true")).build();
        }

        // Rate limiting defensivo (debounce de 1.5s entre mensajes por estudiante)
        Long ultimoEnvioMs = (Long) httpSession.getAttribute("ultimoEnvioChatMs");
        long ahoraMs = System.currentTimeMillis();
        if (ultimoEnvioMs != null && (ahoraMs - ultimoEnvioMs) < 1500) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Por favor espera un momento antes de enviar otra consulta."));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace)).build();
        }
        httpSession.setAttribute("ultimoEnvioChatMs", ahoraMs);

        MensajeDto respuestaTutor = null;
        if (mensaje != null && !mensaje.trim().isEmpty()) {
            respuestaTutor = chatService.procesarMensajeEstudiante(
                    estudiante.getCorreo(), enlace, mensaje.trim(), null);
        }

        // La IA no respondió: no se guardó nada, el estudiante conserva su mensaje y puede reintentar
        if (respuestaTutor != null && respuestaTutor.isErrorIa()) {
            if (esAjax) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                        "success", false,
                        "reintentable", true,
                        "error", respuestaTutor.getContenido()
                ));
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/chat/" + enlace + "?errorIa=true")).build();
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
                .ifPresent(p -> {
                    // Al cerrar sesión sin terminar la ruta pedagógica, no se genera diagnóstico con IA.
                    // Las métricas cuantitativas (turnos M-02, alertas RF-10) ya quedan acumuladas
                    // en la base de datos de manera inmediata con cada consulta enviada.
                    log.info("Estudiante {} cerró su sesión en la clase {}", estudiante.getCorreo(), sesion.getTema());
                });

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
                    log.info("Estudiante {} envió feedback de la clase {}. Sesión finalizada.", estudiante.getCorreo(), sesion.getTema());
                });

        return "redirect:/chat/" + enlace + "?cerrada=true&feedbackEnviado=true";
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
                    log.info("Estudiante {} completó la evaluación SUS/TAM para la clase {}. Sesión finalizada.", estudiante.getCorreo(), sesion.getTema());
                });

        return "redirect:/chat/" + enlace + "?cerrada=true&evaluacionExitosa=true";
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
