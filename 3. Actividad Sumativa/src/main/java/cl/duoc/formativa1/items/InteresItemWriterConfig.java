package cl.duoc.formativa1.items;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import cl.duoc.formativa1.business.InteresCuenta;

@Configuration
public class InteresItemWriterConfig {

    @Bean
    public ItemWriter<InteresCuenta> interesWriter(
            NamedParameterJdbcTemplate jdbcTemplate) {

        return chunk -> {

            String sql = """
                INSERT INTO intereses_calculados (
                    cuenta_id,
                    nombre,
                    saldo_inicial,
                    edad,
                    tipo,
                    tasa_interes,
                    interes_calculado,
                    saldo_final,
                    estado,
                    observacion
                )
                VALUES (
                    :cuentaId,
                    :nombre,
                    :saldo,
                    :edad,
                    :tipo,
                    :tasaInteres,
                    :interesCalculado,
                    :saldoFinal,
                    :estado,
                    :observacion
                )
                """;

            for (InteresCuenta cuenta : chunk) {

                MapSqlParameterSource parametros =
                        new MapSqlParameterSource()
                                .addValue(
                                        "cuentaId",
                                        cuenta.getCuentaId())
                                .addValue(
                                        "nombre",
                                        cuenta.getNombre())
                                .addValue(
                                        "saldo",
                                        cuenta.getSaldo())
                                .addValue(
                                        "edad",
                                        cuenta.getEdad())
                                .addValue(
                                        "tipo",
                                        cuenta.getTipo())
                                .addValue(
                                        "tasaInteres",
                                        cuenta.getTasaInteres())
                                .addValue(
                                        "interesCalculado",
                                        cuenta.getInteresCalculado())
                                .addValue(
                                        "saldoFinal",
                                        cuenta.getSaldoFinal())
                                .addValue(
                                        "estado",
                                        cuenta.getEstado())
                                .addValue(
                                        "observacion",
                                        cuenta.getObservacion());

                jdbcTemplate.update(sql, parametros);
            }
        };
    }
}