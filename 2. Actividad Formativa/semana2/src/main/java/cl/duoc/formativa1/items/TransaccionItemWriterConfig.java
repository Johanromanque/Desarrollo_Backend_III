package cl.duoc.formativa1.items;

import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import cl.duoc.formativa1.business.Transaccion;

@Configuration
public class TransaccionItemWriterConfig {

    private static final String UPDATE_SQL = """
            UPDATE transacciones_procesadas SET
                fecha = :fecha,
                monto = :monto,
                tipo = :tipo,
                estado = :estado,
                observacion = :observacion
            WHERE id = :id
            """;

    private static final String INSERT_SQL = """
            INSERT INTO transacciones_procesadas (
                id, fecha, monto, tipo, estado, observacion
            ) VALUES (
                :id, :fecha, :monto, :tipo, :estado, :observacion
            )
            """;

    @Bean
    public ItemWriter<Transaccion> transaccionWriter(JdbcUpsertSupport upsertSupport) {
        return chunk -> {
            for (Transaccion transaccion : chunk) {
                MapSqlParameterSource parameters = new MapSqlParameterSource()
                        .addValue("id", transaccion.getId())
                        .addValue("fecha", Date.valueOf(transaccion.getFecha()))
                        .addValue("monto", transaccion.getMonto())
                        .addValue("tipo", transaccion.getTipo())
                        .addValue("estado", transaccion.getEstado())
                        .addValue("observacion", transaccion.getObservacion());
                upsertSupport.updateThenInsert(UPDATE_SQL, INSERT_SQL, parameters);
            }
        };
    }
}
