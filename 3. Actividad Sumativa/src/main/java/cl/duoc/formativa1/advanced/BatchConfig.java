package cl.duoc.formativa1.advanced;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchConfig {

    @Bean(name = "batchTaskExecutor")
    public ThreadPoolTaskExecutor batchTaskExecutor(
            @Value("${app.partition.threads:4}") int threads) {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);

        executor.setQueueCapacity(10);

        executor.setThreadNamePrefix(
                "Batch-Thread-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);

        executor.initialize();

        return executor;
    }
}