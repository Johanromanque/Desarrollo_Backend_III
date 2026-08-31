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

import cl.duoc.formativa1.business.MovimientoAnual;

@Configuration
public class MovimientoAnualItemReaderConfig {

    private static final List<DateTimeFormatter> FORMATOS_FECHA =
            List.of(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")
            );

    @Bean
    @StepScope
    public FlatFileItemReader<MovimientoAnual> movimientoAnualReader(

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


        return new FlatFileItemReaderBuilder<MovimientoAnual>()

                .name("movimientoAnualReader")

                .resource(inputFile)

                .encoding("UTF-8")

                .linesToSkip(1)

                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuenta_id",
                                "fecha",
                                "transaccion",
                                "monto",
                                "descripcion"
                        )
                )

                .fieldSetMapper(fieldSet -> {

                    MovimientoAnual movimiento =
                            new MovimientoAnual();

                    movimiento.setCuentaId(
                            fieldSet.readLong(
                                    "cuenta_id"
                            )
                    );

                    movimiento.setFecha(
                            convertirFecha(
                                    fieldSet.readString(
                                            "fecha"
                                    )
                            )
                    );

                    movimiento.setTransaccion(
                            fieldSet.readString(
                                    "transaccion"
                            )
                    );

                    movimiento.setMonto(
                            convertirMonto(
                                    fieldSet.readString(
                                            "monto"
                                    )
                            )
                    );

                    movimiento.setDescripcion(
                            fieldSet.readString(
                                    "descripcion"
                            )
                    );

                    return movimiento;
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
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
                valor.trim()
        );
    }
}