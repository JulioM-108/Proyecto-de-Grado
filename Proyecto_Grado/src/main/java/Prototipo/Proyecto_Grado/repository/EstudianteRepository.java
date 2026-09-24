package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "Estudiante" de Supabase.
 */
@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    /** Busca un estudiante por su correo institucional (case-insensitive). */
    Optional<Estudiante> findByCorreoIgnoreCase(String correo);
}
