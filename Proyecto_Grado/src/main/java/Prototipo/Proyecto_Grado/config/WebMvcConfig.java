package Prototipo.Proyecto_Grado.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC: registra tres interceptores.
 * 1. Cache-Control defensivo: añade cabeceras 'no-cache, no-store' a todas las páginas dinámicas para que
 *    el navegador no guarde copias y el botón "Atrás" no muestre páginas privadas tras cerrar sesión.
 * 2. Control de acceso por rol (RNF-01): exige sesión con rol DOCENTE en las rutas /docente y con rol
 *    ESTUDIANTE en las rutas /estudiante, a partir de los atributos usuarioLogueado y rolUsuario.
 * Las rutas /chat no pasan por estos interceptores; ChatController valida al estudiante en la sesión.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Cabeceras anti-caché en todas las respuestas dinámicas (se excluyen los recursos estáticos)
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico");

        // Módulo docente: requiere rol DOCENTE (RNF-01)
        registry.addInterceptor(new DocenteAuthInterceptor())
                .addPathPatterns("/docente", "/docente/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");

        // Módulo estudiante: requiere rol ESTUDIANTE
        registry.addInterceptor(new EstudianteAuthInterceptor())
                .addPathPatterns("/estudiante", "/estudiante/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");
    }

    /** Desactiva la caché del navegador y de proxies (HTTP/1.1, HTTP/1.0 y Expires) para la respuesta. */
    public static class CacheControlInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            return true;
        }
    }

    /**
     * Deja pasar solo a usuarios con sesión de rol DOCENTE. Si no lo está, las rutas /docente/api/...
     * reciben 401 con un JSON de error y el resto se redirige al login.
     */
    public static class DocenteAuthInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            HttpSession session = request.getSession(false);
            boolean autorizado = session != null
                    && session.getAttribute("usuarioLogueado") != null
                    && "DOCENTE".equalsIgnoreCase((String) session.getAttribute("rolUsuario"));

            if (!autorizado) {
                String uri = request.getRequestURI();
                if (uri.contains("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":false,\"error\":\"No autorizado. Debes iniciar sesión como docente.\"}");
                    return false;
                }
                response.sendRedirect("/login?error=Debes+iniciar+sesion+como+docente");
                return false;
            }
            return true;
        }
    }

    /** Deja pasar solo a usuarios con sesión de rol ESTUDIANTE; en otro caso redirige al login. */
    public static class EstudianteAuthInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            HttpSession session = request.getSession(false);
            boolean autorizado = session != null
                    && session.getAttribute("usuarioLogueado") != null
                    && "ESTUDIANTE".equalsIgnoreCase((String) session.getAttribute("rolUsuario"));

            if (!autorizado) {
                response.sendRedirect("/login?error=Debes+iniciar+sesion+como+estudiante");
                return false;
            }
            return true;
        }
    }
}
