package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CORREO = "usuario@javerianacali.edu.co";
    private static final String PASSWORD = "Clave123";

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProfesorRepository profesorRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(estudianteRepository, profesorRepository, encoder);
    }

    @Test
    @DisplayName("Docente con contraseña en texto plano ingresa y su contraseña NO se migra")
    void docenteTextoPlanoIngresaSinMigrar() {
        Profesor prof = Profesor.builder().id(UUID.randomUUID()).nombre("Profe").correo(CORREO).contrasena(PASSWORD).build();
        when(profesorRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(prof));

        AuthService.UsuarioAutenticado usuario = authService.autenticar(CORREO, PASSWORD, "DOCENTE");

        assertEquals("DOCENTE", usuario.rol());
        assertEquals(PASSWORD, prof.getContrasena(), "La contraseña debe quedar legible en Supabase");
        verify(profesorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estudiante con contraseña en texto plano es rechazado")
    void estudianteTextoPlanoRechazado() {
        Estudiante est = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo(CORREO).contrasena(PASSWORD).build();
        when(estudianteRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(est));

        assertThrows(IllegalArgumentException.class, () -> authService.autenticar(CORREO, PASSWORD, "ESTUDIANTE"));
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estudiante con contraseña BCrypt ingresa")
    void estudianteBcryptIngresa() {
        Estudiante est = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo(CORREO)
                .contrasena(encoder.encode(PASSWORD)).build();
        when(estudianteRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(est));

        assertEquals("ESTUDIANTE", authService.autenticar(CORREO, PASSWORD, "ESTUDIANTE").rol());
    }
}
