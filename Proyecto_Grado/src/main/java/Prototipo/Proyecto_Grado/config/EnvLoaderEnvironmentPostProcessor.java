package Prototipo.Proyecto_Grado.config;

import Prototipo.Proyecto_Grado.ProyectoGradoApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Garantiza que las variables del archivo .env queden cargadas como propiedades del sistema
 * antes de que Spring Boot inicialice el DataSource o cualquier bean, tanto en producción
 * como en los tests JUnit (que no pasan por main).
 * Se registra en META-INF/spring.factories y delega en ProyectoGradoApplication.cargarVariablesEntorno().
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvLoaderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ProyectoGradoApplication.cargarVariablesEntorno();
    }
}
