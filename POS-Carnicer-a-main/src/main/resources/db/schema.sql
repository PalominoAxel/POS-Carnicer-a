-- Esquema minimo requerido por los DAO JDBC del MVP.
-- Ejecutar contra la base "dontoribio_pos" (ver application.properties).

CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(60) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    rol             VARCHAR(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'CAJERO')),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGSERIAL PRIMARY KEY,
    tipo_cliente        VARCHAR(20) NOT NULL CHECK (tipo_cliente IN ('MINORISTA', 'MAYORISTA')),
    nombre_razon_social VARCHAR(150) NOT NULL,
    documento           VARCHAR(20) UNIQUE,
    telefono            VARCHAR(20),
    direccion           VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS productos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(30) UNIQUE NOT NULL,
    nombre              VARCHAR(150) NOT NULL,
    imagen_path         VARCHAR(255),
    precio_kg_minorista NUMERIC(10, 2) NOT NULL,
    precio_kg_mayorista NUMERIC(10, 2) NOT NULL,
    unidad_medida       VARCHAR(10) NOT NULL DEFAULT 'kg',
    stock_disponible    NUMERIC(10, 3) NOT NULL DEFAULT 0 CHECK (stock_disponible >= 0),
    activo              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS proveedores (
    id                  BIGSERIAL PRIMARY KEY,
    nombre_razon_social VARCHAR(150) NOT NULL,
    documento           VARCHAR(20) UNIQUE,
    telefono            VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS ventas (
    id                  BIGSERIAL PRIMARY KEY,
    fecha               TIMESTAMP NOT NULL DEFAULT NOW(),
    cliente_id          BIGINT REFERENCES clientes (id),
    usuario_id          BIGINT NOT NULL REFERENCES usuarios (id),
    subtotal            NUMERIC(10, 2) NOT NULL,
    total               NUMERIC(10, 2) NOT NULL,
    tipo_comprobante    VARCHAR(20) NOT NULL DEFAULT 'TICKET_SIMPLE'
                        CHECK (tipo_comprobante IN ('TICKET_SIMPLE', 'BOLETA', 'FACTURA')),
    documento_cliente   VARCHAR(11)
);

-- Si la tabla ya existia de una version anterior, se agregan las columnas de comprobante.
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tipo_comprobante VARCHAR(20) NOT NULL DEFAULT 'TICKET_SIMPLE';
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS documento_cliente VARCHAR(11);

CREATE TABLE IF NOT EXISTS detalle_ventas (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES ventas (id) ON DELETE CASCADE,
    producto_id     BIGINT NOT NULL REFERENCES productos (id),
    peso_kg         NUMERIC(10, 3) NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    importe         NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS pagos (
    id          BIGSERIAL PRIMARY KEY,
    venta_id    BIGINT NOT NULL REFERENCES ventas (id) ON DELETE CASCADE,
    metodo_pago VARCHAR(20) NOT NULL CHECK (metodo_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA', 'TRANSFERENCIA')),
    monto       NUMERIC(10, 2) NOT NULL,
    referencia  VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS compras (
    id           BIGSERIAL PRIMARY KEY,
    fecha        TIMESTAMP NOT NULL DEFAULT NOW(),
    proveedor_id BIGINT NOT NULL REFERENCES proveedores (id),
    usuario_id   BIGINT NOT NULL REFERENCES usuarios (id),
    total        NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS detalle_compras (
    id                  BIGSERIAL PRIMARY KEY,
    compra_id           BIGINT NOT NULL REFERENCES compras (id) ON DELETE CASCADE,
    producto_id         BIGINT NOT NULL REFERENCES productos (id),
    peso_kg             NUMERIC(10, 3) NOT NULL,
    costo_unitario       NUMERIC(10, 2) NOT NULL,
    importe              NUMERIC(10, 2) NOT NULL,
    -- Trazabilidad opcional del lote recibido: el usuario los completa solo si el proveedor los brinda.
    lote                 VARCHAR(50),
    fecha_produccion     DATE,
    fecha_vencimiento    DATE
);

-- Si la tabla ya existia de una version anterior, se agregan las columnas de trazabilidad.
ALTER TABLE detalle_compras ADD COLUMN IF NOT EXISTS lote VARCHAR(50);
ALTER TABLE detalle_compras ADD COLUMN IF NOT EXISTS fecha_produccion DATE;
ALTER TABLE detalle_compras ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE;

CREATE TABLE IF NOT EXISTS caja (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios (id),
    fecha_apertura  TIMESTAMP NOT NULL DEFAULT NOW(),
    monto_inicial   NUMERIC(10, 2) NOT NULL,
    fecha_cierre    TIMESTAMP,
    monto_final     NUMERIC(10, 2)
);

-- password_hash es el hash BCrypt de 'Palomino12/' (nunca texto plano; PasswordUtil.verificarPassword lo rechazaria).
INSERT INTO usuarios(username, password_hash, nombre_completo, rol, activo) VALUES ('DonToribio', '$2a$12$dDyUgMArMa6TFVPcJjuxnOCc6HxiVkorQzXAyXiTxEPZYPszCKgnm', 'Administrador', 'ADMINISTRADOR', true);