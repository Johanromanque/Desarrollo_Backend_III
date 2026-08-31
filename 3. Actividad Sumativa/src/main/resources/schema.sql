-- =====================================================
-- SCRIPT DE CREACION DE TABLAS
-- BANCO XYZ - SPRING BATCH
--
-- Este archivo crea únicamente las tablas utilizadas
-- por los tres procesos del Banco XYZ.
--
-- Las tablas BATCH_* son administradas por Spring Batch.
--
-- IMPORTANTE:
-- Ejecutar manualmente solo cuando se necesite
-- reinicializar las tablas de la aplicación.
-- Los DROP TABLE eliminan los datos existentes.
-- =====================================================


-- =====================================================
-- LIMPIEZA DE TABLAS DE LA APLICACION
-- =====================================================

DROP TABLE estados_cuenta_anuales PURGE;
DROP TABLE movimientos_anuales PURGE;
DROP TABLE intereses_calculados PURGE;
DROP TABLE resumen_transacciones_diarias PURGE;
DROP TABLE transacciones_procesadas PURGE;


-- =====================================================
-- JOB 1: TRANSACCIONES DIARIAS
-- =====================================================

CREATE TABLE transacciones_procesadas (
    id NUMBER(19) PRIMARY KEY,
    fecha DATE NOT NULL,
    monto NUMBER(15,2) NOT NULL,
    tipo VARCHAR2(20) NOT NULL,
    estado VARCHAR2(30) NOT NULL,
    observacion VARCHAR2(250)
);


CREATE TABLE resumen_transacciones_diarias (
    fecha DATE PRIMARY KEY,
    total_transacciones NUMBER(10) DEFAULT 0,
    total_creditos NUMBER(10) DEFAULT 0,
    total_debitos NUMBER(10) DEFAULT 0,
    total_anomalias NUMBER(10) DEFAULT 0,
    monto_creditos NUMBER(15,2) DEFAULT 0,
    monto_debitos NUMBER(15,2) DEFAULT 0
);


-- =====================================================
-- JOB 2: CALCULO DE INTERESES
-- =====================================================

CREATE TABLE intereses_calculados (
    id_calculo NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuenta_id NUMBER(19) NOT NULL,
    nombre VARCHAR2(150) NOT NULL,
    saldo_inicial NUMBER(15,2),
    edad NUMBER(3),
    tipo VARCHAR2(30) NOT NULL,
    tasa_interes NUMBER(8,4),
    interes_calculado NUMBER(15,2),
    saldo_final NUMBER(15,2),
    estado VARCHAR2(30) NOT NULL,
    observacion VARCHAR2(250)
);

-- =====================================================
-- JOB 3: ESTADOS DE CUENTA ANUALES
-- =====================================================

CREATE TABLE movimientos_anuales (
    id_movimiento NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuenta_id NUMBER(19) NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR2(30) NOT NULL,
    monto NUMBER(15,2) NOT NULL,
    descripcion VARCHAR2(250),
    estado VARCHAR2(30) NOT NULL,
    observacion VARCHAR2(250)
);


CREATE TABLE estados_cuenta_anuales (
    cuenta_id NUMBER(19) PRIMARY KEY,
    total_ingresos NUMBER(15,2) DEFAULT 0,
    total_egresos NUMBER(15,2) DEFAULT 0,
    saldo_neto NUMBER(15,2) DEFAULT 0,
    cantidad_movimientos NUMBER(10) DEFAULT 0,
    cantidad_anomalias NUMBER(10) DEFAULT 0
);