package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DatabaseSchemaVerificationTest {

    static {
        Prototipo.Proyecto_Grado.ProyectoGradoApplication.cargarVariablesEntorno();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ParticipacionRepository participacionRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private MetricaRepository metricaRepository;

    @Test
    void verificarTablasYColumnasEnSupabase() {
        // Asegurar que la columna evaluacion_sus_tam exista en Supabase
        jdbcTemplate.execute("ALTER TABLE participacion ADD COLUMN IF NOT EXISTS evaluacion_sus_tam TEXT;");

        System.out.println("=== 1. TABLAS EXISTENTES EN SCHEMA PUBLIC ===");
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
        );
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("table_name");
            System.out.println("TABLA: " + tableName);

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position",
                    tableName
            );
            for (Map<String, Object> col : columns) {
                System.out.println("   - " + col.get("column_name") + " (" + col.get("data_type") + ", nullable=" + col.get("is_nullable") + ")");
            }
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void verificarConteoYRepositorios() {
        System.out.println("=== 2. CONTEO Y CONSULTAS REALES DE REPOSITORIOS ===");

        // Profesor
        assertDoesNotThrow(() -> {
            List<Profesor> profesores = profesorRepository.findAll();
            System.out.println("Profesor: " + profesores.size() + " registros encontrados.");
            if (!profesores.isEmpty()) {
                Profesor p = profesores.get(0);
                System.out.println("   Ejemplo: id=" + p.getId() + ", nombre=" + p.getNombre() + ", correo=" + p.getCorreo() + ", facultad=" + p.getFacultad());
                profesorRepository.findByCorreoIgnoreCase(p.getCorreo());
            }
        });

        // Sesion
        assertDoesNotThrow(() -> {
            List<Sesion> sesiones = sesionRepository.findAll();
            System.out.println("Sesion: " + sesiones.size() + " registros encontrados.");
            if (!sesiones.isEmpty()) {
                Sesion s = sesiones.get(0);
                System.out.println("   Ejemplo: id=" + s.getId() + ", tema=" + s.getTema() + ", enlace=" + s.getEnlaceUnico() + ", estado=" + s.getEstado());
                if (s.getProfesor() != null) {
                    sesionRepository.findByProfesorOrderByCreatedAtDesc(s.getProfesor());
                    sesionRepository.findByProfesorAndEstadoTrue(s.getProfesor());
                }
                sesionRepository.findByEnlaceUnico(s.getEnlaceUnico());
            }
        });

        // Estudiante
        assertDoesNotThrow(() -> {
            List<Estudiante> estudiantes = estudianteRepository.findAll();
            System.out.println("Estudiante: " + estudiantes.size() + " registros encontrados.");
            for (Estudiante e : estudiantes) {
                String pass = e.getContrasena();
                String tipo = (pass != null && pass.startsWith("$2a$")) ? "BCrypt (Hasheada)" : "Texto plano";
                System.out.println("   - Nombre: " + e.getNombre() + " | Correo: " + e.getCorreo() + " | Tipo Pass: " + tipo);
            }
        });

        // Participacion
        assertDoesNotThrow(() -> {
            List<Participacion> participaciones = participacionRepository.findAll();
            System.out.println("Participacion: " + participaciones.size() + " registros encontrados.");
            if (!participaciones.isEmpty()) {
                Participacion part = participaciones.get(0);
                System.out.println("   Ejemplo: id=" + part.getId() + ", feedback=" + part.getFeedbackEstudiante() + ", valoracion=" + part.getValoracionEstudiante());
                participacionRepository.findByEstudianteOrderByCreatedAtDesc(part.getEstudiante());
                participacionRepository.findFirstByEstudianteAndSesionOrderByCreatedAtDesc(part.getEstudiante(), part.getSesion());
                participacionRepository.findBySesion(part.getSesion());
                participacionRepository.countBySesion(part.getSesion());
                participacionRepository.existsByEstudianteAndSesion(part.getEstudiante(), part.getSesion());
                if (part.getSesion() != null && part.getSesion().getProfesor() != null) {
                    participacionRepository.findDistinctEstudiantesByProfesor(part.getSesion().getProfesor());
                    participacionRepository.findByProfesorWithEstudianteAndSesion(part.getSesion().getProfesor());
                }
                participacionRepository.findByEstudianteWithSesionOrderByCreatedAtDesc(part.getEstudiante());
            }
        });

        // Mensaje
        assertDoesNotThrow(() -> {
            List<Mensaje> mensajes = mensajeRepository.findAll();
            System.out.println("Mensaje: " + mensajes.size() + " registros encontrados.");
            if (!mensajes.isEmpty()) {
                Mensaje m = mensajes.get(0);
                System.out.println("   Ejemplo: id=" + m.getId() + ", emisor=" + m.getEmisor() + ", contenido=" + (m.getContenido() != null ? m.getContenido().substring(0, Math.min(30, m.getContenido().length())) : "null"));
                mensajeRepository.findByParticipacionOrderByCreatedAtAsc(m.getParticipacion());
                mensajeRepository.countByParticipacion(m.getParticipacion());
            }
        });

        // Metrica
        assertDoesNotThrow(() -> {
            List<Metrica> metricas = metricaRepository.findAll();
            System.out.println("Metrica: " + metricas.size() + " registros encontrados.");
            if (!metricas.isEmpty()) {
                Metrica met = metricas.get(0);
                System.out.println("   Ejemplo: id=" + met.getId() + ", codigo=" + met.getCodigo() + ", concepto=" + met.getConcepto() + ", valor=" + met.getValorNumerico());
                metricaRepository.findByParticipacion(met.getParticipacion());
                if (met.getConcepto() != null) {
                    metricaRepository.findByConcepto(met.getConcepto());
                }
                if (met.getCodigo() != null) {
                    metricaRepository.findByCodigo(met.getCodigo());
                }
            }
        });
    }
}
