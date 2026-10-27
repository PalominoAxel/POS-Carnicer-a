package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.CompraDAO;
import com.dontoribio.pos.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC de {@link CompraDAO}. Inserta cabecera y detalle e incrementa
 * el stock de cada producto dentro de una unica transaccion (commit/rollback manual).
 * Esquema esperado: compras, detalle_compras (FK compra_id), productos.stock_disponible.
 */
public class CompraDAOImpl implements CompraDAO {

    private static final Logger LOG = LoggerFactory.getLogger(CompraDAOImpl.class);

    @Override
    public Compra registrarCompra(Compra compra) {
        // RETURNING no es portable entre motores (H2 no lo soporta); el id generado se
        // recupera con getGeneratedKeys(), que funciona igual en H2 y en Postgres.
        String sqlCompra = "INSERT INTO compras (fecha, proveedor_id, usuario_id, total) " +
                "VALUES (?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_compras " +
                "(compra_id, producto_id, peso_kg, costo_unitario, importe, lote, fecha_produccion, fecha_vencimiento) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE productos SET stock_disponible = stock_disponible + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime fecha = compra.getFecha() != null ? compra.getFecha() : LocalDateTime.now();

                try (PreparedStatement ps = conn.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setTimestamp(1, Timestamp.valueOf(fecha));
                    ps.setLong(2, compra.getProveedor().getId());
                    ps.setLong(3, compra.getUsuario().getId());
                    ps.setBigDecimal(4, compra.getTotal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            compra.setId(rs.getLong(1));
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

    private static final String SQL_CABECERA =
            "SELECT c.id, c.fecha, c.total, " +
            "pr.id AS proveedor_id, pr.nombre_razon_social AS proveedor_nombre, pr.documento AS proveedor_documento, " +
            "pr.telefono AS proveedor_telefono, " +
            "u.id AS usuario_id, u.nombre_completo AS usuario_nombre, u.rol AS usuario_rol " +
            "FROM compras c " +
            "JOIN proveedores pr ON pr.id = c.proveedor_id " +
            "JOIN usuarios u ON u.id = c.usuario_id ";

    @Override
    public List<Compra> listarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        String sql = SQL_CABECERA + "WHERE CAST(c.fecha AS DATE) BETWEEN ? AND ? ORDER BY c.fecha DESC";
        List<Compra> compras = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    compras.add(mapearCabecera(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar compras por rango de fechas", e);
        }
        return compras;
    }

    @Override
    public List<Compra> listarPorProveedor(String nombreProveedor) {
        String sql = SQL_CABECERA + "WHERE LOWER(pr.nombre_razon_social) LIKE LOWER(?) ORDER BY c.fecha DESC";
        List<Compra> compras = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nombreProveedor + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    compras.add(mapearCabecera(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar compras por proveedor", e);
        }
        return compras;
    }

    @Override
    public Optional<Compra> buscarCompraCompleta(Long id) {
        String sqlCompra = SQL_CABECERA + "WHERE c.id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            Compra compra;
            try (PreparedStatement ps = conn.prepareStatement(sqlCompra)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    compra = mapearCabecera(rs);
                }
            }

            String sqlDetalle = "SELECT dc.peso_kg, dc.costo_unitario, dc.importe, dc.lote, " +
                    "dc.fecha_produccion, dc.fecha_vencimiento, " +
                    "p.id AS producto_id, p.codigo, p.nombre, p.unidad_medida " +
                    "FROM detalle_compras dc JOIN productos p ON p.id = dc.producto_id " +
                    "WHERE dc.compra_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Producto producto = new Producto();
                        producto.setId(rs.getLong("producto_id"));
                        producto.setCodigo(rs.getString("codigo"));
                        producto.setNombre(rs.getString("nombre"));
                        producto.setUnidadMedida(rs.getString("unidad_medida"));

                        Date fechaProduccion = rs.getDate("fecha_produccion");
                        Date fechaVencimiento = rs.getDate("fecha_vencimiento");
                        DetalleCompra detalle = new DetalleCompra(producto, rs.getBigDecimal("peso_kg"),
                                rs.getBigDecimal("costo_unitario"), rs.getString("lote"),
                                fechaProduccion == null ? null : fechaProduccion.toLocalDate(),
                                fechaVencimiento == null ? null : fechaVencimiento.toLocalDate());
                        detalle.setImporte(rs.getBigDecimal("importe"));
                        compra.agregarDetalle(detalle);
                    }
                }
            }

            return Optional.of(compra);
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar la compra completa", e);
        }
    }

    private Compra mapearCabecera(ResultSet rs) throws SQLException {
        Compra compra = new Compra();
        compra.setId(rs.getLong("id"));
        compra.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        compra.setTotalPersistido(rs.getBigDecimal("total"));

        Proveedor proveedor = new Proveedor();
        proveedor.setId(rs.getLong("proveedor_id"));
        proveedor.setNombreRazonSocial(rs.getString("proveedor_nombre"));
        proveedor.setDocumento(rs.getString("proveedor_documento"));
        proveedor.setTelefono(rs.getString("proveedor_telefono"));
        compra.setProveedor(proveedor);

        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("usuario_id"));
        usuario.setNombreCompleto(rs.getString("usuario_nombre"));
        String rol = rs.getString("usuario_rol");
        usuario.setRol(rol == null ? null : Rol.valueOf(rol));
        compra.setUsuario(usuario);

        return compra;
    }
}
