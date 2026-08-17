package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.InteresCuenta;

import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class InteresItemReaderConfig {

    @Bean
    public ItemReader<InteresCuenta> interesReader(
            @Value("${app.input-file}") Resource resource) {

        return new FlatFileItemReaderBuilder<InteresCuenta>()
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
    }
}