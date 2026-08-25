package cl.duoc.formativa1.advanced;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchConfig.class);

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor(
            BatchProperties properties,
            DataSource dataSource) {

        validarPoolJdbc(properties, dataSource);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("Batch-Thread-");
        // La aplicacion se ejecuta como proceso batch CLI. Al terminar el Job,
        // los workers inactivos no deben mantener viva la JVM.
        executor.setDaemon(true);
        executor.initialize();

        logger.info(
                "Configuracion batch efectiva: chunk={}, corePool={}, maxPool={}, queueCapacity={}",
                properties.getChunkSize(),
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity());

        return executor;
    }

    private void validarPoolJdbc(BatchProperties properties, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            int minimo = properties.getMaxPoolSize() + 1;
            if (hikariDataSource.getMaximumPoolSize() < minimo) {
                throw new IllegalStateException(
                        "El pool JDBC requiere al menos " + minimo
                                + " conexiones para los threads y el JobRepository");
            }
        }
    }
}
