package cl.duoc.formativa1.advanced;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class InteresStepExecutionListener
        implements StepExecutionListener {

    private static final Logger logger =
            LoggerFactory.getLogger(InteresStepExecutionListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {

        logger.info(
                "========== INICIO DEL STEP INTERESES =========="
        );

        logger.info(
                "Step: {}",
                stepExecution.getStepName()
        );
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        long duracionMs = 0;

        LocalDateTime inicio = stepExecution.getStartTime();
        LocalDateTime fin = stepExecution.getEndTime();

        if (inicio != null && fin != null) {
            duracionMs = Duration.between(
                    inicio,
                    fin
            ).toMillis();
        }

        long totalOmitidos =
                stepExecution.getReadSkipCount()
                + stepExecution.getProcessSkipCount()
                + stepExecution.getWriteSkipCount();

        logger.info(
                "========== RESUMEN DEL STEP INTERESES =========="
        );

        logger.info(
                "Estado final: {}",
                stepExecution.getStatus()
        );

        logger.info(
                "Registros leídos: {}",
                stepExecution.getReadCount()
        );

        logger.info(
                "Registros escritos: {}",
                stepExecution.getWriteCount()
        );

        logger.info(
                "Omitidos en lectura: {}",
                stepExecution.getReadSkipCount()
        );

        logger.info(
                "Omitidos en procesamiento: {}",
                stepExecution.getProcessSkipCount()
        );

        logger.info(
                "Omitidos en escritura: {}",
                stepExecution.getWriteSkipCount()
        );

        logger.info(
                "Total omitidos: {}",
                totalOmitidos
        );

        logger.info(
                "Commits realizados: {}",
                stepExecution.getCommitCount()
        );

        logger.info(
                "Rollbacks realizados: {}",
                stepExecution.getRollbackCount()
        );

        logger.info(
                "Tiempo de ejecución: {} ms",
                duracionMs
        );

        logger.info(
                "================================================"
        );

        return stepExecution.getExitStatus();
    }
}