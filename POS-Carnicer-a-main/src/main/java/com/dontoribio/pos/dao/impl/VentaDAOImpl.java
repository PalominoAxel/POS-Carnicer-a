package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.VentaDAO;
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
 * Implementacion JDBC de {@link VentaDAO}. Inserta cabecera, detalles y pagos
 * dentro de una unica transaccion (commit/rollback manual sobre la conexion).
 * Esquema esperado: ventas, detalle_ventas, pagos (FK venta_id).
 */
public class VentaDAOImpl implements VentaDAO {

    private static final Logger LOG = LoggerFactory.getLogger(VentaDAOImpl.class);

    @Override
    public Venta registrarVenta(Venta venta) {
        String sqlVenta = "INSERT INTO ventas (fecha, cliente_id, usuario_id, subtotal, total, tipo_comprobante, documento_cliente) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String sqlDetalle = "INSERT INTO detalle_ventas (venta_id, producto_id, peso_kg, precio_unitario, importe) " +
                "VALUES (?, ?, ?, ?, ?)";
        String sqlPago = "INSERT INTO pagos (venta_id, metodo_pago, monto, referencia) VALUES (?, ?, ?, ?)";
        String sqlStock = "UPDATE productos SET stock_disponible = stock_disponible - ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime fecha = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();

                try (PreparedStatement ps = conn.prepareStatement(sqlVenta)) {
                    ps.setTimestamp(1, Timestamp.valueOf(fecha));
                    ps.setLong(2, venta.getCliente().getId());
                    ps.setLong(3, venta.getUsuario().getId());
                    ps.setBigDecimal(4, venta.getSubtotal());
                    ps.setBigDecimal(5, venta.getTotal());
                    ps.setString(6, venta.getTipoComprobante() == null
                            ? TipoComprobante.TICKET_SIMPLE.name() : venta.getTipoComprobante().name());
                    ps.setString(7, venta.getDocumentoCliente());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            venta.setId(rs.getLong("id"));
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        ps.setLong(1, venta.getId());
                        ps.setLong(2, detalle.getProducto().getId());
                        ps.setBigDecimal(3, detalle.getPesoKg());
                        ps.setBigDecimal(4, detalle.getPrecioUnitario());
                        ps.setBigDecimal(5, detalle.getImporte());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlPago)) {
                    for (Pago pago : venta.getPagos()) {
                        ps.setLong(1, venta.getId());
                        ps.setString(2, pago.getMetodoPago().name());
                        ps.setBigDecimal(3, pago.getMonto());
                        ps.setString(4, pago.getReferencia());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        ps.setBigDecimal(1, detalle.getPesoKg());
                        ps.setLong(2, detalle.getProducto().getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                venta.setFecha(fecha);
                return venta;
            } catch (SQLException e) {
                conn.rollback();
                if (e.getSQLState() != null && e.getSQLState().equals("23514")) {
                    LOG.warn("Venta rechazada por stock insuficiente (usuario={}, cliente={})",
                            venta.getUsuario() == null ? null : venta.getUsuario().getId(),
                            venta.getCliente() == null ? null : venta.getCliente().getId());
                    throw new IllegalStateException("Stock insuficiente para completar la venta", e);
                }
                LOG.error("Error al registrar venta, se revirtio la transaccion", e);
                throw new RuntimeException("Error al registrar venta, se revirtio la transaccion", e);
            }
        } catch (SQLException e) {
            LOG.error("Error de conexion al registrar venta", e);
            throw new RuntimeException("Error de conexion al registrar venta", e);
        }
    }

