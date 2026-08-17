package cl.duoc.formativa1.config;


import cl.duoc.formativa1.model.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class TransaccionItemReaderConfig {

    @Bean
    public ItemReader<Transaccion> transaccionReader(
            @Value("${app.input-file}") Resource resource) {

        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionReader")
                .resource(resource)
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names("id", "fecha", "monto", "tipo"))
                .fieldSetMapper(fieldSet -> {

                    Transaccion transaccion = new Transaccion();

                    transaccion.setId(fieldSet.readLong("id"));
                    transaccion.setFecha(
                            LocalDate.parse(fieldSet.readString("fecha"))
                    );
                    transaccion.setMonto(
                            new BigDecimal(fieldSet.readString("monto"))
                    );
                    transaccion.setTipo(fieldSet.readString("tipo"));

                    return transaccion;
                })
                .build();
    }
}