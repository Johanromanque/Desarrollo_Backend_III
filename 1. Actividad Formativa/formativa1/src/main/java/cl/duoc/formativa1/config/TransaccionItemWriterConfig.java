package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.Transaccion;

import java.math.BigDecimal;
import java.sql.Date;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class TransaccionItemWriterConfig {

    @Bean
    public ItemWriter<Transaccion> transaccionWriter(
            NamedParameterJdbcTemplate jdbcTemplate) {

        return chunk -> {

            for (Transaccion transaccion : chunk) {

                // ==========================================
                // 1. GUARDAR TRANSACCION PROCESADA
                // ==========================================

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

                MapSqlParameterSource parametros =
                        new MapSqlParameterSource()
                                .addValue("id", transaccion.getId())
                                .addValue(
                                        "fecha",
                                        Date.valueOf(transaccion.getFecha())
                                )
                                .addValue("monto", transaccion.getMonto())
                                .addValue("tipo", transaccion.getTipo())
                                .addValue("estado", transaccion.getEstado())
                                .addValue(
                                        "observacion",
                                        transaccion.getObservacion()
                                );

                jdbcTemplate.update(
                        insertTransaccion,
                        parametros
                );


                // ==========================================
                // 2. ACTUALIZAR RESUMEN DIARIO
                // ==========================================

                boolean valida =
                        "VALIDA".equals(transaccion.getEstado());

                int esCredito =
                        valida && "credito".equals(transaccion.getTipo())
                                ? 1 : 0;

                int esDebito =
                        valida && "debito".equals(transaccion.getTipo())
                                ? 1 : 0;

                int esAnomalia =
                        "ANOMALIA".equals(transaccion.getEstado())
                                ? 1 : 0;

                BigDecimal montoCredito =
                        esCredito == 1
                                ? transaccion.getMonto()
                                : BigDecimal.ZERO;

                BigDecimal montoDebito =
                        esDebito == 1
                                ? transaccion.getMonto()
                                : BigDecimal.ZERO;


                String actualizarResumen = """
                    MERGE INTO resumen_transacciones_diarias r
                    USING (
                        SELECT :fecha AS fecha
                        FROM dual
                    ) origen
                    ON (r.fecha = origen.fecha)

                    WHEN MATCHED THEN
                        UPDATE SET
                            total_transacciones =
                                r.total_transacciones + 1,

                            total_creditos =
                                r.total_creditos + :esCredito,

                            total_debitos =
                                r.total_debitos + :esDebito,

                            total_anomalias =
                                r.total_anomalias + :esAnomalia,

                            monto_creditos =
                                r.monto_creditos + :montoCredito,

                            monto_debitos =
                                r.monto_debitos + :montoDebito

                    WHEN NOT MATCHED THEN
                        INSERT (
                            fecha,
                            total_transacciones,
                            total_creditos,
                            total_debitos,
                            total_anomalias,
                            monto_creditos,
                            monto_debitos
                        )
                        VALUES (
                            :fecha,
                            1,
                            :esCredito,
                            :esDebito,
                            :esAnomalia,
                            :montoCredito,
                            :montoDebito
                        )
                    """;

                MapSqlParameterSource resumen =
                        new MapSqlParameterSource()
                                .addValue(
                                        "fecha",
                                        Date.valueOf(transaccion.getFecha())
                                )
                                .addValue("esCredito", esCredito)
                                .addValue("esDebito", esDebito)
                                .addValue("esAnomalia", esAnomalia)
                                .addValue("montoCredito", montoCredito)
                                .addValue("montoDebito", montoDebito);

                jdbcTemplate.update(
                        actualizarResumen,
                        resumen
                );
            }
        };
    }
}