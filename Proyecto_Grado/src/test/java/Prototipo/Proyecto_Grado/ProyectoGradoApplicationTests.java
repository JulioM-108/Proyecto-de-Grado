package Prototipo.Proyecto_Grado;

import Prototipo.Proyecto_Grado.repository.EstudianteRepository;
import Prototipo.Proyecto_Grado.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ProyectoGradoApplicationTests {

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        // Verificar que las consultas SQL con las columnas reales de Supabase ejecutan sin error
        assertDoesNotThrow(() -> {
            profesorRepository.findByCorreoIgnoreCase("correo_prueba@javerianacali.edu.co");
            estudianteRepository.findByCorreoIgnoreCase("correo_prueba@javerianacali.edu.co");
        });
    }

}
