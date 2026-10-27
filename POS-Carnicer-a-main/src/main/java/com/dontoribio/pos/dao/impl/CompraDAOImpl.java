package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.CompraDAO;
import com.dontoribio.pos.model.Compra;
import com.dontoribio.pos.model.DetalleCompra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Implementacion JDBC de {@link CompraDAO}. Inserta cabecera y detalle e incrementa
 * el stock de cada producto dentro de una unica transaccion (commit/rollback manual).
 * Esquema esperado: compras, detalle_compras (FK compra_id), productos.stock_disponible.
 */
public class CompraDAOImpl implements CompraDAO {

    private static final Logger LOG = LoggerFactory.getLogger(CompraDAOImpl.class);

    @Override
    public Compra registrarCompra(Compra compra) {
        String sqlCompra = "INSERT INTO compras (fecha, proveedor_id, usuario_id, total) " +
                "VALUES (?, ?, ?, ?) RETURNING id";
        String sqlDetalle = "INSERT INTO detalle_compras " +
                "(compra_id, producto_id, peso_kg, costo_unitario, importe, lote, fecha_produccion, fecha_vencimiento) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE productos SET stock_disponible = stock_disponible + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime fecha = compra.getFecha() != null ? compra.getFecha() : LocalDateTime.now();

                try (PreparedStatement ps = conn.prepareStatement(sqlCompra)) {
                    ps.setTimestamp(1, Timestamp.valueOf(fecha));
                    ps.setLong(2, compra.getProveedor().getId());
                    ps.setLong(3, compra.getUsuario().getId());
                    ps.setBigDecimal(4, compra.getTotal());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            compra.setId(rs.getLong("id"));
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    for (DetalleCompra detalle : compra.getDetalles()) {
                        ps.setLong(1, compra.getId());
                        ps.setLong(2, detalle.getProducto().getId());
                        ps.setBigDecimal(3, detalle.getPesoKg());
                        ps.setBigDecimal(4, detalle.getCostoUnitario());
                        ps.setBigDecimal(5, detalle.getImporte());
                        ps.setString(6, detalle.getLote());
                        ps.setDate(7, detalle.getFechaProduccion() == null ? null : Date.valueOf(detalle.getFechaProduccion()));
                        ps.setDate(8, detalle.getFechaVencimiento() == null ? null : Date.valueOf(detalle.getFechaVencimiento()));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                    for (DetalleCompra detalle : compra.getDetalles()) {
                        ps.setBigDecimal(1, detalle.getPesoKg());
                        ps.setLong(2, detalle.getProducto().getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                compra.setFecha(fecha);
                return compra;
            } catch (SQLException e) {
                conn.rollback();
                LOG.error("Error al registrar compra, se revirtio la transaccion", e);
                throw new RuntimeException("Error al registrar compra, se revirtio la transaccion", e);
            }
        } catch (SQLException e) {
            LOG.error("Error de conexion al registrar compra", e);
            throw new RuntimeException("Error de conexion al registrar compra", e);
        }
    }
}
