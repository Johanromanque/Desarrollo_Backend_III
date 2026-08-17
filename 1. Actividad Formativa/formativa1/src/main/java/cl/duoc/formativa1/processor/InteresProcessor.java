package cl.duoc.formativa1.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.model.InteresCuenta;

@Component
public class InteresProcessor
        implements ItemProcessor<InteresCuenta, InteresCuenta> {

    private static final BigDecimal TASA_AHORRO =
            new BigDecimal("0.01");

    private static final BigDecimal TASA_PRESTAMO =
            new BigDecimal("0.02");


    @Override
    public InteresCuenta process(InteresCuenta cuenta) {

        String tipo = cuenta.getTipo()
                .trim()
                .toLowerCase();

        cuenta.setTipo(tipo);


        // =====================================
        // VALIDACION DE SALDO
        // =====================================

        if (cuenta.getSaldo() == null ||
                cuenta.getSaldo()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            cuenta.setTasaInteres(BigDecimal.ZERO);
            cuenta.setInteresCalculado(BigDecimal.ZERO);
            cuenta.setSaldoFinal(cuenta.getSaldo());

            cuenta.setEstado("ANOMALIA");
            cuenta.setObservacion(
                    "Saldo debe ser mayor que cero");

            return cuenta;
        }


        // =====================================
        // CUENTA DE AHORRO
        // =====================================

        if ("ahorro".equals(tipo)) {

            calcularInteres(
                    cuenta,
                    TASA_AHORRO);

            cuenta.setEstado("PROCESADA");
            cuenta.setObservacion(
                    "Interés de ahorro aplicado");

            return cuenta;
        }


        // =====================================
        // CUENTA DE PRESTAMO
        // =====================================

        if ("prestamo".equals(tipo)) {

            calcularInteres(
                    cuenta,
                    TASA_PRESTAMO);

            cuenta.setEstado("PROCESADA");
            cuenta.setObservacion(
                    "Interés de préstamo aplicado");

            return cuenta;
        }


        // =====================================
        // TIPO NO VALIDO
        // =====================================

        cuenta.setTasaInteres(BigDecimal.ZERO);
        cuenta.setInteresCalculado(BigDecimal.ZERO);
        cuenta.setSaldoFinal(cuenta.getSaldo());

        cuenta.setEstado("ANOMALIA");
        cuenta.setObservacion(
                "Tipo de cuenta no válido para este proceso");

        return cuenta;
    }


    private void calcularInteres(
            InteresCuenta cuenta,
            BigDecimal tasa) {

        BigDecimal interes =
                cuenta.getSaldo()
                        .multiply(tasa)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        BigDecimal saldoFinal =
                cuenta.getSaldo()
                        .add(interes)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        cuenta.setTasaInteres(tasa);
        cuenta.setInteresCalculado(interes);
        cuenta.setSaldoFinal(saldoFinal);
    }
}