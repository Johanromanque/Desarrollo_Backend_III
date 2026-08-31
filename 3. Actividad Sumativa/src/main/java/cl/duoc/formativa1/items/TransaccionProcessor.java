package cl.duoc.formativa1.items;

import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.business.Transaccion;

@Component
public class TransaccionProcessor
        implements ItemProcessor<Transaccion, Transaccion> {


    @Override
    public Transaccion process(Transaccion transaccion) {

        System.out.println(
                "[" + Thread.currentThread().getName() +
                "] Procesando transacción ID: " +
                transaccion.getId()
        );

        // Normalizar tipo
        String tipo = transaccion.getTipo()
                .trim()
                .toLowerCase();

        transaccion.setTipo(tipo);

        // Validar monto
        if (transaccion.getMonto() == null ||
                transaccion.getMonto().compareTo(BigDecimal.ZERO) <= 0) {

            transaccion.setEstado("ANOMALIA");
            transaccion.setObservacion(
                    "Monto inválido: debe ser mayor que cero"
            );

            return transaccion;
        }

        // Validar tipo de transacción
        if (!tipo.equals("debito") && !tipo.equals("credito")) {

            transaccion.setEstado("ANOMALIA");
            transaccion.setObservacion(
                    "Tipo de transacción no válido"
            );

            return transaccion;
        }

        // Transacción correcta
        transaccion.setEstado("VALIDA");
        transaccion.setObservacion("Sin observaciones");

        return transaccion;
    }
}