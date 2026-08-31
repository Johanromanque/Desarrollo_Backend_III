package cl.duoc.formativa1.items;

import java.math.BigDecimal;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import cl.duoc.formativa1.business.InteresCuenta;

@Configuration
public class InteresItemReaderConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<InteresCuenta> interesReader(

            @Value("#{stepExecutionContext['file']}")
            Object resourceObj,

            @Value("${app.input-file}")
            Resource defaultInputFile) {


        Resource inputFile;

        if (resourceObj instanceof Resource) {

            inputFile = (Resource) resourceObj;

        } else if (resourceObj instanceof String) {

            inputFile = new DefaultResourceLoader()
                    .getResource((String) resourceObj);

        } else {

            inputFile = defaultInputFile;
        }


        return new FlatFileItemReaderBuilder<InteresCuenta>()

                .name("interesReader")

                .resource(inputFile)

                .encoding("UTF-8")

                .linesToSkip(1)

                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuenta_id",
                                "nombre",
                                "saldo",
                                "edad",
                                "tipo"
                        )
                )

                .fieldSetMapper(fieldSet -> {

                    InteresCuenta cuenta =
                            new InteresCuenta();

                    cuenta.setCuentaId(
                            fieldSet.readLong(
                                    "cuenta_id"
                            )
                    );

                    cuenta.setNombre(
                            fieldSet.readString(
                                    "nombre"
                            )
                    );

                    cuenta.setSaldo(
                            convertirSaldo(
                                    fieldSet.readString(
                                            "saldo"
                                    )
                            )
                    );

                    cuenta.setEdad(
                            convertirEdad(
                                    fieldSet.readString(
                                            "edad"
                                    )
                            )
                    );

                    cuenta.setTipo(
                            fieldSet.readString(
                                    "tipo"
                            )
                    );

                    return cuenta;
                })

                .build();
    }


    private BigDecimal convertirSaldo(
            String valor) {

        if (valor == null ||
                valor.isBlank()) {

            return BigDecimal.ZERO;
        }

        return new BigDecimal(
                valor.trim()
        );
    }


    private Integer convertirEdad(
            String valor) {

        if (valor == null ||
                valor.isBlank()) {

            return null;
        }

        return Integer.valueOf(
                valor.trim()
        );
    }
}