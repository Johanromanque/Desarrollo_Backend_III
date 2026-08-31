package cl.duoc.formativa1.items;

import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import cl.duoc.formativa1.business.MovimientoAnual;

@Configuration
public class MovimientoAnualItemWriterConfig {

    @Bean
    public ItemWriter<MovimientoAnual> movimientoAnualWriter(
            NamedParameterJdbcTemplate jdbcTemplate) {

        return chunk -> {

            String insertMovimiento = """
                INSERT INTO movimientos_anuales (
                    cuenta_id,
                    fecha,
                    transaccion,
                    monto,
                    descripcion,
                    estado,
                    observacion
                )
                VALUES (
                    :cuentaId,
                    :fecha,
                    :transaccion,
                    :monto,
                    :descripcion,
                    :estado,
                    :observacion
                )
                """;

            for (MovimientoAnual movimiento : chunk) {

                MapSqlParameterSource parametros =
                        new MapSqlParameterSource()
                                .addValue(
                                        "cuentaId",
                                        movimiento.getCuentaId()
                                )
                                .addValue(
                                        "fecha",
                                        Date.valueOf(
                                                movimiento.getFecha()
                                        )
                                )
                                .addValue(
                                        "transaccion",
                                        movimiento.getTransaccion()
                                )
                                .addValue(
                                        "monto",
                                        movimiento.getMonto()
                                )
                                .addValue(
                                        "descripcion",
                                        movimiento.getDescripcion()
                                )
                                .addValue(
                                        "estado",
                                        movimiento.getEstado()
                                )
                                .addValue(
                                        "observacion",
                                        movimiento.getObservacion()
                                );

                jdbcTemplate.update(
                        insertMovimiento,
                        parametros
                );
            }
        };
    }
}