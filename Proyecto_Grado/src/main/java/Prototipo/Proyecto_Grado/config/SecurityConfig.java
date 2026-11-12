package Prototipo.Proyecto_Grado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Configuración de Spring Security.
 * La autenticación es propia de la aplicación (el login y el registro guardan en la sesión HTTP los atributos
 * usuarioLogueado y rolUsuario), por lo que aquí se desactivan formLogin y httpBasic y se dejan pasar
 * las rutas de la aplicación. El control de acceso por rol lo hacen los interceptores de
 * {@link WebMvcConfig} (RNF-01). Spring Security aporta además el codificador BCrypt y la protección
 * CSRF, que queda activa con su configuración por defecto.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Cadena de filtros: rutas públicas y de la aplicación sin restricción; cualquier otra ruta exige
     * autenticación de Spring Security y, como no hay mecanismo de login configurado, responde 403.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos (CSS, JS, imágenes)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Página de error de Spring Boot: sin esto, cualquier error (p. ej. un 400) se mostraba como 403 "Access Denied"
                .requestMatchers("/error").permitAll()
                // Rutas de la aplicación: el rol lo validan los interceptores de WebMvcConfig y los controladores
                .requestMatchers("/", "/login/**", "/logout", "/chat/**", "/docente/**", "/docente", "/estudiante/**", "/estudiante", "/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /** Codificador BCrypt para las contraseñas de docentes y estudiantes (registro, cambio y verificación). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

