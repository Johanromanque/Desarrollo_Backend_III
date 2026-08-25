package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import cl.duoc.formativa1.advanced.CustomDecider;
import cl.duoc.formativa1.advanced.CustomSkipPolicy;
import cl.duoc.formativa1.advanced.ProcessingThreadTracker;
import cl.duoc.formativa1.business.InteresCuenta;
import cl.duoc.formativa1.business.MovimientoAnual;
import cl.duoc.formativa1.business.Transaccion;
import cl.duoc.formativa1.items.InteresProcessor;
import cl.duoc.formativa1.items.MovimientoAnualProcessor;
import cl.duoc.formativa1.items.TransaccionProcessor;

class ProcessorAndPolicyTests {

    private final ProcessingThreadTracker tracker = new ProcessingThreadTracker();

    @Test
    void transaccionProcessorNormalizaYConservaAnomalias() throws Exception {
        TransaccionProcessor processor = new TransaccionProcessor(tracker);
        Transaccion valida = new Transaccion(1L, LocalDate.parse("2024-01-01"),
                new BigDecimal("100"), " CREDITO ");
        Transaccion negativa = new Transaccion(2L, LocalDate.parse("2024-01-01"),
                new BigDecimal("-1"), "debito");
        Transaccion tipoNulo = new Transaccion(3L, LocalDate.parse("2024-01-01"),
                BigDecimal.ONE, null);

        assertThat(processor.process(valida).getEstado()).isEqualTo("VALIDA");
        assertThat(valida.getTipo()).isEqualTo("credito");
        assertThat(processor.process(negativa).getEstado()).isEqualTo("ANOMALIA");
        assertThat(processor.process(tipoNulo).getEstado()).isEqualTo("ANOMALIA");
    }

    @Test
    void interesProcessorCalculaSaldosYMarcaTipoDesconocido() throws Exception {
        InteresProcessor processor = new InteresProcessor(tracker);
        InteresCuenta ahorro = cuenta(101L, "5000", "ahorro");
        InteresCuenta prestamo = cuenta(102L, "8000", "prestamo");
        InteresCuenta desconocida = cuenta(105L, "7000", "hipoteca");

        assertThat(processor.process(ahorro).getSaldoFinal()).isEqualByComparingTo("5050.00");
        assertThat(processor.process(prestamo).getSaldoFinal()).isEqualByComparingTo("8160.00");
        assertThat(processor.process(desconocida).getEstado()).isEqualTo("ANOMALIA");
        assertThat(desconocida.getSaldoFinal()).isEqualByComparingTo("7000");
    }

    @Test
    void movimientoProcessorConservaDuplicadosPorLineaYValidaMontoCero() throws Exception {
        MovimientoAnualProcessor processor = new MovimientoAnualProcessor(tracker);
        MovimientoAnual primero = movimiento(101L, "deposito", "1000", " Ingreso mensual ");
        MovimientoAnual repetido = movimiento(101L, " DEPOSITO ", "1000.00", "Ingreso mensual");
        MovimientoAnual mismaLinea = movimiento(101L, "deposito", "1000", "Ingreso mensual");
        MovimientoAnual cero = movimiento(107L, "deposito", "0", "Ingreso");

        primero.setSourceLine(2);
        repetido.setSourceLine(3);
        mismaLinea.setSourceLine(2);
        cero.setSourceLine(4);

        processor.process(primero);
        processor.process(repetido);
        processor.process(mismaLinea);
        processor.process(cero);

        assertThat(primero.getSourceKey()).hasSize(64).isNotEqualTo(repetido.getSourceKey());
        assertThat(primero.getSourceKey()).isEqualTo(mismaLinea.getSourceKey());
        assertThat(primero.getEstado()).isEqualTo("VALIDO");
        assertThat(cero.getEstado()).isEqualTo("ANOMALIA");
    }

    @Test
    void skipPolicyRespetaElLimiteYNoOcultaOtrosErrores() {
        CustomSkipPolicy policy = new CustomSkipPolicy();
        FlatFileParseException parseException =
                new FlatFileParseException("mal formado", "dato", 5);

        assertThat(policy.shouldSkip(parseException, -1)).isTrue();
        assertThat(policy.shouldSkip(parseException, 0)).isTrue();
        assertThat(policy.shouldSkip(parseException, 9)).isTrue();
        assertThat(policy.shouldSkip(parseException, 10)).isFalse();
        assertThat(policy.shouldSkip(new IllegalStateException("infraestructura"), 0)).isFalse();
    }

    @Test
    void deciderSumaSkipsDeTodosLosSteps() {
        CustomDecider decider = new CustomDecider();
        JobExecution jobExecution = new JobExecution(
                1L, new JobInstance(1L, "job"), new JobParameters());
        StepExecution procesamiento = new StepExecution(1L, "procesamiento", jobExecution);
        procesamiento.setReadSkipCount(2);
        StepExecution resumen = new StepExecution(2L, "resumen", jobExecution);
        jobExecution.addStepExecution(procesamiento);
        jobExecution.addStepExecution(resumen);

        assertThat(decider.decide(jobExecution, resumen))
                .isEqualTo(new FlowExecutionStatus("COMPLETED_WITH_SKIPS"));

        procesamiento.setReadSkipCount(0);
        assertThat(decider.decide(jobExecution, resumen))
                .isEqualTo(new FlowExecutionStatus("COMPLETED_CLEAN"));
    }

    private InteresCuenta cuenta(long id, String saldo, String tipo) {
        InteresCuenta cuenta = new InteresCuenta();
        cuenta.setCuentaId(id);
        cuenta.setNombre("Cuenta " + id);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.setEdad(30);
        cuenta.setTipo(tipo);
        return cuenta;
    }

    private MovimientoAnual movimiento(long cuentaId, String tipo, String monto, String descripcion) {
        MovimientoAnual movimiento = new MovimientoAnual();
        movimiento.setCuentaId(cuentaId);
        movimiento.setFecha(LocalDate.parse("2024-01-01"));
        movimiento.setTransaccion(tipo);
        movimiento.setMonto(new BigDecimal(monto));
        movimiento.setDescripcion(descripcion);
        return movimiento;
    }
}
