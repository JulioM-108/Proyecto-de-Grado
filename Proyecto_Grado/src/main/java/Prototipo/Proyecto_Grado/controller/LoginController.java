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
 * Controlador del login único con rol (estudiante o docente), del segundo factor del docente y del logout.
 * <p>
 * Rutas: {@code /login}, {@code /login/2fa} y {@code /logout}. Valida las credenciales contra las tablas
 * "Estudiante" y "Profesor" de Supabase mediante {@link AuthService}, que solo admite los dominios
 * {@code @javerianacali.edu.co} y {@code @javeriana.edu.co}. Bloquea el acceso 10 minutos tras 5 intentos
 * fallidos (clave correo+IP) y renueva el id de sesión al autenticar, contra la fijación de sesión.
 * <p>
 * Requisitos: RNF-01 y RNF-02.
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final TotpService totpService;
    private final Prototipo.Proyecto_Grado.service.LoginRateLimiterService rateLimiterService;

    /**
     * GET {@code /login}: formulario de inicio de sesión. Muestra el error recibido por parámetro
     * (p. ej. el que envían los interceptores de rol) o la confirmación de cierre de sesión.
     *
     * @return vista {@code Login/login}
     */
    @GetMapping("/login")
    public String verLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null)  model.addAttribute("error", error);
        if (logout != null) model.addAttribute("mensaje", "Has cerrado sesión correctamente.");

        return "Login/login";
    }

    /**
     * POST {@code /login}: autentica con correo, contraseña y rol. Comprueba primero el bloqueo por
     * intentos fallidos y registra cada fallo. Si el docente tiene el 2FA activo, deja el correo como
     * pendiente en la sesión y redirige a {@code /login/2fa} sin autenticarlo todavía.
     *
     * @return redirección a {@code /docente}, {@code /estudiante} o {@code /login/2fa};
     *         o la vista {@code Login/login} con el error si el acceso falla
     */
    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam("correo") String correo,
            @RequestParam("password") String password,
            @RequestParam(value = "rol", defaultValue = "ESTUDIANTE") String rol,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        // Misma clave correo+IP que el acceso desde /chat/{enlace}/registro: ambos comparten el contador
        String claveRateLimit = (correo != null ? correo.trim().toLowerCase() : "") + "_" + request.getRemoteAddr();
        if (rateLimiterService.estaBloqueado(claveRateLimit)) {
            model.addAttribute("error", "Demasiados intentos fallidos. Por seguridad, el acceso ha sido bloqueado temporalmente durante 10 minutos.");
            model.addAttribute("correoIngresado", correo);
            model.addAttribute("rolSeleccionado", rol);
            return "Login/login";
        }

        try {
            AuthService.UsuarioAutenticado usuario = authService.autenticar(correo, password, rol);

            // Éxito: limpiar intentos previos
            rateLimiterService.limpiarIntentos(claveRateLimit);

            // Si es docente, comprobar si tiene activado el Doble Factor (2FA)
            if ("DOCENTE".equalsIgnoreCase(usuario.rol())) {
                var profOpt = profesorRepository.findByCorreoIgnoreCase(usuario.correo());
                if (profOpt.isPresent() && Boolean.TRUE.equals(profOpt.get().getDosFactorActivo())) {
                    // Aún no se fijan los atributos de usuario: sin ellos el interceptor bloquea /docente
                    // hasta validar el código. El id de sesión se renueva al completar el 2FA.
                    session.setAttribute("docentePendiente2fa", usuario.correo());
                    session.setAttribute("intentosFallidos2fa", 0);
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
            rateLimiterService.registrarIntentoFallido(claveRateLimit);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("correoIngresado", correo);
            model.addAttribute("rolSeleccionado", rol);
            return "Login/login";
        }
    }

    // ─── Verificación del Segundo Factor (2FA) ───────────────────────────────

    /**
     * GET {@code /login/2fa}: formulario del código TOTP. Solo accesible con un docente pendiente de 2FA
     * en la sesión, es decir, tras superar la contraseña en {@code POST /login}.
     *
     * @return vista {@code Login/verificar_2fa}, o redirección a {@code /login}
     */
    @GetMapping("/login/2fa")
    public String verVerificacion2Fa(HttpSession session, Model model) {
        String correoPendiente = (String) session.getAttribute("docentePendiente2fa");
        if (correoPendiente == null) {
            return "redirect:/login";
        }
        model.addAttribute("correo", correoPendiente);
        return "Login/verificar_2fa";
    }

    /**
     * POST {@code /login/2fa}: valida el código TOTP del docente pendiente. Admite 5 intentos, contados
     * en la sesión; al quinto fallo descarta el estado pendiente y obliga a reiniciar el login.
     * Si el código es válido, renueva el id de sesión y fija los atributos del docente.
     *
     * @return redirección a {@code /docente}; o la vista de 2FA o de login con el error
     */
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
            int intentos = (session.getAttribute("intentosFallidos2fa") != null)
                    ? (int) session.getAttribute("intentosFallidos2fa") + 1 : 1;
            session.setAttribute("intentosFallidos2fa", intentos);

            if (intentos >= 5) {
                session.removeAttribute("docentePendiente2fa");
                session.removeAttribute("intentosFallidos2fa");
                model.addAttribute("error", "Demasiados intentos incorrectos de 2FA. Por seguridad, debes iniciar sesión de nuevo.");
                return "Login/login";
            }

            model.addAttribute("error", "Código de autenticación incorrecto o expirado (intento " + intentos + " de 5). Verifica tu app autenticadora.");
            model.addAttribute("correo", correoPendiente);
            return "Login/verificar_2fa";
        }

        // 2FA Exitoso: rotar sesión y establecer sesión docente
        request.changeSessionId();
        session.removeAttribute("docentePendiente2fa");
        session.removeAttribute("intentosFallidos2fa");
        session.setAttribute("usuarioLogueado", profesor.getCorreo());
        session.setAttribute("nombreUsuario",   profesor.getNombre());
        session.setAttribute("usuarioId",       profesor.getId().toString());
        session.setAttribute("rolUsuario",      "DOCENTE");

        return "redirect:/docente";
    }

    /**
     * Fija los atributos de sesión que usan los interceptores y controladores. Para el estudiante guarda
     * además la entidad en {@code estudianteActivo}, que es lo que ChatController usa para identificarlo.
     */
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

    /**
     * GET {@code /logout}: invalida la sesión HTTP de cualquier rol.
     *
     * @return redirección a {@code /login?logout=true}
     */
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
