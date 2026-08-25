package cl.duoc.formativa1.items;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.advanced.ProcessingThreadTracker;
import cl.duoc.formativa1.business.MovimientoAnual;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    private static final Logger logger = LoggerFactory.getLogger(MovimientoAnualProcessor.class);
    private final ProcessingThreadTracker threadTracker;

    public MovimientoAnualProcessor(ProcessingThreadTracker threadTracker) {
        this.threadTracker = threadTracker;
    }

    @Override
    public MovimientoAnual process(MovimientoAnual movimiento) {

        threadTracker.recordCurrentThread();
        logger.info("[{}] Procesando registro de movimientos anuales",
                Thread.currentThread().getName());

        if (movimiento.getDescripcion() != null) {
            movimiento.setDescripcion(movimiento.getDescripcion().trim());
        }

        if (movimiento.getTransaccion() == null || movimiento.getTransaccion().isBlank()) {
            movimiento.setSourceKey(calcularSourceKey(movimiento, ""));
            movimiento.setEstado("ANOMALIA");
            movimiento.setObservacion("Tipo de transaccion vacio");
            return movimiento;
        }

        String tipo = movimiento.getTransaccion()
                .trim()
                .toLowerCase();

        movimiento.setTransaccion(tipo);
        movimiento.setSourceKey(calcularSourceKey(movimiento, tipo));

        // Monto nulo o cero
        if (movimiento.getMonto() == null ||
                movimiento.getMonto()
                        .compareTo(BigDecimal.ZERO) == 0) {

            movimiento.setEstado("ANOMALIA");
            movimiento.setObservacion(
                    "El monto no puede ser cero");

            return movimiento;
        }

        // Deposito debe ser positivo
        if ("deposito".equals(tipo)) {

            if (movimiento.getMonto()
                    .compareTo(BigDecimal.ZERO) < 0) {

                movimiento.setEstado("ANOMALIA");
                movimiento.setObservacion(
                        "Un depósito debe tener monto positivo");

                return movimiento;
            }

            movimiento.setEstado("VALIDO");
            movimiento.setObservacion("Sin observaciones");

            return movimiento;
        }

        // Retiro o compra deben ser negativos
        if ("retiro".equals(tipo) ||
                "compra".equals(tipo)) {

            if (movimiento.getMonto()
                    .compareTo(BigDecimal.ZERO) > 0) {

                movimiento.setEstado("ANOMALIA");
                movimiento.setObservacion(
                        "Retiro o compra debe tener monto negativo");

                return movimiento;
            }

            movimiento.setEstado("VALIDO");
            movimiento.setObservacion("Sin observaciones");

            return movimiento;
        }

        // Tipo desconocido
        movimiento.setEstado("ANOMALIA");
        movimiento.setObservacion(
                "Tipo de transacción no válido");

        return movimiento;
    }

    private String calcularSourceKey(MovimientoAnual movimiento, String tipoNormalizado) {
        String monto = movimiento.getMonto() == null
                ? ""
                : movimiento.getMonto().stripTrailingZeros().toPlainString();
        String descripcion = movimiento.getDescripcion() == null ? "" : movimiento.getDescripcion();
        String normalizado = String.join("|",
                String.valueOf(movimiento.getSourceLine()),
                String.valueOf(movimiento.getCuentaId()),
                String.valueOf(movimiento.getFecha()),
                tipoNormalizado,
                monto,
                descripcion);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible", exception);
        }
    }
}
