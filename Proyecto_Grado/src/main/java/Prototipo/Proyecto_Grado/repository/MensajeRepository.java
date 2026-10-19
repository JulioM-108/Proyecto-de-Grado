package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Mensaje;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Mensajes de múltiples participaciones en lote, ordenados cronológicamente.
     */
    List<Mensaje> findByParticipacionInOrderByCreatedAtAsc(List<Participacion> participaciones);

    /** Cuenta total de mensajes en una participación. */
    long countByParticipacion(Participacion participacion);

    /** Fecha del último mensaje del ESTUDIANTE por participación: filas [participacionId (UUID), fecha (OffsetDateTime)]. */
    @Query("SELECT m.participacion.id, MAX(m.createdAt) FROM Mensaje m "
            + "WHERE m.participacion IN :participaciones AND m.emisor = true GROUP BY m.participacion.id")
    List<Object[]> findUltimoMensajeEstudiantePorParticipacion(@Param("participaciones") List<Participacion> participaciones);

    /** Hora de cada mensaje (estudiante y tutor) por participación, para el tiempo activo (M-04): filas [participacionId (UUID), fecha (OffsetDateTime)]. */
    @Query("SELECT m.participacion.id, m.createdAt FROM Mensaje m WHERE m.participacion IN :participaciones")
    List<Object[]> findHorasMensajesPorParticipacion(@Param("participaciones") List<Participacion> participaciones);

    /** Cuenta los mensajes de un emisor (true = estudiante) en una participación. */
    long countByParticipacionAndEmisor(Participacion participacion, Boolean emisor);

    /** Eliminación en lote de mensajes por lista de participaciones. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Mensaje m WHERE m.participacion IN :participaciones")
    void deleteByParticipacionIn(@Param("participaciones") List<Participacion> participaciones);
}
