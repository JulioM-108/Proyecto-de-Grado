package Prototipo.Proyecto_Grado.config;

import Prototipo.Proyecto_Grado.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las defensas de seguridad y control de caché:
 * - Cache-Control defensivo (solución al botón "Atrás" tras logout)
 * - Interceptores de autenticación por roles
 * - Validación de dominios institucionales Javerianos
 */
class SecurityAndInterceptorTest {

    @Test
    @DisplayName("CacheControlInterceptor inyecta cabeceras defensivas no-cache, no-store, must-revalidate")
    void testCacheControlHeadersInyectadas() {
        WebMvcConfig.CacheControlInterceptor interceptor = new WebMvcConfig.CacheControlInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docente/sesiones");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertTrue(continuar);
        assertEquals("no-cache, no-store, max-age=0, must-revalidate", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
        assertEquals("0", response.getHeader("Expires"));
    }

    @Test
    @DisplayName("DocenteAuthInterceptor bloquea acceso anónimo y redirige a login")
    void testDocenteAuthInterceptorBloqueaAnonimo() throws Exception {
        WebMvcConfig.DocenteAuthInterceptor interceptor = new WebMvcConfig.DocenteAuthInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docente/sesiones");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals("/login?error=Debes+iniciar+sesion+como+docente", response.getRedirectedUrl());
    }

    @Test
    @DisplayName("DocenteAuthInterceptor responde 401 en endpoints API para peticiones sin sesión")
    void testDocenteAuthInterceptorRetorna401ParaApi() throws Exception {
        WebMvcConfig.DocenteAuthInterceptor interceptor = new WebMvcConfig.DocenteAuthInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/docente/api/prompt-assistant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("No autorizado"));
    }

    @Test
    @DisplayName("DocenteAuthInterceptor permite acceso si el usuario tiene rol DOCENTE activo")
    void testDocenteAuthInterceptorPermiteDocenteAutenticado() throws Exception {
        WebMvcConfig.DocenteAuthInterceptor interceptor = new WebMvcConfig.DocenteAuthInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docente/sesiones");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", "profesor@javerianacali.edu.co");
        session.setAttribute("rolUsuario", "DOCENTE");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertTrue(continuar);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    @DisplayName("EstudianteAuthInterceptor bloquea acceso a estudiantes sin sesión")
    void testEstudianteAuthInterceptorBloqueaAnonimo() throws Exception {
        WebMvcConfig.EstudianteAuthInterceptor interceptor = new WebMvcConfig.EstudianteAuthInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/estudiante");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals("/login?error=Debes+iniciar+sesion+como+estudiante", response.getRedirectedUrl());
    }

    @Test
    @DisplayName("AuthService valida dominios @javerianacali.edu.co y @javeriana.edu.co y rechaza externos")
    void testValidacionDominiosJaverianos() {
        AuthService authService = new AuthService(null, null, new BCryptPasswordEncoder());

        assertTrue(authService.esDominioJaveriano("julio.mazo@javerianacali.edu.co"));
        assertTrue(authService.esDominioJaveriano("estudiante@javeriana.edu.co"));
        assertFalse(authService.esDominioJaveriano("atacante@gmail.com"));
        assertFalse(authService.esDominioJaveriano("externo@hotmail.com"));
        assertFalse(authService.esDominioJaveriano(""));
        assertFalse(authService.esDominioJaveriano(null));
    }

    @Test
    @DisplayName("Estudiantes: solo BCrypt. Docentes: BCrypt o texto plano (docente de prueba creado en Supabase)")
    void testVerificacionPasswordPorRol() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AuthService authService = new AuthService(null, null, encoder);

        String rawPassword = "MiPasswordSegura123";
        String bcryptHash = encoder.encode(rawPassword);

        // 1. Detección de formato BCrypt
        assertTrue(authService.esHashBcrypt(bcryptHash));
        assertFalse(authService.esHashBcrypt(rawPassword));

        // 2. Estudiante: hash BCrypt válido, texto plano siempre rechazado
        assertTrue(authService.verificarPasswordEstudiante(rawPassword, bcryptHash));
        assertFalse(authService.verificarPasswordEstudiante("Incorrecta", bcryptHash));
        assertFalse(authService.verificarPasswordEstudiante(rawPassword, rawPassword));

        // 3. Docente: acepta hash BCrypt y texto plano
        assertTrue(authService.verificarPasswordDocente(rawPassword, bcryptHash));
        assertTrue(authService.verificarPasswordDocente(rawPassword, rawPassword));
        assertFalse(authService.verificarPasswordDocente("Incorrecta", rawPassword));

        // 4. Casos nulos
        assertFalse(authService.verificarPasswordEstudiante(null, bcryptHash));
        assertFalse(authService.verificarPasswordEstudiante(rawPassword, null));
        assertFalse(authService.verificarPasswordDocente(null, rawPassword));
        assertFalse(authService.verificarPasswordDocente(rawPassword, null));
    }
}
