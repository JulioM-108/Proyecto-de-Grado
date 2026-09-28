package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import Prototipo.Proyecto_Grado.service.AuthService;
import Prototipo.Proyecto_Grado.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de Inicio de Sesión.
 * Valida credenciales contra las tablas "Estudiante" y "Profesor" de Supabase
 * y restringe a correos institucionales Javerianos.
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final TotpService totpService;

    @GetMapping("/login")
    public String verLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null)  model.addAttribute("error", error);
        if (logout != null) model.addAttribute("mensaje", "Has cerrado sesión correctamente.");

        return "Login/login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam("correo") String correo,
            @RequestParam("password") String password,
            @RequestParam(value = "rol", defaultValue = "ESTUDIANTE") String rol,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        try {
            AuthService.UsuarioAutenticado usuario = authService.autenticar(correo, password, rol);

            // Si es docente, comprobar si tiene activado el Doble Factor (2FA)
            if ("DOCENTE".equalsIgnoreCase(usuario.rol())) {
                var profOpt = profesorRepository.findByCorreoIgnoreCase(usuario.correo());
                if (profOpt.isPresent() && Boolean.TRUE.equals(profOpt.get().getDosFactorActivo())) {
                    // Requiere paso adicional de 2FA
                    session.setAttribute("docentePendiente2fa", usuario.correo());
                    return "redirect:/login/2fa";
                }
            }

            // Rotar ID de sesión para prevenir Session Fixation
            request.changeSessionId();

            // Inicio de sesión directo (sin 2FA o estudiante)
            establecerSesionUsuario(session, usuario);

            if ("DOCENTE".equalsIgnoreCase(usuario.rol())) {
                return "redirect:/docente";
            } else {
                return "redirect:/estudiante";
            }

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("correoIngresado", correo);
            model.addAttribute("rolSeleccionado", rol);
            return "Login/login";
        }
    }

    // ─── Verificación del Segundo Factor (2FA) ───────────────────────────────

    @GetMapping("/login/2fa")
    public String verVerificacion2Fa(HttpSession session, Model model) {
        String correoPendiente = (String) session.getAttribute("docentePendiente2fa");
        if (correoPendiente == null) {
            return "redirect:/login";
        }
        model.addAttribute("correo", correoPendiente);
        return "Login/verificar_2fa";
    }

    @PostMapping("/login/2fa")
    public String procesarVerificacion2Fa(
            @RequestParam("codigo") String codigo,
            jakarta.servlet.http.HttpServletRequest request,
            HttpSession session,
            Model model) {

        String correoPendiente = (String) session.getAttribute("docentePendiente2fa");
        if (correoPendiente == null) {
            return "redirect:/login";
        }

        var profOpt = profesorRepository.findByCorreoIgnoreCase(correoPendiente);
        if (profOpt.isEmpty()) {
            return "redirect:/login";
        }

        var profesor = profOpt.get();
        boolean valido = totpService.validarCodigo(profesor.getSecreto2fa(), codigo);

        if (!valido) {
            model.addAttribute("error", "Código de autenticación incorrecto o expirado. Verifica tu app autenticadora.");
            model.addAttribute("correo", correoPendiente);
            return "Login/verificar_2fa";
        }

        // 2FA Exitoso: rotar sesión y establecer sesión docente
        request.changeSessionId();
        session.removeAttribute("docentePendiente2fa");
        session.setAttribute("usuarioLogueado", profesor.getCorreo());
        session.setAttribute("nombreUsuario",   profesor.getNombre());
        session.setAttribute("usuarioId",       profesor.getId().toString());
        session.setAttribute("rolUsuario",      "DOCENTE");

        return "redirect:/docente";
    }

    private void establecerSesionUsuario(HttpSession session, AuthService.UsuarioAutenticado usuario) {
        session.setAttribute("usuarioLogueado", usuario.correo());
        session.setAttribute("nombreUsuario",   usuario.nombre());
        session.setAttribute("usuarioId",       usuario.id().toString());
        session.setAttribute("rolUsuario",      usuario.rol());

        if ("ESTUDIANTE".equalsIgnoreCase(usuario.rol())) {
            estudianteRepository.findByCorreoIgnoreCase(usuario.correo())
                    .ifPresent(est -> session.setAttribute("estudianteActivo", est));
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
