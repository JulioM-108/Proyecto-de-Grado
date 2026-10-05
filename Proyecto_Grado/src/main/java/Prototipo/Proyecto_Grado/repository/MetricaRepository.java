package Prototipo.Proyecto_Grado.repository;

import Prototipo.Proyecto_Grado.model.entity.Metrica;
import Prototipo.Proyecto_Grado.model.entity.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Métricas de múltiples participaciones en lote (batching). */
    List<Metrica> findByParticipacionIn(List<Participacion> participaciones);

    /** Métricas de un concepto POO específico en todas las participaciones (para analítica docente). */
    List<Metrica> findByConcepto(String concepto);

    /** Métricas filtradas por código (ej: 10L para RF-10). */
    List<Metrica> findByCodigo(Long codigo);

    /** Conteo directo en BD por código. */
    long countByCodigo(Long codigo);

    /** Indica si una participación ya tiene una métrica con el código dado (ej: 11L = cierre por el estudiante). */
    boolean existsByParticipacionAndCodigo(Participacion participacion, Long codigo);

    /** Agregación en base de datos de conceptos con sumatoria de valores numéricos (excluye telemetría interna como sesion_cerrada, turnos_totales y cierre_estudiante). */
    @Query("SELECT m.concepto, SUM(m.valorNumerico) FROM Metrica m WHERE m.concepto IS NOT NULL AND UPPER(m.concepto) <> 'RF-10' AND LOWER(m.concepto) NOT IN ('sesion_cerrada', 'turnos_totales', 'cierre_estudiante') AND (m.codigo IS NULL OR m.codigo NOT IN (3, 5, 10, 11)) AND m.valorNumerico IS NOT NULL GROUP BY m.concepto")
    List<Object[]> sumValoresPorConcepto();

    /** Eliminación en lote de métricas por lista de participaciones. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Metrica m WHERE m.participacion IN :participaciones")
    void deleteByParticipacionIn(@Param("participaciones") List<Participacion> participaciones);
}
