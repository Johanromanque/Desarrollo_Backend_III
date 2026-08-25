# Banco XYZ - Procesos batch de la semana 2

Proyecto formativo de Desarrollo Backend III que moderniza tres procesos legacy con Spring Batch:

1. reporte de transacciones diarias;
2. calculo de intereses mensuales;
3. generacion de estados de cuenta anuales.

La solucion usa Java 17, Spring Boot 4.1, Spring Batch 6, Spring JDBC y Oracle. Las pruebas se ejecutan con H2 en memoria y no necesitan Wallet, red ni una Oracle externa.

## Requisitos implementados

- Un Job independiente para cada proceso.
- Procesamiento orientado a chunks de tamaño 5.
- `ThreadPoolTaskExecutor` con 3 hilos.
- Politicas personalizadas de skip, retry y finalizacion.
- Maximo de 10 omisiones; la numero 11 hace fallar el Step.
- Hasta 3 reintentos de errores transitorios, con 500 ms de espera.
- Readers con alcance de Step, nombre estable, estado persistente y acceso sincronizado.
- Writers idempotentes mediante update-then-insert y control de inserciones concurrentes.
- Resumenes recalculados desde el detalle con valores absolutos.
- Logs en consola y `logs/formativa1.log` sin filas CSV, nombres, cuentas ni montos.
- Perfiles separados para pruebas y Oracle.

## Arquitectura

```text
CSV -> ItemReader -> ItemProcessor -> ItemWriter -> detalle
                                               |
                                               v
                                  Step de recálculo de resumen
                                               |
                                               v
                                         CustomDecider
```

`transaccionesJob` y `estadosCuentaJob` tienen un Step adicional que reconstruye sus resumenes. `interesesJob` solo necesita la tabla de detalle calculado.

## Configuracion batch

Los valores efectivos se encuentran en `src/main/resources/application.properties`:

```properties
app.batch.chunk-size=5
app.batch.core-pool-size=3
app.batch.max-pool-size=3
app.batch.queue-capacity=15
```

`BatchProperties` valida que los tamaños sean positivos, que el maximo no sea menor al core y que la cola no sea negativa. `BatchConfig` tambien exige un pool JDBC con al menos una conexion adicional a los threads para el `JobRepository`.

## Perfiles

### test

Usa H2 en memoria, crea automaticamente el esquema local y no inicia Jobs al cargar el contexto. Solo se activa desde las pruebas.

```bash
sh mvnw test
```

### oracle

La conexion solo se obtiene desde variables de entorno:

```bash
export ORACLE_JDBC_URL='jdbc:oracle:thin:@SERVICIO?TNS_ADMIN=/ruta/al/wallet'
export ORACLE_DB_USERNAME='USUARIO_ESQUEMA'
export ORACLE_DB_PASSWORD='valor-proporcionado-de-forma-segura'
export ORACLE_POOL_SIZE='6'
```

No se debe guardar la contraseña, el Wallet ni una URL sensible dentro del proyecto.

## Creacion del esquema Oracle

Los scripts normales son no destructivos y se ejecutan una sola vez, en este orden:

```text
src/main/resources/db/oracle/001_batch_metadata.sql
src/main/resources/db/oracle/002_business_tables.sql
src/main/resources/db/oracle/003_constraints_indexes.sql
src/main/resources/db/oracle/004_validation_queries.sql
```

El esquema de negocio queda identificado como version `2.1.0`. Antes de ejecutarlos se debe confirmar que las tablas no existen y contar con autorizacion para crear objetos en el esquema.

`scripts/oracle/reset_test_data.sql` es destructivo, se encuentra separado y nunca se ejecuta automaticamente. Requiere autorizacion explicita.

## Ejecucion de los Jobs

Primero compilar:

```bash
sh mvnw package
```

Cada ejecucion selecciona el Job y el CSV mediante argumentos, sin editar archivos de propiedades. `RunIdIncrementer` genera automaticamente el siguiente `run.id` al iniciar una nueva ejecucion desde la aplicacion CLI.

### Transacciones

```bash
java -jar target/formativa1-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=oracle \
  --spring.batch.job.name=transaccionesJob \
  --app.input-file=classpath:transacciones.csv
```

### Intereses

```bash
java -jar target/formativa1-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=oracle \
  --spring.batch.job.name=interesesJob \
  --app.input-file=classpath:intereses.csv
```

### Estados de cuenta

```bash
java -jar target/formativa1-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=oracle \
  --spring.batch.job.name=estadosCuentaJob \
  --app.input-file=classpath:cuentas_anuales.csv
```

