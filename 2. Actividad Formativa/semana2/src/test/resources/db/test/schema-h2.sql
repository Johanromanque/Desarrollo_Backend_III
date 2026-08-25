CREATE TABLE IF NOT EXISTS transacciones_procesadas (
    id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    observacion VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS resumen_transacciones_diarias (
    fecha DATE PRIMARY KEY,
    total_transacciones INT DEFAULT 0 NOT NULL,
    total_creditos INT DEFAULT 0 NOT NULL,
    total_debitos INT DEFAULT 0 NOT NULL,
    total_anomalias INT DEFAULT 0 NOT NULL,
    monto_creditos DECIMAL(15,2) DEFAULT 0 NOT NULL,
    monto_debitos DECIMAL(15,2) DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    cuenta_id BIGINT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    saldo_inicial DECIMAL(15,2) NOT NULL,
    edad INT,
    tipo VARCHAR(30) NOT NULL,
    tasa_interes DECIMAL(8,4),
    interes_calculado DECIMAL(15,2),
    saldo_final DECIMAL(15,2),
    estado VARCHAR(30) NOT NULL,
    observacion VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS movimientos_anuales (
    source_key VARCHAR(64) PRIMARY KEY,
    source_line INT NOT NULL,
    cuenta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR(30) NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    descripcion VARCHAR(250),
    estado VARCHAR(30) NOT NULL,
    observacion VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS estados_cuenta_anuales (
    cuenta_id BIGINT PRIMARY KEY,
    total_ingresos DECIMAL(15,2) DEFAULT 0 NOT NULL,
    total_egresos DECIMAL(15,2) DEFAULT 0 NOT NULL,
    saldo_neto DECIMAL(15,2) DEFAULT 0 NOT NULL,
    cantidad_movimientos INT DEFAULT 0 NOT NULL,
    cantidad_anomalias INT DEFAULT 0 NOT NULL
);