    @Override
    public Optional<Venta> buscarPorId(Long id) {
        String sql = "SELECT id, fecha, subtotal, total FROM ventas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Venta venta = new Venta();
                venta.setId(rs.getLong("id"));
                venta.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                return Optional.of(venta);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar venta por id", e);
        }
    }

    @Override
    public List<Venta> listarPorFecha(LocalDate fecha) {
        return listarPorRangoFechas(fecha, fecha);
    }

    @Override
    public List<Venta> listarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT v.id, v.fecha, v.subtotal, v.total, v.tipo_comprobante, v.documento_cliente, " +
                "c.id AS cliente_id, c.nombre_razon_social AS cliente_nombre, c.tipo_cliente, " +
                "u.id AS usuario_id, u.nombre_completo AS usuario_nombre, u.rol AS usuario_rol " +
                "FROM ventas v " +
                "JOIN clientes c ON c.id = v.cliente_id " +
                "JOIN usuarios u ON u.id = v.usuario_id " +
                "WHERE v.fecha::date BETWEEN ? AND ? ORDER BY v.fecha DESC";
        List<Venta> ventas = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapearCabecera(rs);
                    ventas.add(venta);
                }
            }

            String sqlPago = "SELECT metodo_pago, monto, referencia FROM pagos WHERE venta_id = ?";
            try (PreparedStatement psPago = conn.prepareStatement(sqlPago)) {
                for (Venta venta : ventas) {
                    psPago.setLong(1, venta.getId());
                    try (ResultSet rsPago = psPago.executeQuery()) {
                        List<Pago> pagos = new ArrayList<>();
                        while (rsPago.next()) {
                            pagos.add(new Pago(MetodoPago.valueOf(rsPago.getString("metodo_pago")),
                                    rsPago.getBigDecimal("monto"), rsPago.getString("referencia")));
                        }
                        venta.setPagos(pagos);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar ventas por rango de fechas", e);
        }
        return ventas;
    }

    @Override
    public Optional<Venta> buscarVentaCompleta(Long id) {
        String sqlVenta = "SELECT v.id, v.fecha, v.subtotal, v.total, v.tipo_comprobante, v.documento_cliente, " +
                "c.id AS cliente_id, c.nombre_razon_social AS cliente_nombre, c.tipo_cliente, " +
                "u.id AS usuario_id, u.nombre_completo AS usuario_nombre, u.rol AS usuario_rol " +
                "FROM ventas v " +
                "JOIN clientes c ON c.id = v.cliente_id " +
                "JOIN usuarios u ON u.id = v.usuario_id " +
                "WHERE v.id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            Venta venta;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenta)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    venta = mapearCabecera(rs);
                }
            }

            String sqlDetalle = "SELECT dv.peso_kg, dv.precio_unitario, dv.importe, " +
                    "p.id AS producto_id, p.codigo, p.nombre, p.unidad_medida " +
                    "FROM detalle_ventas dv JOIN productos p ON p.id = dv.producto_id " +
                    "WHERE dv.venta_id = ?";
            List<DetalleVenta> detalles = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Producto producto = new Producto();
                        producto.setId(rs.getLong("producto_id"));
                        producto.setCodigo(rs.getString("codigo"));
                        producto.setNombre(rs.getString("nombre"));
                        producto.setUnidadMedida(rs.getString("unidad_medida"));

                        DetalleVenta detalle = new DetalleVenta();
                        detalle.setProducto(producto);
                        detalle.setPesoKg(rs.getBigDecimal("peso_kg"));
                        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                        detalle.setImporte(rs.getBigDecimal("importe"));
                        detalles.add(detalle);
                    }
                }
            }
            venta.setDetalles(detalles);

            String sqlPago = "SELECT metodo_pago, monto, referencia FROM pagos WHERE venta_id = ?";
            List<Pago> pagos = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlPago)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        pagos.add(new Pago(MetodoPago.valueOf(rs.getString("metodo_pago")),
                                rs.getBigDecimal("monto"), rs.getString("referencia")));
                    }
                }
            }
            venta.setPagos(pagos);

            return Optional.of(venta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar la venta completa", e);
        }
    }

    private Venta mapearCabecera(ResultSet rs) throws SQLException {
        Venta venta = new Venta();
        venta.setId(rs.getLong("id"));
        venta.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        venta.setTotalesPersistidos(rs.getBigDecimal("subtotal"), rs.getBigDecimal("total"));
        venta.setTipoComprobante(TipoComprobante.valueOf(rs.getString("tipo_comprobante")));
        venta.setDocumentoCliente(rs.getString("documento_cliente"));

        Cliente cliente = new Cliente();
        cliente.setId(rs.getLong("cliente_id"));
        cliente.setNombreRazonSocial(rs.getString("cliente_nombre"));
        String tipoCliente = rs.getString("tipo_cliente");
        cliente.setTipoCliente(tipoCliente == null ? null : TipoCliente.valueOf(tipoCliente));
        venta.setCliente(cliente);

        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("usuario_id"));
        usuario.setNombreCompleto(rs.getString("usuario_nombre"));
        String rol = rs.getString("usuario_rol");
        usuario.setRol(rol == null ? null : Rol.valueOf(rol));
        venta.setUsuario(usuario);

        return venta;
    }
}
