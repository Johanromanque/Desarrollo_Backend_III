package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.core.io.ClassPathResource;

import cl.duoc.formativa1.business.InteresCuenta;
import cl.duoc.formativa1.business.MovimientoAnual;
import cl.duoc.formativa1.business.Transaccion;
import cl.duoc.formativa1.items.InteresItemReaderConfig;
import cl.duoc.formativa1.items.MovimientoAnualItemReaderConfig;
import cl.duoc.formativa1.items.TransaccionItemReaderConfig;

class CsvReaderTests {

    @Test
    void transaccionesLeeOchoYOmitiriaLasLineasCincoYSeis() throws Exception {
        ItemStreamReader<Transaccion> reader = new TransaccionItemReaderConfig()
                .transaccionReader(new ClassPathResource("transacciones.csv"));
        ResultadoLectura resultado = leer(reader);
        assertThat(resultado.leidos()).isEqualTo(8);
        assertThat(resultado.lineasOmitidas()).containsExactly(5, 6);
    }

    @Test
    void interesesLeeSeisYOmitiriaLasLineasCuatroYCinco() throws Exception {
        ItemStreamReader<InteresCuenta> reader = new InteresItemReaderConfig()
                .interesReader(new ClassPathResource("intereses.csv"));
        ResultadoLectura resultado = leer(reader);
        assertThat(resultado.leidos()).isEqualTo(6);
        assertThat(resultado.lineasOmitidas()).containsExactly(4, 5);
    }

    @Test
    void movimientosLeeOchoYOmitiriaLaLineaSiete() throws Exception {
        ItemStreamReader<MovimientoAnual> reader = new MovimientoAnualItemReaderConfig()
                .movimientoAnualReader(new ClassPathResource("cuentas_anuales.csv"));
        ResultadoLectura resultado = leer(reader);
        assertThat(resultado.leidos()).isEqualTo(8);
        assertThat(resultado.lineasOmitidas()).containsExactly(7);
    }

    private ResultadoLectura leer(ItemStreamReader<?> reader) throws Exception {
        int leidos = 0;
        List<Integer> omitidas = new ArrayList<>();
        reader.open(new ExecutionContext());
        try {
            while (true) {
                try {
                    if (reader.read() == null) {
                        break;
                    }
                    leidos++;
                } catch (FlatFileParseException exception) {
                    omitidas.add(exception.getLineNumber());
                }
            }
        } finally {
            reader.close();
        }
        return new ResultadoLectura(leidos, omitidas);
    }

    private record ResultadoLectura(int leidos, List<Integer> lineasOmitidas) {
    }
}
