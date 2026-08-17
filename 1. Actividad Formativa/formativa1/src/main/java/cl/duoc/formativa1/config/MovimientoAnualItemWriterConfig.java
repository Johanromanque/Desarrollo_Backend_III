package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.MovimientoAnual;

import java.math.BigDecimal;
import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class MovimientoAnualItemWriterConfig {

    @Bean
    public ItemWriter<MovimientoAnual> movimientoAnualWriter(
            NamedParameterJdbcTemplate jdbcTemplate) {

        return chunk -> {

            for (MovimientoAnual movimiento : chunk) {

                // =====================================
                // 1. GUARDAR MOVIMIENTO
                // =====================================

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

                MapSqlParameterSource parametros =
                        new MapSqlParameterSource()
                                .addValue(
                                        "cuentaId",
                                        movimiento.getCuentaId())
                                .addValue(
                                        "fecha",
                                        Date.valueOf(movimiento.getFecha()))
                                .addValue(
                                        "transaccion",
                                        movimiento.getTransaccion())
                                .addValue(
                                        "monto",
                                        movimiento.getMonto())
                                .addValue(
                                        "descripcion",
                                        movimiento.getDescripcion())
                                .addValue(
                                        "estado",
                                        movimiento.getEstado())
                                .addValue(
                                        "observacion",
                                        movimiento.getObservacion());

                jdbcTemplate.update(
                        insertMovimiento,
                        parametros);


                // =====================================
                // 2. CALCULAR RESUMEN
                // =====================================

                boolean valido =
                        "VALIDO".equals(movimiento.getEstado());

                BigDecimal ingresos = BigDecimal.ZERO;
                BigDecimal egresos = BigDecimal.ZERO;
                BigDecimal saldoNeto = BigDecimal.ZERO;

                if (valido) {

                    if ("deposito".equals(
                            movimiento.getTransaccion())) {

                        ingresos = movimiento.getMonto();

                    } else {

                        // Guardamos egresos como número positivo
                        egresos = movimiento.getMonto().abs();
                    }

                    // El monto original conserva el signo
                    saldoNeto = movimiento.getMonto();
                }

                int anomalia =
                        "ANOMALIA".equals(movimiento.getEstado())
                                ? 1 : 0;


                // =====================================
                // 3. ACTUALIZAR ESTADO DE CUENTA
                // =====================================

                String mergeResumen = """
                    MERGE INTO estados_cuenta_anuales e
                    USING (
                        SELECT :cuentaId AS cuenta_id
                        FROM dual
                    ) origen
                    ON (e.cuenta_id = origen.cuenta_id)

                    WHEN MATCHED THEN
                        UPDATE SET
                            e.total_ingresos =
                                e.total_ingresos + :ingresos,

                            e.total_egresos =
                                e.total_egresos + :egresos,

                            e.saldo_neto =
                                e.saldo_neto + :saldoNeto,

                            e.cantidad_movimientos =
                                e.cantidad_movimientos + 1,

                            e.cantidad_anomalias =
                                e.cantidad_anomalias + :anomalia

                    WHEN NOT MATCHED THEN
                        INSERT (
                            cuenta_id,
                            total_ingresos,
                            total_egresos,
                            saldo_neto,
                            cantidad_movimientos,
                            cantidad_anomalias
                        )
                        VALUES (
                            :cuentaId,
                            :ingresos,
                            :egresos,
                            :saldoNeto,
                            1,
                            :anomalia
                        )
                    """;

                MapSqlParameterSource resumen =
                        new MapSqlParameterSource()
                                .addValue(
                                        "cuentaId",
                                        movimiento.getCuentaId())
                                .addValue(
                                        "ingresos",
                                        ingresos)
                                .addValue(
                                        "egresos",
                                        egresos)
                                .addValue(
                                        "saldoNeto",
                                        saldoNeto)
                                .addValue(
                                        "anomalia",
                                        anomalia);

                jdbcTemplate.update(
                        mergeResumen,
                        resumen);
            }
        };
    }
}