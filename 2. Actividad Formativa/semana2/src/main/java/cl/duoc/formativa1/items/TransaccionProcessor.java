package cl.duoc.formativa1.items;

import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.advanced.ProcessingThreadTracker;
import cl.duoc.formativa1.business.Transaccion;

@Component
public class TransaccionProcessor
        implements ItemProcessor<Transaccion, Transaccion> {

    private static final Logger logger = LoggerFactory.getLogger(TransaccionProcessor.class);
    private final ProcessingThreadTracker threadTracker;

    public TransaccionProcessor(ProcessingThreadTracker threadTracker) {
        this.threadTracker = threadTracker;
    }

    @Override
    public Transaccion process(Transaccion transaccion) {

        threadTracker.recordCurrentThread();
        logger.info("[{}] Procesando registro de transacciones",
                Thread.currentThread().getName());

        if (transaccion.getTipo() == null || transaccion.getTipo().isBlank()) {
            transaccion.setEstado("ANOMALIA");
            transaccion.setObservacion("Tipo de transaccion vacio");
            return transaccion;
        }

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
        if (!"debito".equals(tipo) && !"credito".equals(tipo)) {

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
