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
            if (!verificarPassword(password, prof.getContrasena())) {
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
            if (!verificarPassword(password, est.getContrasena())) {
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
     * Verifica la contraseña contra el hash BCrypt almacenado.
     */
    public boolean verificarPassword(String passwordIngresada, String passwordAlmacenada) {
        if (passwordAlmacenada == null || passwordIngresada == null) {
            return false;
        }

        try {
            return passwordEncoder.matches(passwordIngresada, passwordAlmacenada);
        } catch (Exception e) {
            log.warn("Error al verificar contraseña con BCrypt: {}", e.getMessage());
            return false;
        }
    }
}
