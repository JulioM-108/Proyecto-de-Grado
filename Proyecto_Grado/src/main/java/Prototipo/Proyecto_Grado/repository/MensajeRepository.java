package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "mensaje" de Supabase.
 */
@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    /**
     * Todos los mensajes de una participación ordenados cronológicamente.
     * Permite reconstruir el historial completo del chat en orden.
     */
    List<Mensaje> findByParticipacionOrderByCreatedAtAsc(Participacion participacion);

    /** Cuenta total de mensajes en una participación. */
    long countByParticipacion(Participacion participacion);
}
