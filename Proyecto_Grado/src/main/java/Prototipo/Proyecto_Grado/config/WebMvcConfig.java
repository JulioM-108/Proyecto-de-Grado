package Prototipo.Proyecto_Grado.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC.
 * 1. Cache-Control defensivo: Inyecta cabeceras 'no-cache, no-store' en todas las páginas dinámicas
 *    para evitar que navegadores guarden copias en caché y solucionando la vulnerabilidad del botón "Atrás" tras logout.
 * 2. Control de Acceso por Roles: Intercepta rutas de docente y estudiante para impedir acceso no autenticado.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Inyecta cabeceras Cache-Control en todas las respuestas dinámicas
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico");

        // Seguridad para el módulo docente
        registry.addInterceptor(new DocenteAuthInterceptor())
                .addPathPatterns("/docente", "/docente/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");

        // Seguridad para el módulo estudiante
        registry.addInterceptor(new EstudianteAuthInterceptor())
                .addPathPatterns("/estudiante", "/estudiante/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");
    }

    public static class CacheControlInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            return true;
        }
    }

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
