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
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$w8.5R32y9m3U3rM7.Y1uQeIeS9P9G.bJ4o3a4t9E8p0g2Z7o5r1K.";

    public UsuarioAutenticado autenticar(String correo, String password, String rol) {
        validarDominioJaveriano(correo);
        String emailLimpio = correo.trim().toLowerCase();

        if ("DOCENTE".equalsIgnoreCase(rol)) {
            Optional<Profesor> profesorOpt = profesorRepository.findByCorreoIgnoreCase(emailLimpio);
            if (profesorOpt.isEmpty()) {
                // Mitigación de timing attack: igualar tiempo de cómputo de BCrypt
                passwordEncoder.matches(password != null ? password : "", DUMMY_BCRYPT_HASH);
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            Profesor prof = profesorOpt.get();
            if (!verificarPasswordDocente(password, prof.getContrasena())) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            return new UsuarioAutenticado(prof.getId(), prof.getNombre(), prof.getCorreo(), "DOCENTE");

        } else {
            // Por defecto: ESTUDIANTE
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoIgnoreCase(emailLimpio);
            if (estudianteOpt.isEmpty()) {
                // Mitigación de timing attack: igualar tiempo de cómputo de BCrypt
                passwordEncoder.matches(password != null ? password : "", DUMMY_BCRYPT_HASH);
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            Estudiante est = estudianteOpt.get();
            if (!verificarPasswordEstudiante(password, est.getContrasena())) {
                throw new IllegalArgumentException("Credenciales inválidas. Verifica tu correo y contraseña.");
            }
            return new UsuarioAutenticado(est.getId(), est.getNombre(), est.getCorreo(), "ESTUDIANTE");
        }
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
     * Verificación estricta para estudiantes: su contraseña se hashea con BCrypt al registrarse,
     * por lo que solo se acepta un hash BCrypt válido (nunca texto plano).
     */
    public boolean verificarPasswordEstudiante(String passwordIngresada, String passwordAlmacenada) {
        if (passwordIngresada == null || !esHashBcrypt(passwordAlmacenada)) {
            return false;
        }
        return coincideBcrypt(passwordIngresada, passwordAlmacenada);
    }

    /**
     * Verificación para docentes: el docente de prueba se crea manualmente en Supabase, por lo que
     * se admite contraseña en texto plano (sin migrarla). Si ya es un hash BCrypt, se verifica como tal.
     */
    public boolean verificarPasswordDocente(String passwordIngresada, String passwordAlmacenada) {
        if (passwordIngresada == null || passwordAlmacenada == null) {
            return false;
        }
        if (esHashBcrypt(passwordAlmacenada)) {
            return coincideBcrypt(passwordIngresada, passwordAlmacenada);
        }
        return java.security.MessageDigest.isEqual(
                passwordIngresada.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                passwordAlmacenada.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean coincideBcrypt(String passwordIngresada, String hash) {
        try {
            return passwordEncoder.matches(passwordIngresada, hash);
        } catch (Exception e) {
            log.warn("Error al verificar contraseña con BCrypt: {}", e.getMessage());
            return false;
        }
    }
}