## Retry, restart y reprocesamiento

- **Retry:** vuelve a intentar una operacion transitoria dentro del mismo Step. El limite configurado es de tres reintentos con 500 ms de espera.
- **Restart:** relanza la misma instancia, con exactamente los mismos parametros, despues de un fallo. Spring Batch recupera el checkpoint guardado en el `JobRepository`.
- **Nueva ejecucion o reprocesamiento:** usa un `run.id` nuevo. Los writers actualizan o insertan por clave, por lo que repetir el mismo archivo mantiene el mismo estado final.

Claves de idempotencia:

- `TRANSACCIONES_PROCESADAS.ID`;
- `INTERESES_CALCULADOS.CUENTA_ID`;
- `MOVIMIENTOS_ANUALES.SOURCE_KEY`, SHA-256 de la linea de origen y los campos normalizados.

La linea de origen forma parte de la identidad para conservar dos movimientos con campos identicos que aparezcan en posiciones distintas del CSV. Reprocesar el mismo archivo mantiene las mismas claves.

## Tratamiento de errores

- Un error de formato del CSV produce `FlatFileParseException`, se registra con numero de linea y puede omitirse hasta el limite.
- Una anomalia de negocio legible se conserva con estado `ANOMALIA` y su observacion.
- Los errores de infraestructura o programacion no son omitidos.
- Los procesadores validan campos nulos antes de normalizarlos.

## Resultados esperados de los CSV entregados

| Job | Leidos | Escritos | Omitidos | Anomalias persistidas |
|---|---:|---:|---:|---:|
| transaccionesJob | 8 | 8 | 2 | 2 |
| interesesJob | 6 | 6 | 2 | 1 |
| estadosCuentaJob | 8 | 8 | 1 | 1 |

Resultados principales:

- Transacciones: 8 detalles y 7 fechas resumidas.
- Intereses: 6 cuentas; saldo final de la cuenta 101 = `5050.00` y de la 102 = `8160.00`.
- Estados anuales: 8 movimientos y 7 cuentas resumidas; cuenta 101 con ingresos `1000`, egresos `500`, saldo neto `500` y 2 movimientos.

## Pruebas automatizadas

```bash
sh mvnw test
```

El conjunto cubre:

- contexto aislado con H2;
- procesadores y valores nulos;
- skip counts y numeros de linea de los tres CSV;
- retry recuperado en el tercer intento y retry agotado;
- decision final con y sin skips;
- los tres Jobs y sus conteos;
- doble ejecucion sin duplicados;
- dataset de 15 registros observado en tres threads;
- limite de 10 skips;
- restart de la misma instancia desde un checkpoint despues del primer commit.
- retry y restart inyectando fallos en `transaccionesJob` con sus tres hilos reales.

La evidencia local comprobada se registra en `EVIDENCIAS_EJECUCION.md`.

## Logs y verificacion

Cada ejecucion registra nombre del Job y Step, hilo, conteos read/write/filter/skip, commits, rollbacks, duracion, reintentos y estado final. El archivo local predeterminado es:

```text
logs/formativa1.log
```

Las consultas de verificacion Oracle se encuentran en `004_validation_queries.sql`.

## Empaquetado seguro

Para crear el ZIP final sin incluir Wallet, credenciales, logs, compilados ni capturas obsoletas:

```bash
./scripts/package_delivery.sh Exp1_S2_GrupoX
```

El script usa una lista explicita de archivos permitidos y se niega a sobrescribir una entrega existente. Se debe reemplazar `GrupoX` por el nombre real del grupo.

## Estado de Oracle

El 22 de agosto de 2026 se aplico y valido el esquema `2.1.0` en Oracle mediante el Wallet proporcionado. La verificacion confirmo:

- 12 tablas objetivo: seis de metadatos Spring Batch, cinco de negocio y `APP_SCHEMA_VERSION`;
- tres secuencias oficiales de Spring Batch;
- columna obligatoria `MOVIMIENTOS_ANUALES.SOURCE_LINE`;
- tres Jobs ejecutados al menos dos veces, sin ejecuciones fallidas;
- 8 transacciones, 6 intereses, 8 movimientos, 7 resumenes diarios y 7 estados de cuenta.

Las credenciales y el contenido del Wallet no se incorporaron a los archivos del proyecto. Las pruebas locales continúan siendo independientes de Oracle.

## Evidencia visual anterior

Las imagenes existentes en `Markdown/` corresponden a una version anterior y estan marcadas como obsoletas. No deben usarse como evidencia de esta version.

Git, GitHub, commits, ramas y empaquetado final permanecen fuera de esta etapa.
