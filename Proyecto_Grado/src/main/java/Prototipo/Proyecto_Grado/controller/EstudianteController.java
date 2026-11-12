package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.dto.EvaluacionSusTamDTO;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador del panel del estudiante: perfil, listado de sus clases (con acceso a cada chat por su
 * enlace), "Feedback Profe" y encuesta SUS+TAM desde el panel.
 * <p>
 * Ruta base: {@code /estudiante}. El interceptor de {@code WebMvcConfig} exige rol ESTUDIANTE y cada
 * endpoint opera solo sobre los datos del estudiante en sesión (RNF-01).
 * <p>
 * Requisitos: RF-01, RF-04, RF-09, RNF-01 y RNF-02.
 */
@Controller
@RequestMapping("/estudiante")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteRepository estudianteRepository;
    private final ParticipacionRepository participacionRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * GET {@code /estudiante}: panel del estudiante con su perfil y sus participaciones, de la más reciente
     * a la más antigua; desde cada una puede retomar el chat (RF-04). Solo lista las participaciones del
     * estudiante en sesión (RNF-01). Los parámetros opcionales solo activan avisos.
     *
     * @return vista {@code estudiante/clases}, o redirección a {@code /login} si no hay estudiante identificable
     */
    @Transactional(readOnly = true)
    @GetMapping
    public String verPanelEstudiante(
            @RequestParam(value = "perfilActualizado", required = false) Boolean perfilActualizado,
            @RequestParam(value = "feedbackEnviado", required = false) Boolean feedbackEnviado,
            HttpSession session, Model model) {
        String correo = (String) session.getAttribute("usuarioLogueado");
        String rol = (String) session.getAttribute("rolUsuario");

        // Defensa en profundidad: si solo existe estudianteActivo, se reconstruyen los atributos estándar.
        // El interceptor ya exige usuarioLogueado y rol ESTUDIANTE, así que en la práctica no se ejecuta.
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
        // Refresca la copia en sesión, que usa ChatController para identificar al estudiante
        session.setAttribute("estudianteActivo", estudiante);

        List<Participacion> participaciones = participacionRepository
                .findByEstudianteWithSesionOrderByCreatedAtDesc(estudiante);

        model.addAttribute("estudiante", estudiante);
        model.addAttribute("participaciones", participaciones);
        model.addAttribute("perfilActualizado", Boolean.TRUE.equals(perfilActualizado));
        model.addAttribute("feedbackEnviado", Boolean.TRUE.equals(feedbackEnviado));
        return "estudiante/clases";
    }

    /**
     * POST {@code /estudiante/perfil/actualizar}: actualiza el nombre y, opcionalmente, la contraseña del
     * estudiante en sesión (hash BCrypt, RNF-02). El correo no se puede cambiar. Los campos vacíos se ignoran.
     * Escribe en la BD y sincroniza los atributos de sesión.
     *
     * @return redirección a {@code /estudiante?perfilActualizado=true}
     */
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

    /**
     * POST {@code /estudiante/clases/{participacionId}/feedback}: "Feedback Profe" desde el panel (RF-09):
     * valoración de 1 a 5 y comentario. Solo guarda si la participación pertenece al estudiante en sesión
     * (RNF-01); los valores fuera de rango o vacíos se ignoran.
     *
     * @return redirección a {@code /estudiante?feedbackEnviado=true}
     */
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

        final String correoFinal = correo;
        participacionRepository.findById(participacionId).ifPresent(p -> {
            if (p.getEstudiante() != null && correoFinal.equalsIgnoreCase(p.getEstudiante().getCorreo())) {
                if (valoracion != null && valoracion >= 1 && valoracion <= 5) {
                    p.setValoracionEstudiante(valoracion);
                }
                if (comentario != null && !comentario.isBlank()) {
                    p.setFeedbackEstudiante(comentario.trim());
                }
                participacionRepository.save(p);
            }
        });

        return "redirect:/estudiante?feedbackEnviado=true";
    }

    /**
     * POST {@code /estudiante/clases/{participacionId}/evaluacion-sus-tam}: encuesta SUS+TAM desde el panel.
     * Valida la escala Likert de 1 a 5, calcula el puntaje SUS y lo guarda como JSON solo si la participación
     * pertenece al estudiante en sesión (RNF-01). Enviar la encuesta finaliza la tutoría de esa clase.
     * Equivale a {@code POST /chat/{enlace}/evaluacion-sus-tam}.
     *
     * @return redirección a {@code /estudiante?evaluacionExitosa=true}, o con {@code errorEvaluacion=rangoInvalido}
     */
    @PostMapping("/clases/{participacionId}/evaluacion-sus-tam")
    public String guardarEvaluacionSusTamEstudiante(
            @PathVariable UUID participacionId,
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
            HttpSession session) {

        if (!validarRangoLikert(sus1, sus2, sus3, sus4, sus5, sus6, sus7, sus8, sus9, sus10,
                tamUtilidad, tamAutonomia, tamFrustracion, tamDialogo, tamVentaja, tamAdopcion)) {
            return "redirect:/estudiante?errorEvaluacion=rangoInvalido";
        }

        String correo = (String) session.getAttribute("usuarioLogueado");
        if (correo == null) {
            Estudiante estActivo = (Estudiante) session.getAttribute("estudianteActivo");
            if (estActivo != null) {
                correo = estActivo.getCorreo();
            } else {
                return "redirect:/login";
            }
        }

        final String correoFinal = correo;
        participacionRepository.findById(participacionId).ifPresent(p -> {
            if (p.getEstudiante() != null && correoFinal.equalsIgnoreCase(p.getEstudiante().getCorreo())) {
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
            }
        });

        return "redirect:/estudiante?evaluacionExitosa=true";
    }

    /** Comprueba que todas las respuestas estén presentes y en la escala Likert de 1 a 5. */
    private boolean validarRangoLikert(Integer... valores) {
        for (Integer v : valores) {
            if (v == null || v < 1 || v > 5) {
                return false;
            }
        }
        return true;
    }
}
