# Evidencias de ejecución y corrección

Fecha de verificación: 22 de agosto de 2026  
Estado: copia de trabajo local corregida y validada en H2 y Oracle; publicación en Git pendiente.

## Configuración verificada

- Spring Batch con tres jobs independientes.
- Tamaño de chunk: `5`.
- Ejecutor paralelo: `core-pool-size=3`, `max-pool-size=3`, `queue-capacity=15`.
- Perfil de pruebas: H2 en memoria, pool de 6 conexiones y creación automática de metadatos de Spring Batch.
- Perfil Oracle: credenciales y URL leídas exclusivamente desde variables de entorno.
- Logs de aplicación en consola y en `logs/formativa1.log`.

## Resultado automatizado

Comando ejecutado:

```text
sh mvnw test
```

Resultado:

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 7.101 s
Finished at: 2026-08-22T16:44:31-04:00
```

La generación final del ejecutable también terminó correctamente:

```text
sh mvnw -DskipTests package
BUILD SUCCESS
Artefacto: target/formativa1-0.0.1-SNAPSHOT.jar
Tamaño aproximado: 26 MB
```

Distribución de pruebas:

| Suite | Pruebas |
|---|---:|
| `CsvReaderTests` | 3 |
| `Formativa1ApplicationTests` | 1 |
| `JobIntegrationTests` | 4 |
| `ParallelProcessingTests` | 1 |
| `ProcessorAndPolicyTests` | 5 |
| `ProductionResilienceIntegrationTests` | 2 |
| `RestartIntegrationTests` | 1 |
| `RetryBehaviorTests` | 3 |
| `SkipLimitIntegrationTests` | 1 |
| **Total** | **21** |

## Resultados funcionales

| Job | Leídos | Escritos | Saltados al leer | Resultado persistido |
|---|---:|---:|---:|---|
| Transacciones | 8 | 8 | 2 | 8 detalles y 7 resúmenes diarios |
| Intereses | 6 | 6 | 2 | 6 cuentas; cuenta 101 con saldo final 5050,00 |
| Movimientos anuales | 8 | 8 | 1 | 8 movimientos y 7 resúmenes; cuenta 101 con neto 500 y 2 movimientos |

Cada job se ejecutó dos veces con parámetros distintos. La segunda ejecución no duplicó información de negocio, lo que comprueba el reproceso idempotente.

## Paralelismo

La prueba con 15 registros confirmó procesamiento efectivo en exactamente tres hilos de trabajo:

```text
Batch-Thread-1
Batch-Thread-2
Batch-Thread-3
```

## Reintentos, saltos y reinicio

- Reintento: un fallo transitorio se recuperó en el tercer intento y produjo una sola escritura.
- Agotamiento de reintentos: se verificaron 3 reintentos después del intento inicial, con 4 intentos totales.
- Límite de saltos: un archivo con 11 filas inválidas provocó el fallo esperado al superar el límite configurado de 10.
- Reinicio real: la primera ejecución falló tras confirmar el primer chunk de 5 elementos. Al reiniciar la misma instancia con los mismos parámetros, procesó los 7 restantes y terminó con 12 filas, una sola `JobInstance` y dos `JobExecution`.
- Resiliencia del Job productivo: `transaccionesJob`, manteniendo sus tres hilos, recuperó dos fallos transitorios del writer. En otra prueba confirmó 5 registros, falló, reinició la misma instancia y procesó solamente los 10 restantes.

## Seguridad y scripts SQL

- No quedan contraseñas, alias privados del servicio ni rutas locales del Wallet dentro de la configuración versionable.
- Se eliminaron impresiones directas por consola y se reemplazaron por logging estructurado.
- Los scripts normales de instalación Oracle son no destructivos y están separados por responsabilidad.
- Las operaciones de limpieza se aislaron en `scripts/oracle/reset_test_data.sql`, con advertencia explícita.
- Los logs omiten el contenido de las filas CSV, nombres, cuentas y montos.
- El empaquetador seguro se probó con 92 entradas y no incluyó Wallet, certificados, configuración TNS, logs, compilados ni Git.

## Validación Oracle

El esquema se instaló de forma no destructiva y luego se ejecutaron los tres Jobs dos veces. La validación final informó:

```text
oracle_connection=PASS
target_tables_after=12
batch_sequences=3
source_line_column=1
schema_version_2_1=1
rows_transacciones=8
rows_intereses=6
rows_movimientos=8
rows_resumen_transacciones=7
rows_estados_cuenta=7
completed_job_executions=7
failed_job_executions=0
oracle_schema_validation=PASS
```

La ejecución adicional de transacciones corresponde a la validación inicial posterior a la instalación. Después se realizaron dos rondas completas de los tres Jobs y los conteos de negocio permanecieron estables.

## Pendientes de cierre

1. Las salidas de consola anteriores constituyen la evidencia reproducible solicitada. Las capturas son opcionales si el docente exige específicamente formato de imagen; las antiguas continúan excluidas.
2. Crear rama, commit o publicación en Git cuando se autorice la etapa final.
