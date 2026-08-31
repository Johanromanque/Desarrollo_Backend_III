package cl.duoc.formativa1.advanced;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionListener
        implements JobExecutionListener {

    private static final Logger logger =
            LoggerFactory.getLogger(JobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {

        logger.info(
                "========== INICIO DEL JOB =========="
        );

        logger.info(
                "Job: {}",
                jobExecution.getJobInstance().getJobName()
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {

        long duracionMs = 0;

        LocalDateTime inicio = jobExecution.getStartTime();
        LocalDateTime fin = jobExecution.getEndTime();

        if (inicio != null && fin != null) {
            duracionMs = Duration.between(
                    inicio,
                    fin
            ).toMillis();
        }

        logger.info(
                "========== FINALIZACIÓN DEL JOB =========="
        );

        logger.info(
                "Job: {}",
                jobExecution.getJobInstance().getJobName()
        );

        logger.info(
                "Estado final: {}",
                jobExecution.getStatus()
        );

        logger.info(
                "Inicio: {}",
                inicio
        );

        logger.info(
                "Fin: {}",
                fin
        );

        logger.info(
                "Steps ejecutados: {}",
                jobExecution.getStepExecutions().size()
        );

        logger.info(
                "Tiempo total: {} ms",
                duracionMs
        );

        if (!jobExecution.getFailureExceptions().isEmpty()) {

            logger.error(
                    "Errores registrados: {}",
                    jobExecution.getFailureExceptions().size()
            );
        }

        logger.info(
                "==========================================="
        );
    }
}