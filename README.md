# Backend Development III

Academic repository containing backend development projects created during the Backend Development III course at Duoc UC.

The projects focus on batch processing, database integration, data validation, and backend development using Java, Spring Boot, Spring Batch, and Oracle Database.

## Technologies

- Java 17
- Spring Boot
- Spring Batch
- Spring JDBC
- Oracle Database
- Oracle Autonomous Database
- Maven
- SQL
- Git & GitHub
- Visual Studio Code
- Oracle SQL Developer

## Main Topics

- Batch processing with Spring Batch
- Chunk-oriented processing
- Job and Step configuration
- CSV file processing
- Readers, processors, and writers
- Job execution listeners
- Oracle Database integration
- Spring Batch metadata
- Data validation and processing
- Backend application configuration

## Featured Projects

### Spring Batch Loan Processing

Batch application designed to process loan records from CSV files and persist the processed information in Oracle Database.

Main concepts:

- Spring Batch Job and Step configuration
- CSV processing
- Chunk-oriented processing
- Oracle Database persistence
- Job execution monitoring

### Banking Batch Processing

Backend application that processes banking information through multiple Spring Batch jobs.

Implemented jobs include:

- `transaccionesJob`
- `interesesJob`
- `estadosCuentaJob`

The project processes transaction, interest, and account-statement information and stores the results in Oracle Autonomous Database.

## Database

Oracle Autonomous Database is used for application data and Spring Batch metadata.

Execution results can be monitored through tables such as:

```text
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_STEP_EXECUTION
