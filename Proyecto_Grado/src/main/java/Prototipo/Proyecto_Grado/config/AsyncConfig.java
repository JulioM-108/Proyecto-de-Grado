package Prototipo.Proyecto_Grado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor en segundo plano para los informes individuales (progreso_md) que se generan
 * automáticamente al completar la ruta o con "Generar informes pendientes".
 * Dos hilos acotan las llamadas simultáneas a Gemini y el uso del pool de conexiones (Hikari).
 */
@Configuration
public class AsyncConfig {

    @Bean
    public TaskExecutor informesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("informe-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
