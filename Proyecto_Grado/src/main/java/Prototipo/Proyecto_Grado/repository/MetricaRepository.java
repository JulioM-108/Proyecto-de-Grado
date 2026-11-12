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
 * Repositorio JPA para la tabla metrica de Supabase.
 * Se usa para leer las métricas de una o varias participaciones (M-02 y M-03 en el dashboard y en el chat)
 * y para borrarlas en lote junto con sus participaciones. Los códigos se describen en el campo codigo de
 * {@link Metrica}.
 */
@Repository
public interface MetricaRepository extends JpaRepository<Metrica, UUID> {

    /** Todas las métricas de una participación (el chat las usa para saber qué preguntas guía ya se registraron). */
    List<Metrica> findByParticipacion(Participacion participacion);

    /** Métricas de múltiples participaciones en una sola consulta (evita N+1 en el dashboard). */
    List<Metrica> findByParticipacionIn(List<Participacion> participaciones);

    /** Métricas con un concepto dado (p. ej., "APERTURA" o "P3") en todas las participaciones. */
    List<Metrica> findByConcepto(String concepto);

    /** Métricas filtradas por código (ej: 10L para las solicitudes directas de M-03). */
    List<Metrica> findByCodigo(Long codigo);

    /** Conteo directo en BD por código. */
    long countByCodigo(Long codigo);

    /** Indica si una participación ya tiene alguna métrica con el código dado (p. ej., 20L = apertura registrada). */
    boolean existsByParticipacionAndCodigo(Participacion participacion, Long codigo);

    /**
     * Suma valor_numerico agrupado por concepto, en la base de datos. Devuelve filas [concepto (String), suma (Long)].
     * Excluye las solicitudes directas (código 10, conceptos "M-03" y "RF-10") y la telemetría de versiones
     * anteriores (códigos 3, 5 y 11; conceptos sesion_cerrada, turnos_totales y cierre_estudiante).
     * Los códigos actuales 2, 20 y 21 sí se incluyen.
     */
    @Query("SELECT m.concepto, SUM(m.valorNumerico) FROM Metrica m WHERE m.concepto IS NOT NULL AND UPPER(m.concepto) NOT IN ('RF-10', 'M-03') AND LOWER(m.concepto) NOT IN ('sesion_cerrada', 'turnos_totales', 'cierre_estudiante') AND (m.codigo IS NULL OR m.codigo NOT IN (3, 5, 10, 11)) AND m.valorNumerico IS NOT NULL GROUP BY m.concepto")
    List<Object[]> sumValoresPorConcepto();

    /**
     * Eliminación en lote de métricas por lista de participaciones, con un DELETE JPQL que no carga entidades.
     * Se ejecuta antes de borrar las participaciones (FK participacion_id) y requiere una transacción del llamador.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Metrica m WHERE m.participacion IN :participaciones")
    void deleteByParticipacionIn(@Param("participaciones") List<Participacion> participaciones);
}
