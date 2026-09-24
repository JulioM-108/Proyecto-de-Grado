package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ParticipacionRepository;
import Prototipo.Proyecto_Grado.repository.SesionRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador para el Módulo de Interfaz de Estudiante.
 * Permite visualizar el perfil, ingresar a nuevas clases mediante código/enlace
 * y consultar el historial de clases en las que ha participado.
 */
@Controller
@RequestMapping("/estudiante")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteRepository estudianteRepository;
    private final ParticipacionRepository participacionRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @GetMapping
    public String verPanelEstudiante(
            @RequestParam(value = "perfilActualizado", required = false) Boolean perfilActualizado,
            @RequestParam(value = "feedbackEnviado", required = false) Boolean feedbackEnviado,
            HttpSession session, Model model) {
        String correo = (String) session.getAttribute("usuarioLogueado");
        String rol = (String) session.getAttribute("rolUsuario");

        // Defense-in-depth: si el estudiante entró por enlace de chat pero falta usuarioLogueado,
        // sincronizar automáticamente las variables de sesión estándar.
        if ((correo == null || !"ESTUDIANTE".equalsIgnoreCase(rol))) {
            Estudiante estActivo = (Estudiante) session.getAttribute("estudianteActivo");
            if (estActivo != null) {
                correo = estActivo.getCorreo();
                session.setAttribute("usuarioLogueado", correo);
                session.setAttribute("nombreUsuario", estActivo.getNombre());
                session.setAttribute("usuarioId", estActivo.getId().toString());
                session.setAttribute("rolUsuario", "ESTUDIANTE");
            } else {
                return "redirect:/login";
            }
        }

        Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoIgnoreCase(correo);
        if (estudianteOpt.isEmpty()) {
            return "redirect:/login";
        }

        Estudiante estudiante = estudianteOpt.get();
        session.setAttribute("estudianteActivo", estudiante);

        List<Participacion> participaciones = participacionRepository
                .findByEstudianteWithSesionOrderByCreatedAtDesc(estudiante);

        model.addAttribute("estudiante", estudiante);
        model.addAttribute("participaciones", participaciones);
        model.addAttribute("perfilActualizado", Boolean.TRUE.equals(perfilActualizado));
        model.addAttribute("feedbackEnviado", Boolean.TRUE.equals(feedbackEnviado));
        return "estudiante/clases";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "nuevaContrasena", required = false) String nuevaContrasena,
            HttpSession session) {

        String correo = (String) session.getAttribute("usuarioLogueado");
        if (correo == null) {
            Estudiante estActivo = (Estudiante) session.getAttribute("estudianteActivo");
            if (estActivo != null) {
                correo = estActivo.getCorreo();
            } else {
                return "redirect:/login";
            }
        }

        Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoIgnoreCase(correo);
        if (estudianteOpt.isPresent()) {
            Estudiante est = estudianteOpt.get();
            if (nombre != null && !nombre.isBlank()) {
                est.setNombre(nombre.trim());
                session.setAttribute("nombreUsuario", est.getNombre());
            }
            if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                est.setContrasena(passwordEncoder.encode(nuevaContrasena.trim()));
            }
            estudianteRepository.save(est);
            session.setAttribute("estudianteActivo", est);
            session.setAttribute("usuarioLogueado", est.getCorreo());
            session.setAttribute("nombreUsuario", est.getNombre());
            session.setAttribute("rolUsuario", "ESTUDIANTE");
        }

        return "redirect:/estudiante?perfilActualizado=true";
    }

    @PostMapping("/clases/{participacionId}/feedback")
    public String guardarFeedbackEstudiante(
            @PathVariable UUID participacionId,
            @RequestParam(value = "valoracion", required = false) Integer valoracion,
            @RequestParam(value = "comentario", required = false) String comentario,
            HttpSession session) {

        String correo = (String) session.getAttribute("usuarioLogueado");
        if (correo == null) {
            Estudiante estActivo = (Estudiante) session.getAttribute("estudianteActivo");
            if (estActivo != null) {
                correo = estActivo.getCorreo();
            } else {
                return "redirect:/login";
            }
        }

        participacionRepository.findById(participacionId).ifPresent(p -> {
            if (valoracion != null && valoracion >= 1 && valoracion <= 5) {
                p.setValoracionEstudiante(valoracion);
            }
            if (comentario != null && !comentario.isBlank()) {
                p.setFeedbackEstudiante(comentario.trim());
            }
            participacionRepository.save(p);
        });

        return "redirect:/estudiante?feedbackEnviado=true";
    }
}
