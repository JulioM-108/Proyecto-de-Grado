package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración del esquema de Supabase y de los repositorios JPA: lista las tablas y columnas del
 * esquema public y ejecuta las consultas de cada repositorio contra los datos reales.
 * Levanta el contexto completo de Spring ({@code @SpringBootTest}) y requiere conexión a Supabase.
 * Funciona como diagnóstico: imprime los resultados por consola (incluidos nombres y correos de los
 * registros) y solo falla si una consulta lanza una excepción.
 */
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

    @Autowired
    private ProfesorEstudianteRepository profesorEstudianteRepository;

    /** Imprime el esquema real (tablas y columnas) para compararlo con las entidades JPA. */
    @Test
    void verificarTablasYColumnasEnSupabase() {
        // Asegurar que la columna evaluacion_sus_tam exista en Supabase (DDL idempotente sobre la BD real)
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

    /**
     * Ejecuta cada consulta derivada de los repositorios con un registro existente, para detectar columnas o
     * consultas desalineadas con el esquema. Es transaccional para poder recorrer las relaciones lazy; al
     * terminar, la transacción de la prueba se revierte.
     */
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
            for (Participacion part : participaciones) {
                String estCorreo = part.getEstudiante() != null ? part.getEstudiante().getCorreo() : "null";
                String sesTema = part.getSesion() != null ? part.getSesion().getTema() : "null";
                System.out.println("   -> ID: " + part.getId() + " | Estudiante: " + estCorreo + " | Sesion: " + sesTema);
            }
            if (!participaciones.isEmpty()) {
                Participacion part = participaciones.get(0);
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

        // ProfesorEstudiante
        assertDoesNotThrow(() -> {
            List<ProfesorEstudiante> vinculos = profesorEstudianteRepository.findAll();
            System.out.println("ProfesorEstudiante: " + vinculos.size() + " registros encontrados.");
            for (ProfesorEstudiante pe : vinculos) {
                System.out.println("   -> Profesor: " + pe.getProfesor().getCorreo() + " <-> Estudiante: " + pe.getEstudiante().getCorreo() + " (" + pe.getEstudiante().getNombre() + ")");
            }
        });
    }
}
