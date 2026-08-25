package cl.duoc.formativa1.items;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import cl.duoc.formativa1.business.InteresCuenta;

@Configuration
public class InteresItemWriterConfig {

    private static final String UPDATE_SQL = """
            UPDATE intereses_calculados SET
                nombre = :nombre,
                saldo_inicial = :saldo,
                edad = :edad,
                tipo = :tipo,
                tasa_interes = :tasaInteres,
                interes_calculado = :interesCalculado,
                saldo_final = :saldoFinal,
                estado = :estado,
                observacion = :observacion
            WHERE cuenta_id = :cuentaId
            """;

    private static final String INSERT_SQL = """
            INSERT INTO intereses_calculados (
                cuenta_id, nombre, saldo_inicial, edad, tipo,
                tasa_interes, interes_calculado, saldo_final, estado, observacion
            ) VALUES (
                :cuentaId, :nombre, :saldo, :edad, :tipo,
                :tasaInteres, :interesCalculado, :saldoFinal, :estado, :observacion
            )
            """;

    @Bean
    public ItemWriter<InteresCuenta> interesWriter(JdbcUpsertSupport upsertSupport) {
        return chunk -> {
            for (InteresCuenta cuenta : chunk) {
                MapSqlParameterSource parameters = new MapSqlParameterSource()
                        .addValue("cuentaId", cuenta.getCuentaId())
                        .addValue("nombre", cuenta.getNombre())
                        .addValue("saldo", cuenta.getSaldo())
                        .addValue("edad", cuenta.getEdad())
                        .addValue("tipo", cuenta.getTipo())
                        .addValue("tasaInteres", cuenta.getTasaInteres())
                        .addValue("interesCalculado", cuenta.getInteresCalculado())
                        .addValue("saldoFinal", cuenta.getSaldoFinal())
                        .addValue("estado", cuenta.getEstado())
                        .addValue("observacion", cuenta.getObservacion());
                upsertSupport.updateThenInsert(UPDATE_SQL, INSERT_SQL, parameters);
            }
        };
    }
}
