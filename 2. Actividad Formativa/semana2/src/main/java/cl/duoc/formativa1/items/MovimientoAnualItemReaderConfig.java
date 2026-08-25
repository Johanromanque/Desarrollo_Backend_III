package cl.duoc.formativa1.items;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import cl.duoc.formativa1.business.MovimientoAnual;

@Configuration
public class MovimientoAnualItemReaderConfig {

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MovimientoAnual> movimientoAnualReader(
            @Value("${app.input-file:classpath:cuentas_anuales.csv}") Resource resource) {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames(
                "cuenta_id",
                "fecha",
                "transaccion",
                "monto",
                "descripcion");

        LineMapper<MovimientoAnual> lineMapper = (line, lineNumber) -> {
            FieldSet fieldSet = tokenizer.tokenize(line);
            MovimientoAnual movimiento = new MovimientoAnual();
            movimiento.setSourceLine(lineNumber);
            movimiento.setCuentaId(fieldSet.readLong("cuenta_id"));
            movimiento.setFecha(LocalDate.parse(fieldSet.readString("fecha")));
            movimiento.setTransaccion(fieldSet.readString("transaccion"));
            movimiento.setMonto(new BigDecimal(fieldSet.readString("monto")));
            movimiento.setDescripcion(fieldSet.readString("descripcion"));
            return movimiento;
        };

        FlatFileItemReader<MovimientoAnual> delegate = new FlatFileItemReaderBuilder<MovimientoAnual>()
                .name("movimientoAnualReader")
                .resource(resource)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();

        return new SynchronizedItemStreamReaderBuilder<MovimientoAnual>()
                .delegate(delegate)
                .build();
    }
}
