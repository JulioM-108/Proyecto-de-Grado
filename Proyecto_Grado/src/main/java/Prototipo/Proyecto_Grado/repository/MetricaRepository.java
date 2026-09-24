package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla "metrica" de Supabase.
 */
@Repository
public interface MetricaRepository extends JpaRepository<Metrica, UUID> {

    /** Todas las métricas de una participación. */
    List<Metrica> findByParticipacion(Participacion participacion);

    /** Métricas de un concepto POO específico en todas las participaciones (para analítica docente). */
    List<Metrica> findByConcepto(String concepto);

    /** Métricas filtradas por código (ej: 10L para RF-10). */
    List<Metrica> findByCodigo(Long codigo);
}
