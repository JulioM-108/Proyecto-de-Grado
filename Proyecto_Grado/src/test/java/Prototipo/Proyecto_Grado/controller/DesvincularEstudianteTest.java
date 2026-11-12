package Prototipo.Proyecto_Grado.controller;

import Prototipo.Proyecto_Grado.model.entity.*;
import Prototipo.Proyecto_Grado.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración de DocenteController: desvincular a un estudiante de una sesión (sale de la clase,
 * pero sigue en la cartera del docente) y eliminarlo de la plataforma (borra su cuenta y participaciones).
 * Levanta el contexto completo de Spring ({@code @SpringBootTest}) y escribe en la base de datos real de
 * Supabase: necesita al menos un profesor con una sesión creada. Usa estudiantes temporales
 * (correos test.desvincular.* y test.eliminar.*) que se borran al final; si una aserción falla antes de la
 * limpieza, el estudiante temporal queda en la BD y hay que borrarlo a mano.
 */
@SpringBootTest
class DesvincularEstudianteTest {

    static {
        Prototipo.Proyecto_Grado.ProyectoGradoApplication.cargarVariablesEntorno();
    }

    @Autowired
    private DocenteController docenteController;

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

    @Test
    @DisplayName("Desvincular a un estudiante de una sesión elimina su participación y ya no aparece en sus clases")
    void testDesvincularEstudianteDeSesion() {
        // 1. Obtener docente y sesión existentes
        List<Profesor> profesores = profesorRepository.findAll();
        assertFalse(profesores.isEmpty(), "Debe existir al menos un profesor");
        Profesor profesor = profesores.get(0);

        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);
        assertFalse(sesiones.isEmpty(), "Debe existir al menos una sesión para el profesor");
        Sesion sesion = sesiones.get(0);

        // 2. Crear estudiante temporal de prueba para no alterar datos de producción
        Estudiante testEstudiante = estudianteRepository.save(
                Estudiante.builder()
                        .nombre("Estudiante Prueba Desvinculación")
                        .correo("test.desvincular." + System.currentTimeMillis() + "@javerianacali.edu.co")
                        .contrasena("Prueba123*")
                        .build()
        );

        // 3. Crear participación de prueba y vínculo permanente con el docente
        profesorEstudianteRepository.save(
                ProfesorEstudiante.builder()
                        .profesor(profesor)
                        .estudiante(testEstudiante)
                        .build()
        );

        Participacion testPart = participacionRepository.save(
                Participacion.builder()
                        .estudiante(testEstudiante)
                        .sesion(sesion)
                        .build()
        );

        // 4. Agregar mensaje y métrica de prueba
        mensajeRepository.save(
                Mensaje.builder()
                        .participacion(testPart)
                        .emisor(true)
                        .contenido("Hola profesor")
                        .build()
        );
        metricaRepository.save(
                Metrica.builder()
                        .participacion(testPart)
                        .codigo(2L)
                        .concepto("Prueba")
                        .valorNumerico(1L)
                        .build()
        );

        // Verificar que el estudiante ve la clase antes de desvincularlo
        List<Participacion> clasesAntes = participacionRepository.findByEstudianteWithSesionOrderByCreatedAtDesc(testEstudiante);
        assertTrue(clasesAntes.stream().anyMatch(p -> p.getSesion().getId().equals(sesion.getId())),
                "El estudiante debe ver la clase antes de ser desvinculado");

        // 5. Ejecutar la acción del controlador como docente autenticado
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute("usuarioLogueado", profesor.getCorreo());
        httpSession.setAttribute("rolUsuario", "DOCENTE");
        httpSession.setAttribute("profesorActivo", profesor);

        String redirect = docenteController.desvincularEstudianteDeSesion(sesion.getId(), testPart.getId(), httpSession);
        assertTrue(redirect.contains("estudianteDesvinculado=true"), "Debe redirigir con éxito");

        // 6. Verificar que el estudiante YA NO ve la clase al iniciar sesión / consultar sus clases
        List<Participacion> clasesDespues = participacionRepository.findByEstudianteWithSesionOrderByCreatedAtDesc(testEstudiante);
        assertFalse(clasesDespues.stream().anyMatch(p -> p.getSesion().getId().equals(sesion.getId())),
                "El estudiante YA NO debe ver la clase después de ser desvinculado");

        // 7. Verificar que el estudiante PERMANECE en la cartera del docente aunque tenga 0 clases
        assertTrue(profesorEstudianteRepository.existsByProfesorAndEstudiante(profesor, testEstudiante),
                "El estudiante debe permanecer en la cartera del docente a pesar de estar desvinculado de la sesión");

        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        docenteController.listarEstudiantes(null, httpSession, model);
        @SuppressWarnings("unchecked")
        List<DocenteController.EstudianteDirectorioDto> directorio = (List<DocenteController.EstudianteDirectorioDto>) model.get("directorio");
        assertNotNull(directorio);
        assertTrue(directorio.stream().anyMatch(d -> d.id().equals(testEstudiante.getId()) && d.totalClases() == 0),
                "El estudiante debe aparecer en el directorio del docente con 0 clases");

        // Limpieza del estudiante de prueba
        profesorEstudianteRepository.deleteByProfesorAndEstudiante(profesor, testEstudiante);
        estudianteRepository.delete(testEstudiante);
    }

    @Test
    @DisplayName("Eliminar a un estudiante de la plataforma elimina todas sus participaciones y su cuenta")
    void testEliminarEstudianteDePlataforma() {
        List<Profesor> profesores = profesorRepository.findAll();
        Profesor profesor = profesores.get(0);

        List<Sesion> sesiones = sesionRepository.findByProfesorOrderByCreatedAtDesc(profesor);
        Sesion sesion = sesiones.get(0);

        // Mismo escenario que la prueba anterior; aquí la limpieza la hace el propio eliminarEstudiante
        Estudiante testEstudiante = estudianteRepository.save(
                Estudiante.builder()
                        .nombre("Estudiante Para Eliminar")
                        .correo("test.eliminar." + System.currentTimeMillis() + "@javerianacali.edu.co")
                        .contrasena("Prueba123*")
                        .build()
        );

        profesorEstudianteRepository.save(
                ProfesorEstudiante.builder()
                        .profesor(profesor)
                        .estudiante(testEstudiante)
                        .build()
        );

        Participacion testPart = participacionRepository.save(
                Participacion.builder()
                        .estudiante(testEstudiante)
                        .sesion(sesion)
                        .build()
        );

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute("usuarioLogueado", profesor.getCorreo());
        httpSession.setAttribute("rolUsuario", "DOCENTE");
        httpSession.setAttribute("profesorActivo", profesor);

        String redirect = docenteController.eliminarEstudiante(testEstudiante.getId(), httpSession);
        assertTrue(redirect.contains("estudianteEliminado=true"), "Debe redirigir con éxito");

        // Verificar que ya no existe ni el estudiante, ni su participación, ni el vínculo con el docente
        assertFalse(estudianteRepository.findById(testEstudiante.getId()).isPresent(),
                "El estudiante no debe existir en la BD");
        assertFalse(participacionRepository.findById(testPart.getId()).isPresent(),
                "La participación no debe existir en la BD");
        assertFalse(profesorEstudianteRepository.existsByProfesorAndEstudiante(profesor, testEstudiante),
                "El vínculo en profesor_estudiante no debe existir en la BD");
    }
}
