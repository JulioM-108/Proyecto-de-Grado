package Prototipo.Proyecto_Grado.config;

import Prototipo.Proyecto_Grado.ProyectoGradoApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Garantiza que las variables de entorno del archivo .env sean cargadas
 * en el entorno del sistema antes de que Spring Boot inicialice el DataSource
 * o cualquier Bean tanto en producción como en la ejecución de tests JUnit.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvLoaderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ProyectoGradoApplication.cargarVariablesEntorno();
    }
}
