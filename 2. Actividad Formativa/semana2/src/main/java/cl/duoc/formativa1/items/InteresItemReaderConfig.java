package cl.duoc.formativa1.items;

import java.math.BigDecimal;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import cl.duoc.formativa1.business.InteresCuenta;

@Configuration
public class InteresItemReaderConfig {

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<InteresCuenta> interesReader(
            @Value("${app.input-file:classpath:intereses.csv}") Resource resource) {

        FlatFileItemReader<InteresCuenta> delegate = new FlatFileItemReaderBuilder<InteresCuenta>()
                .name("interesReader")
                .resource(resource)
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuenta_id",
                                "nombre",
                                "saldo",
                                "edad",
                                "tipo"))
                .fieldSetMapper(fieldSet -> {

                    InteresCuenta cuenta = new InteresCuenta();

                    cuenta.setCuentaId(
                            fieldSet.readLong("cuenta_id"));

                    cuenta.setNombre(
                            fieldSet.readString("nombre"));

                    cuenta.setSaldo(
                            new BigDecimal(
                                    fieldSet.readString("saldo")));

                    cuenta.setEdad(
                            fieldSet.readInt("edad"));

                    cuenta.setTipo(
                            fieldSet.readString("tipo"));

                    return cuenta;
                })
                .build();

        return new SynchronizedItemStreamReaderBuilder<InteresCuenta>()
                .delegate(delegate)
                .build();
    }
}
