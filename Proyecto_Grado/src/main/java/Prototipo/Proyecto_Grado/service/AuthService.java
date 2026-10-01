package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de autenticación que valida usuarios contra las tablas "Estudiante" y "Profesor"
 * en Supabase. Ambas tablas tienen la columna "contrase\u00f1a" (varchar) para la contraseña.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final PasswordEncoder passwordEncoder;

    public record UsuarioAutenticado(UUID id, String nombre, String correo, String rol) {}

    /**
     * Autentica las credenciales de un usuario contra la tabla correspondiente en Supabase.
     *
     * @param correo   correo institucional del usuario
     * @param password contraseña ingresada en texto plano
     * @param rol      "DOCENTE" o "ESTUDIANTE"
     */
    public UsuarioAutenticado autenticar(String correo, String password, String rol) {
        validarDominioJaveriano(correo);
        String emailLimpio = correo.trim().toLowerCase();

        if ("DOCENTE".equalsIgnoreCase(rol)) {
            Optional<Profesor> profesorOpt = profesorRepository.findByCorreoIgnoreCase(emailLimpio);
            if (profesorOpt.isEmpty()) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            Profesor prof = profesorOpt.get();
            if (!verificarYMigrarPasswordProfesor(prof, password)) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            return new UsuarioAutenticado(prof.getId(), prof.getNombre(), prof.getCorreo(), "DOCENTE");

        } else {
            // Por defecto: ESTUDIANTE
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoIgnoreCase(emailLimpio);
            if (estudianteOpt.isEmpty()) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            Estudiante est = estudianteOpt.get();
            if (!verificarYMigrarPasswordEstudiante(est, password)) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            return new UsuarioAutenticado(est.getId(), est.getNombre(), est.getCorreo(), "ESTUDIANTE");
        }
    }

    /**
     * Verifica y actualiza transparentemente a BCrypt si la contraseña almacenada estaba en texto plano.
     */
    private boolean verificarYMigrarPasswordProfesor(Profesor prof, String passwordIngresada) {
        if (!verificarPassword(passwordIngresada, prof.getContrasena())) {
            return false;
        }
        if (!esHashBcrypt(prof.getContrasena()) && profesorRepository != null) {
            try {
                prof.setContrasena(passwordEncoder.encode(passwordIngresada));
                profesorRepository.save(prof);
                log.info("Contraseña de docente {} migrada exitosamente a hash BCrypt.", prof.getCorreo());
            } catch (Exception e) {
                log.warn("No se pudo persistir la migración a BCrypt para docente {}: {}", prof.getCorreo(), e.getMessage());
            }
        }
        return true;
    }

    /**
     * Verifica y actualiza transparentemente a BCrypt si la contraseña almacenada estaba en texto plano.
     */
    private boolean verificarYMigrarPasswordEstudiante(Estudiante est, String passwordIngresada) {
        if (!verificarPassword(passwordIngresada, est.getContrasena())) {
            return false;
        }
        if (!esHashBcrypt(est.getContrasena()) && estudianteRepository != null) {
            try {
                est.setContrasena(passwordEncoder.encode(passwordIngresada));
                estudianteRepository.save(est);
                log.info("Contraseña de estudiante {} migrada exitosamente a hash BCrypt.", est.getCorreo());
            } catch (Exception e) {
                log.warn("No se pudo persistir la migración a BCrypt para estudiante {}: {}", est.getCorreo(), e.getMessage());
            }
        }
        return true;
    }

    /**
     * Comprueba si el correo pertenece al dominio oficial de la Javeriana.
     */
    public boolean esDominioJaveriano(String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        String email = correo.trim().toLowerCase();
        return email.endsWith("@javerianacali.edu.co") || email.endsWith("@javeriana.edu.co");
    }

    /**
     * Exige que el correo pertenezca al dominio oficial de la Javeriana.
     */
    public void validarDominioJaveriano(String correo) {
        if (!esDominioJaveriano(correo)) {
            throw new IllegalArgumentException(
                    "Solo se aceptan correos institucionales de la Pontificia Universidad Javeriana (@javerianacali.edu.co).");
        }
    }

    /**
     * Determina si la contraseña almacenada tiene formato de hash BCrypt ($2a$, $2b$ o $2y$).
     */
    public boolean esHashBcrypt(String passwordAlmacenada) {
        return passwordAlmacenada != null && (
                passwordAlmacenada.startsWith("$2a$") ||
                passwordAlmacenada.startsWith("$2b$") ||
                passwordAlmacenada.startsWith("$2y$")
        );
    }

    /**
     * Verifica la contraseña contra el valor almacenado.
     * Soporta hashes BCrypt y realiza fallback transparente para contraseñas heredadas en texto plano,
     * evitando advertencias innecesarias en el log de Spring Security.
     */
    public boolean verificarPassword(String passwordIngresada, String passwordAlmacenada) {
        if (passwordAlmacenada == null || passwordIngresada == null) {
            return false;
        }

        if (esHashBcrypt(passwordAlmacenada)) {
            try {
                return passwordEncoder.matches(passwordIngresada, passwordAlmacenada);
            } catch (Exception e) {
                log.warn("Error al verificar contraseña con BCrypt: {}", e.getMessage());
                return false;
            }
        }

        // Fallback para usuarios heredados cuya contraseña fue creada originalmente en texto plano
        return passwordIngresada.equals(passwordAlmacenada);
    }
}
