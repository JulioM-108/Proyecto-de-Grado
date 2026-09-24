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
                throw new IllegalArgumentException("No se encontró ningún docente registrado con ese correo institucional.");
            }
            Profesor prof = profesorOpt.get();
            verificarPassword(password, prof.getContrasena());
            return new UsuarioAutenticado(prof.getId(), prof.getNombre(), prof.getCorreo(), "DOCENTE");

        } else {
            // Por defecto: ESTUDIANTE
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoIgnoreCase(emailLimpio);
            if (estudianteOpt.isEmpty()) {
                throw new IllegalArgumentException("No se encontró ningún estudiante registrado con ese correo institucional.");
            }
            Estudiante est = estudianteOpt.get();
            verificarPassword(password, est.getContrasena());
            return new UsuarioAutenticado(est.getId(), est.getNombre(), est.getCorreo(), "ESTUDIANTE");
        }
    }

    /**
     * Exige que el correo pertenezca al dominio oficial de la Javeriana.
     */
    public void validarDominioJaveriano(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo institucional es obligatorio.");
        }
        String email = correo.trim().toLowerCase();
        if (!email.endsWith("@javerianacali.edu.co") && !email.endsWith("@javeriana.edu.co")) {
            throw new IllegalArgumentException(
                    "Solo se aceptan correos institucionales de la Pontificia Universidad Javeriana (@javerianacali.edu.co).");
        }
    }

    /**
     * Verifica la contraseña contra el valor almacenado.
     * Soporta BCrypt (producción) y texto plano (desarrollo inicial).
     */
    private void verificarPassword(String passwordIngresada, String passwordAlmacenada) {
        if (passwordAlmacenada == null || passwordIngresada == null) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }

        boolean coincide;
        try {
            // Intento BCrypt primero
            coincide = passwordEncoder.matches(passwordIngresada, passwordAlmacenada);
        } catch (Exception e) {
            coincide = false;
        }

        // Fallback: comparación en texto plano para datos de prueba sin hashear
        if (!coincide) {
            coincide = passwordIngresada.equals(passwordAlmacenada);
        }

        if (!coincide) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }
    }
}
