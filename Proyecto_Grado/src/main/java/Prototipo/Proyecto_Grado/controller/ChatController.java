package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.MensajeRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import Prototipo.Proyecto_Grado.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Muestra la interfaz de chat o redirige al formulario de registro (RF-01).
     * Si el estudiante ya tiene sesión HTTP activa, accede directamente al chat.
     */
    @GetMapping("/{enlace}")
    public String verChat(@PathVariable String enlace,
                          @RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "feedbackEnviado", required = false) Boolean feedbackEnviado,
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

        model.addAttribute("sesion", sesion);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("participacionId", participacion.getId());
        model.addAttribute("participacion", participacion);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("feedbackEnviado", Boolean.TRUE.equals(feedbackEnviado));
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
                                      HttpSession httpSession) {

        java.util.Optional<Estudiante> existente = estudianteRepository.findByCorreoIgnoreCase(correo);

        Estudiante estudiante;
        if (existente.isPresent()) {
            // Estudiante ya registrado: validar contraseña
            estudiante = existente.get();
            String passwordAlmacenada = estudiante.getContrasena();

            boolean coincide = false;
            try {
                coincide = passwordEncoder.matches(contrasena, passwordAlmacenada);
            } catch (Exception ignored) {}

            // Fallback: comparación en texto plano para datos de desarrollo/migración
            if (!coincide) {
                coincide = contrasena.equals(passwordAlmacenada);
            }

            if (!coincide) {
                return "redirect:/chat/" + enlace + "?error=credenciales";
            }
        } else {
            // Estudiante nuevo: registrar con contraseña hasheada
            estudiante = estudianteRepository.save(
                    Estudiante.builder()
                            .nombre(nombre)
                            .correo(correo.toLowerCase())
                            .contrasena(passwordEncoder.encode(contrasena))
                            .build()
            );
        }

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
    public String enviarMensaje(@PathVariable String enlace,
                                @RequestParam("mensaje") String mensaje,
                                HttpSession httpSession) {

        Estudiante estudiante = (Estudiante) httpSession.getAttribute("estudianteActivo");
        if (estudiante == null) {
            return "redirect:/chat/" + enlace;
        }

        Sesion sesion = sesionRepository.findByEnlaceUnico(enlace).orElse(null);
        if (sesion != null && Boolean.FALSE.equals(sesion.getEstado())) {
            return "redirect:/chat/" + enlace + "?pausada=true";
        }

        if (mensaje != null && !mensaje.trim().isEmpty()) {
            chatService.procesarMensajeEstudiante(
                    estudiante.getCorreo(), enlace, mensaje.trim(), null);
        }

        return "redirect:/chat/" + enlace;
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
}
