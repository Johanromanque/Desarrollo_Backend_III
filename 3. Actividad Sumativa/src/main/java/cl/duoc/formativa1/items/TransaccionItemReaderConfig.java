package cl.duoc.formativa1.items;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import cl.duoc.formativa1.business.Transaccion;

@Configuration
public class TransaccionItemReaderConfig {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Bean
    @StepScope
    public FlatFileItemReader<Transaccion> transaccionReader(
            @Value("#{stepExecutionContext['file']}") Object resourceObj,
            @Value("${app.input-file}") Resource defaultInputFile) {

        Resource inputFile;

        // Cada partición recibe su propio archivo.
        if (resourceObj instanceof Resource) {

            inputFile = (Resource) resourceObj;

        } else if (resourceObj instanceof String) {

            inputFile = new DefaultResourceLoader()
                    .getResource((String) resourceObj);

        } else {

            // Permite mantener un archivo por defecto.
            inputFile = defaultInputFile;
        }

        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionReader")
                .resource(inputFile)
                .encoding("UTF-8")
                .linesToSkip(1)

                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "id",
                                "fecha",
                                "monto",
                                "tipo"
                        )
                )

                .fieldSetMapper(fieldSet -> {

                    Transaccion transaccion =
                            new Transaccion();

                    transaccion.setId(
                            fieldSet.readLong("id")
                    );

                    transaccion.setFecha(
                            convertirFecha(
                                    fieldSet.readString("fecha")
                            )
                    );

                    transaccion.setMonto(
                            convertirMonto(
                                    fieldSet.readString("monto")
                            )
                    );

                    transaccion.setTipo(
                            fieldSet.readString("tipo")
                    );

                    return transaccion;
                })

                .build();
    }


    private LocalDate convertirFecha(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String fecha = valor.trim();

        for (DateTimeFormatter formato : FORMATOS_FECHA) {

            try {

                return LocalDate.parse(
                        fecha,
                        formato
                );

            } catch (DateTimeParseException ignored) {

                // Intentar con el siguiente formato.
            }
        }

        throw new DateTimeParseException(
                "Formato de fecha no reconocido",
                fecha,
                0
        );
    }


    private BigDecimal convertirMonto(String valor) {

        if (valor == null || valor.isBlank()) {

            /*
             * En los CSV nuevos existen montos vacíos.
             * Se convierten a cero para que el Processor
             * pueda clasificarlos como ANOMALIA.
             */
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
                valor.trim()
        );
    }
}