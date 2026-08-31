package cl.duoc.formativa1.items;

import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import cl.duoc.formativa1.business.Transaccion;

@Configuration
public class TransaccionItemWriterConfig {

    @Bean
    public ItemWriter<Transaccion> transaccionWriter(
            NamedParameterJdbcTemplate jdbcTemplate) {

        return chunk -> {

            String insertTransaccion = """
                INSERT INTO transacciones_procesadas (
                    id,
                    fecha,
                    monto,
                    tipo,
                    estado,
                    observacion
                )
                VALUES (
                    :id,
                    :fecha,
                    :monto,
                    :tipo,
                    :estado,
                    :observacion
                )
                """;

            for (Transaccion transaccion : chunk) {

                MapSqlParameterSource parametros =
                        new MapSqlParameterSource()
                                .addValue(
                                        "id",
                                        transaccion.getId()
                                )
                                .addValue(
                                        "fecha",
                                        Date.valueOf(
                                                transaccion.getFecha()
                                        )
                                )
                                .addValue(
                                        "monto",
                                        transaccion.getMonto()
                                )
                                .addValue(
                                        "tipo",
                                        transaccion.getTipo()
                                )
                                .addValue(
                                        "estado",
                                        transaccion.getEstado()
                                )
                                .addValue(
                                        "observacion",
                                        transaccion.getObservacion()
                                );

                jdbcTemplate.update(
                        insertTransaccion,
                        parametros
                );
            }
        };
    }
}