package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "participacion" de Supabase.
 * Una Participacion vincula un Estudiante con una Sesion y acumula su progreso_md.
 */
@Repository
public interface ParticipacionRepository extends JpaRepository<Participacion, UUID> {

    /** Participaciones de un estudiante ordenadas de más reciente a más antigua. */
    List<Participacion> findByEstudianteOrderByCreatedAtDesc(Estudiante estudiante);

    /** La participación activa más reciente de un estudiante en una sesión específica. */
    Optional<Participacion> findFirstByEstudianteAndSesionOrderByCreatedAtDesc(Estudiante estudiante, Sesion sesion);

    /** Todas las participaciones de una sesión (vista docente) con Estudiante cargado para evitar N+1. */
    @EntityGraph(attributePaths = {"estudiante"})
    List<Participacion> findBySesion(Sesion sesion);

    /** Cantidad de participaciones en una sesión (para métricas del dashboard). */
    long countBySesion(Sesion sesion);

    /** Estudiantes únicos que han participado en alguna sesión del docente. */
    @Query("SELECT DISTINCT p.estudiante FROM Participacion p WHERE p.sesion.profesor = :profesor ORDER BY p.estudiante.nombre ASC")
    List<Estudiante> findDistinctEstudiantesByProfesor(@Param("profesor") Profesor profesor);

    /** Todas las participaciones de un docente con Estudiante y Sesion cargados eagerly para evitar LazyInitializationException. */
    @Query("SELECT p FROM Participacion p JOIN FETCH p.estudiante JOIN FETCH p.sesion WHERE p.sesion.profesor = :profesor ORDER BY p.createdAt DESC")
    List<Participacion> findByProfesorWithEstudianteAndSesion(@Param("profesor") Profesor profesor);

    /** Participaciones de un estudiante con su Sesion cargada eagerly. */
    @Query("SELECT p FROM Participacion p JOIN FETCH p.sesion WHERE p.estudiante = :estudiante ORDER BY p.createdAt DESC")
    List<Participacion> findByEstudianteWithSesionOrderByCreatedAtDesc(@Param("estudiante") Estudiante estudiante);

    /** Comprueba si un estudiante ya está vinculado a una sesión específica. */
    boolean existsByEstudianteAndSesion(Estudiante estudiante, Sesion sesion);

    /** Busca una participación cargando eager su Sesion y el Profesor para evitar LazyInitializationException */
    @Query("SELECT p FROM Participacion p JOIN FETCH p.sesion s JOIN FETCH s.profesor WHERE p.id = :id")
    Optional<Participacion> findByIdWithSesionAndProfesor(@Param("id") UUID id);

    /** Busca una participación cargando eager su Estudiante, Sesion y Profesor para evitar LazyInitializationException */
    @Query("SELECT p FROM Participacion p JOIN FETCH p.estudiante JOIN FETCH p.sesion s JOIN FETCH s.profesor WHERE p.id = :id")
    Optional<Participacion> findByIdWithEstudianteAndSesionAndProfesor(@Param("id") UUID id);
}
