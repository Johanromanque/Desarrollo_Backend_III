package cl.duoc.formativa1.items;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

@Service
public class ResumenService {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcUpsertSupport upsertSupport;

    public ResumenService(JdbcTemplate jdbcTemplate, JdbcUpsertSupport upsertSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSupport = upsertSupport;
    }

    public void reconstruirResumenTransacciones() {
        List<ResumenTransaccion> resumenes = jdbcTemplate.query("""
                SELECT fecha,
                       COUNT(*) AS total_transacciones,
                       SUM(CASE WHEN estado = 'VALIDA' AND tipo = 'credito' THEN 1 ELSE 0 END) AS total_creditos,
                       SUM(CASE WHEN estado = 'VALIDA' AND tipo = 'debito' THEN 1 ELSE 0 END) AS total_debitos,
                       SUM(CASE WHEN estado = 'ANOMALIA' THEN 1 ELSE 0 END) AS total_anomalias,
                       SUM(CASE WHEN estado = 'VALIDA' AND tipo = 'credito' THEN monto ELSE 0 END) AS monto_creditos,
                       SUM(CASE WHEN estado = 'VALIDA' AND tipo = 'debito' THEN monto ELSE 0 END) AS monto_debitos
                FROM transacciones_procesadas
                GROUP BY fecha
                """, (resultSet, rowNum) -> new ResumenTransaccion(
                resultSet.getDate("fecha").toLocalDate(),
                resultSet.getInt("total_transacciones"),
                resultSet.getInt("total_creditos"),
                resultSet.getInt("total_debitos"),
                resultSet.getInt("total_anomalias"),
                resultSet.getBigDecimal("monto_creditos"),
                resultSet.getBigDecimal("monto_debitos")));

        for (ResumenTransaccion resumen : resumenes) {
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("fecha", Date.valueOf(resumen.fecha()))
                    .addValue("totalTransacciones", resumen.totalTransacciones())
                    .addValue("totalCreditos", resumen.totalCreditos())
                    .addValue("totalDebitos", resumen.totalDebitos())
                    .addValue("totalAnomalias", resumen.totalAnomalias())
                    .addValue("montoCreditos", resumen.montoCreditos())
                    .addValue("montoDebitos", resumen.montoDebitos());
            upsertSupport.updateThenInsert("""
                    UPDATE resumen_transacciones_diarias SET
                        total_transacciones = :totalTransacciones,
                        total_creditos = :totalCreditos,
                        total_debitos = :totalDebitos,
                        total_anomalias = :totalAnomalias,
                        monto_creditos = :montoCreditos,
                        monto_debitos = :montoDebitos
                    WHERE fecha = :fecha
                    """, """
                    INSERT INTO resumen_transacciones_diarias (
                        fecha, total_transacciones, total_creditos, total_debitos,
                        total_anomalias, monto_creditos, monto_debitos
                    ) VALUES (
                        :fecha, :totalTransacciones, :totalCreditos, :totalDebitos,
                        :totalAnomalias, :montoCreditos, :montoDebitos
                    )
                    """, parameters);
        }
    }

    public void reconstruirEstadosCuenta() {
        List<ResumenCuenta> resumenes = jdbcTemplate.query("""
                SELECT cuenta_id,
                       SUM(CASE WHEN estado = 'VALIDO' AND transaccion = 'deposito' THEN monto ELSE 0 END) AS total_ingresos,
                       SUM(CASE WHEN estado = 'VALIDO' AND transaccion IN ('retiro', 'compra') THEN ABS(monto) ELSE 0 END) AS total_egresos,
                       SUM(CASE WHEN estado = 'VALIDO' THEN monto ELSE 0 END) AS saldo_neto,
                       COUNT(*) AS cantidad_movimientos,
                       SUM(CASE WHEN estado = 'ANOMALIA' THEN 1 ELSE 0 END) AS cantidad_anomalias
                FROM movimientos_anuales
                GROUP BY cuenta_id
                """, (resultSet, rowNum) -> new ResumenCuenta(
                resultSet.getLong("cuenta_id"),
                resultSet.getBigDecimal("total_ingresos"),
                resultSet.getBigDecimal("total_egresos"),
                resultSet.getBigDecimal("saldo_neto"),
                resultSet.getInt("cantidad_movimientos"),
                resultSet.getInt("cantidad_anomalias")));

        for (ResumenCuenta resumen : resumenes) {
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("cuentaId", resumen.cuentaId())
                    .addValue("totalIngresos", resumen.totalIngresos())
                    .addValue("totalEgresos", resumen.totalEgresos())
                    .addValue("saldoNeto", resumen.saldoNeto())
                    .addValue("cantidadMovimientos", resumen.cantidadMovimientos())
                    .addValue("cantidadAnomalias", resumen.cantidadAnomalias());
            upsertSupport.updateThenInsert("""
                    UPDATE estados_cuenta_anuales SET
                        total_ingresos = :totalIngresos,
                        total_egresos = :totalEgresos,
                        saldo_neto = :saldoNeto,
                        cantidad_movimientos = :cantidadMovimientos,
                        cantidad_anomalias = :cantidadAnomalias
                    WHERE cuenta_id = :cuentaId
                    """, """
                    INSERT INTO estados_cuenta_anuales (
                        cuenta_id, total_ingresos, total_egresos, saldo_neto,
                        cantidad_movimientos, cantidad_anomalias
                    ) VALUES (
                        :cuentaId, :totalIngresos, :totalEgresos, :saldoNeto,
                        :cantidadMovimientos, :cantidadAnomalias
                    )
                    """, parameters);
        }
    }

    private record ResumenTransaccion(
            LocalDate fecha,
            int totalTransacciones,
            int totalCreditos,
            int totalDebitos,
            int totalAnomalias,
            BigDecimal montoCreditos,
            BigDecimal montoDebitos) {
    }

    private record ResumenCuenta(
            long cuentaId,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal saldoNeto,
            int cantidadMovimientos,
            int cantidadAnomalias) {
    }
}
