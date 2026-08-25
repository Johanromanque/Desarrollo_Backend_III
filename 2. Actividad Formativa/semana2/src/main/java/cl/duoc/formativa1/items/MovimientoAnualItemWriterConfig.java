package cl.duoc.formativa1.items;

import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import cl.duoc.formativa1.business.MovimientoAnual;

@Configuration
public class MovimientoAnualItemWriterConfig {

    private static final String UPDATE_SQL = """
            UPDATE movimientos_anuales SET
                source_line = :sourceLine,
                cuenta_id = :cuentaId,
                fecha = :fecha,
                transaccion = :transaccion,
                monto = :monto,
                descripcion = :descripcion,
                estado = :estado,
                observacion = :observacion
            WHERE source_key = :sourceKey
            """;

    private static final String INSERT_SQL = """
            INSERT INTO movimientos_anuales (
                source_key, source_line, cuenta_id, fecha, transaccion, monto,
                descripcion, estado, observacion
            ) VALUES (
                :sourceKey, :sourceLine, :cuentaId, :fecha, :transaccion, :monto,
                :descripcion, :estado, :observacion
            )
            """;

    @Bean
    public ItemWriter<MovimientoAnual> movimientoAnualWriter(JdbcUpsertSupport upsertSupport) {
        return chunk -> {
            for (MovimientoAnual movimiento : chunk) {
                MapSqlParameterSource parameters = new MapSqlParameterSource()
                        .addValue("sourceKey", movimiento.getSourceKey())
                        .addValue("sourceLine", movimiento.getSourceLine())
                        .addValue("cuentaId", movimiento.getCuentaId())
                        .addValue("fecha", Date.valueOf(movimiento.getFecha()))
                        .addValue("transaccion", movimiento.getTransaccion())
                        .addValue("monto", movimiento.getMonto())
                        .addValue("descripcion", movimiento.getDescripcion())
                        .addValue("estado", movimiento.getEstado())
                        .addValue("observacion", movimiento.getObservacion());
                upsertSupport.updateThenInsert(UPDATE_SQL, INSERT_SQL, parameters);
            }
        };
    }
}
