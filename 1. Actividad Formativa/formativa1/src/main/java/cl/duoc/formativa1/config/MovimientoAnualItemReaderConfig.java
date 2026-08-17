package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.MovimientoAnual;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class MovimientoAnualItemReaderConfig {

    @Bean
    public ItemReader<MovimientoAnual> movimientoAnualReader(
            @Value("${app.input-file}") Resource resource) {

        return new FlatFileItemReaderBuilder<MovimientoAnual>()
                .name("movimientoAnualReader")
                .resource(resource)
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuenta_id",
                                "fecha",
                                "transaccion",
                                "monto",
                                "descripcion"))
                .fieldSetMapper(fieldSet -> {

                    MovimientoAnual movimiento =
                            new MovimientoAnual();

                    movimiento.setCuentaId(
                            fieldSet.readLong("cuenta_id"));

                    movimiento.setFecha(
                            LocalDate.parse(
                                    fieldSet.readString("fecha")));

                    movimiento.setTransaccion(
                            fieldSet.readString("transaccion"));

                    movimiento.setMonto(
                            new BigDecimal(
                                    fieldSet.readString("monto")));

                    movimiento.setDescripcion(
                            fieldSet.readString("descripcion"));

                    return movimiento;
                })
                .build();
    }
}