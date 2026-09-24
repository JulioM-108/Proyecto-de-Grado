package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "Sesion" de Supabase.
 */
@Repository
public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    /** Todas las sesiones creadas por un profesor dado ordenadas cronológicamente. */
    List<Sesion> findByProfesorOrderByCreatedAtDesc(Profesor profesor);

    /** Busca una sesión por su enlace único (usado para que estudiantes se unan). */
    Optional<Sesion> findByEnlaceUnico(String enlaceUnico);

    /** Sesiones activas de un profesor (estado = true). */
    List<Sesion> findByProfesorAndEstadoTrue(Profesor profesor);
}
