package Prototipo.Proyecto_Grado.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de intentos de inicio de sesión contra ataques de fuerza bruta y de diccionario (RNF-01).
 * <p>
 * Lo usan el login único ({@code LoginController}) y el acceso del estudiante por enlace de clase
 * ({@code ChatController}), con la clave {@code correo_IP}. Tras 5 intentos fallidos dentro de una ventana
 * de 10 minutos, la clave queda bloqueada 10 minutos.
 * <p>
 * El estado vive en memoria: se pierde al reiniciar la aplicación y no se comparte entre instancias.
 */
@Service
public class LoginRateLimiterService {

    private static final int MAX_INTENTOS = 5;
    // Se usa tanto para la ventana de conteo de fallos como para la duración del bloqueo
    private static final long VENTANA_BLOQUEO_SEGUNDOS = 10 * 60; // 10 minutos

    /** Fallos acumulados de una clave; {@code bloqueadoHasta} es null mientras no se alcance el máximo. */
    private record IntentoRegistro(int conteo, Instant primerIntento, Instant bloqueadoHasta) {}

    private final Map<String, IntentoRegistro> intentosPorClave = new ConcurrentHashMap<>();

    /**
     * Indica si la clave ({@code correo_IP}) está bloqueada por exceso de intentos fallidos.
     * Como efecto secundario, elimina el registro si el bloqueo o la ventana de conteo ya expiraron.
     *
     * @param clave clave del intento; se normaliza (trim y minúsculas). Una clave vacía nunca se bloquea
     * @return true si el bloqueo sigue vigente
     */
    public boolean estaBloqueado(String clave) {
        if (clave == null || clave.isBlank()) return false;
        String key = clave.trim().toLowerCase();

        IntentoRegistro reg = intentosPorClave.get(key);
        if (reg == null) return false;

        Instant ahora = Instant.now();
        if (reg.bloqueadoHasta() != null) {
            if (ahora.isBefore(reg.bloqueadoHasta())) {
                return true;
            } else {
                // El bloqueo expiró
                intentosPorClave.remove(key);
                return false;
            }
        }

        // Sin bloqueo: si la ventana de conteo ya venció, se descartan los fallos (solo libera memoria)
        if (ahora.isAfter(reg.primerIntento().plusSeconds(VENTANA_BLOQUEO_SEGUNDOS))) {
            intentosPorClave.remove(key);
            return false;
        }

        return false;
    }

    /**
     * Registra un intento fallido para la clave. Si la ventana de 10 minutos venció, el conteo se reinicia;
     * al llegar a {@value #MAX_INTENTOS} fallos se bloquea la clave durante 10 minutos.
     */
    public void registrarIntentoFallido(String clave) {
        if (clave == null || clave.isBlank()) return;
        String key = clave.trim().toLowerCase();
        Instant ahora = Instant.now();

        // compute() hace la lectura y la actualización de forma atómica ante peticiones concurrentes
        intentosPorClave.compute(key, (k, reg) -> {
            if (reg == null || ahora.isAfter(reg.primerIntento().plusSeconds(VENTANA_BLOQUEO_SEGUNDOS))) {
                return new IntentoRegistro(1, ahora, null);
            }
            int nuevoConteo = reg.conteo() + 1;
            Instant bloqueo = (nuevoConteo >= MAX_INTENTOS) ? ahora.plusSeconds(VENTANA_BLOQUEO_SEGUNDOS) : null;
            return new IntentoRegistro(nuevoConteo, reg.primerIntento(), bloqueo);
        });
    }

    /**
     * Borra los fallos acumulados de la clave tras un inicio de sesión exitoso.
     */
    public void limpiarIntentos(String clave) {
        if (clave != null) {
            intentosPorClave.remove(clave.trim().toLowerCase());
        }
    }
}
