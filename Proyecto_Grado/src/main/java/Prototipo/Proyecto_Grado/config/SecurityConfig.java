package Prototipo.Proyecto_Grado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Configuración de seguridad inicial con Spring Security.
 * Permite la navegación en el prototipo durante el desarrollo y habilita recursos estáticos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos (CSS, JS, imágenes)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Página de error de Spring Boot: sin esto, cualquier error (p. ej. un 400) se mostraba como 403 "Access Denied"
                .requestMatchers("/error").permitAll()
                // Rutas del prototipo accesibles durante el desarrollo
                .requestMatchers("/", "/login/**", "/logout", "/chat/**", "/docente/**", "/docente", "/estudiante/**", "/estudiante", "/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

