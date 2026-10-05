package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.ProfesorEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "profesor_estudiante".
 * Permite gestionar la cartera histórica de alumnos vinculados a un profesor.
 */
@Repository
public interface ProfesorEstudianteRepository extends JpaRepository<ProfesorEstudiante, UUID> {

    /** Retorna todos los vínculos de un docente cargando el Estudiante eagerly para evitar LazyInitializationException. */
    @Query("SELECT pe FROM ProfesorEstudiante pe JOIN FETCH pe.estudiante WHERE pe.profesor = :profesor ORDER BY pe.estudiante.nombre ASC")
    List<ProfesorEstudiante> findByProfesorWithEstudiante(@Param("profesor") Profesor profesor);

    /** Comprueba si ya existe el vínculo persistente entre el docente y el estudiante. */
    boolean existsByProfesorAndEstudiante(Profesor profesor, Estudiante estudiante);

    /** Busca el vínculo específico entre un docente y un estudiante. */
    Optional<ProfesorEstudiante> findByProfesorAndEstudiante(Profesor profesor, Estudiante estudiante);

    /** Elimina el vínculo entre un docente y un estudiante. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ProfesorEstudiante pe WHERE pe.profesor = :profesor AND pe.estudiante = :estudiante")
    void deleteByProfesorAndEstudiante(@Param("profesor") Profesor profesor, @Param("estudiante") Estudiante estudiante);

    /** Retorna todos los vínculos de un estudiante con diferentes docentes (para verificar antes de eliminar de plataforma). */
    List<ProfesorEstudiante> findByEstudiante(Estudiante estudiante);

    /** Cantidad total de alumnos en la cartera del docente. */
    long countByProfesor(Profesor profesor);
}
