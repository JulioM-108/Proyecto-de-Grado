package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Profesor;
import Prototipo.Proyecto_Grado.model.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "Sesion" de Supabase.
 * Se usa para listar las clases del docente, resolver la clase desde el enlace del chat y guardar la síntesis grupal.
 */
@Repository
public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    /** Todas las sesiones creadas por un profesor, de la más reciente a la más antigua. */
    List<Sesion> findByProfesorOrderByCreatedAtDesc(Profesor profesor);

    /** Busca una sesión por su enlace único (usado para que estudiantes se unan). */
    Optional<Sesion> findByEnlaceUnico(String enlaceUnico);

    /** Sesiones activas de un profesor (estado = true). */
    List<Sesion> findByProfesorAndEstadoTrue(Profesor profesor);

    /**
     * RF-07: guarda la síntesis grupal sin tocar el resto de columnas de la sesión
     * (evita sobrescribir, p. ej., un CRAFT editado mientras se generaba la síntesis).
     *
     * @return número de filas actualizadas (0 si la sesión ya no existe)
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Sesion s SET s.sintesisMd = :sintesis, s.sintesisFecha = :fecha WHERE s.id = :id")
    int guardarSintesis(@Param("id") UUID id, @Param("sintesis") String sintesis, @Param("fecha") OffsetDateTime fecha);
}
