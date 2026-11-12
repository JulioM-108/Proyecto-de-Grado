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

    /**
     * Pool fijo de 2 hilos ("informe-N") con cola de 500 tareas; si la cola se llena, el ejecutor rechaza
     * la tarea con una excepción. Al apagar la aplicación espera hasta 30 s a que terminen los informes en curso.
     */
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
