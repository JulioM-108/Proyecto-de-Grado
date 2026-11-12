package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "Profesor" de Supabase.
 * Se usa en el login (incluido el 2FA) y para resolver al docente de la sesión HTTP por su correo.
 */
@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, UUID> {

    /** Busca un profesor por su correo institucional (case-insensitive). */
    Optional<Profesor> findByCorreoIgnoreCase(String correo);
}
