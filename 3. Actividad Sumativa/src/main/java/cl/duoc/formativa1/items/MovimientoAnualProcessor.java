package cl.duoc.formativa1.items;

import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.business.MovimientoAnual;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    @Override
    public MovimientoAnual process(MovimientoAnual movimiento) {

        System.out.println(
                "[" + Thread.currentThread().getName()
                + "] Procesando movimiento de cuenta ID: "
                + movimiento.getCuentaId()
        );

        String tipo = movimiento.getTransaccion()
                .trim()
                .toLowerCase();

        movimiento.setTransaccion(tipo);

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
}