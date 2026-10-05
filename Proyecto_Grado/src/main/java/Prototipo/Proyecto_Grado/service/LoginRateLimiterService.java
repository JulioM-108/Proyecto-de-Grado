package Prototipo.Proyecto_Grado.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio en memoria para mitigar ataques de fuerza bruta y diccionarios en el inicio de sesión.
 * Bloquea temporalmente solicitudes tras 5 intentos fallidos durante una ventana de 10 minutos.
 */
@Service
public class LoginRateLimiterService {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_BLOQUEO_SEGUNDOS = 10 * 60; // 10 minutos

    private record IntentoRegistro(int conteo, Instant primerIntento, Instant bloqueadoHasta) {}

    private final Map<String, IntentoRegistro> intentosPorClave = new ConcurrentHashMap<>();

    /**
     * Verifica si la clave dada (correo o IP) se encuentra bloqueada por exceso de intentos.
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

        // Si pasaron más de 10 minutos desde el primer intento fallido, reiniciar
        if (ahora.isAfter(reg.primerIntento().plusSeconds(VENTANA_BLOQUEO_SEGUNDOS))) {
            intentosPorClave.remove(key);
            return false;
        }

        return false;
    }

    /**
     * Registra un intento fallido. Si alcanza el máximo, aplica el bloqueo.
     */
    public void registrarIntentoFallido(String clave) {
        if (clave == null || clave.isBlank()) return;
        String key = clave.trim().toLowerCase();
        Instant ahora = Instant.now();

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
     * Limpia los intentos tras un login exitoso.
     */
    public void limpiarIntentos(String clave) {
        if (clave != null) {
            intentosPorClave.remove(clave.trim().toLowerCase());
        }
    }
}
