package cl.duoc.formativa1.items;


import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import cl.duoc.formativa1.business.Transaccion;

@Configuration
public class TransaccionItemReaderConfig {

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaccion> transaccionReader(
            @Value("${app.input-file:classpath:transacciones.csv}") Resource resource) {

        FlatFileItemReader<Transaccion> delegate = new FlatFileItemReaderBuilder<Transaccion>()
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

        return new SynchronizedItemStreamReaderBuilder<Transaccion>()
                .delegate(delegate)
                .build();
    }
}
