package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.ProductoDAO;
import com.dontoribio.pos.model.Producto;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC de {@link ProductoDAO} contra la tabla `productos`
 * (esquema esperado: id, codigo, nombre, precio_kg_minorista, precio_kg_mayorista,
 * unidad_medida, stock_disponible, activo).
 */
public class ProductoDAOImpl implements ProductoDAO {

    /**
     * Subconsulta que trae, por producto, el lote/fecha de produccion/fecha de vencimiento
     * de la compra mas reciente registrada (dato informativo de solo lectura; se captura en
     * el modulo de Compras, no aqui).
     */
    private static final String LOTE_ACTUAL =
            "(SELECT dc.lote FROM detalle_compras dc JOIN compras c ON c.id = dc.compra_id " +
            "WHERE dc.producto_id = productos.id ORDER BY c.fecha DESC, dc.id DESC LIMIT 1) AS lote_actual, " +
            "(SELECT dc.fecha_produccion FROM detalle_compras dc JOIN compras c ON c.id = dc.compra_id " +
            "WHERE dc.producto_id = productos.id ORDER BY c.fecha DESC, dc.id DESC LIMIT 1) AS fecha_produccion_actual, " +
            "(SELECT dc.fecha_vencimiento FROM detalle_compras dc JOIN compras c ON c.id = dc.compra_id " +
            "WHERE dc.producto_id = productos.id ORDER BY c.fecha DESC, dc.id DESC LIMIT 1) AS fecha_vencimiento_actual";

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por id", e);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por codigo", e);
        }
    }

    @Override
    public List<Producto> listarActivos() {
        String sql = "SELECT productos.*, " + LOTE_ACTUAL + " FROM productos WHERE activo = TRUE ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearConLote(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos activos", e);
        }
        return productos;
    }

    @Override
    public List<Producto> listarTodos() {
        String sql = "SELECT productos.*, " + LOTE_ACTUAL + " FROM productos ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearConLote(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos", e);
        }
        return productos;
    }

    @Override
    public Producto guardar(Producto producto) {
        String sql = "INSERT INTO productos (codigo, nombre, imagen_path, precio_kg_minorista, precio_kg_mayorista, " +
                "unidad_medida, stock_disponible, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getImagenPath());
            ps.setBigDecimal(4, producto.getPrecioKgMinorista());
            ps.setBigDecimal(5, producto.getPrecioKgMayorista());
            ps.setString(6, producto.getUnidadMedida());
            ps.setBigDecimal(7, producto.getStockDisponible());
            ps.setBoolean(8, producto.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    producto.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar producto", e);
        }
        return producto;
    }

    @Override
    public void actualizar(Producto producto) {
        String sql = "UPDATE productos SET codigo = ?, nombre = ?, imagen_path = ?, precio_kg_minorista = ?, " +
                "precio_kg_mayorista = ?, unidad_medida = ?, stock_disponible = ?, activo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getImagenPath());
            ps.setBigDecimal(4, producto.getPrecioKgMinorista());
            ps.setBigDecimal(5, producto.getPrecioKgMayorista());
            ps.setString(6, producto.getUnidadMedida());
            ps.setBigDecimal(7, producto.getStockDisponible());
            ps.setBoolean(8, producto.isActivo());
            ps.setLong(9, producto.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto", e);
        }
    }

    @Override
    public void eliminar(Long id) {
        String sql = "DELETE FROM productos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar producto", e);
        }
    }

    @Override
    public boolean actualizarTrazabilidad(Long productoId, String lote, LocalDate fechaProduccion, LocalDate fechaVencimiento) {
        String sql = "UPDATE detalle_compras SET lote = ?, fecha_produccion = ?, fecha_vencimiento = ? " +
                "WHERE id = (SELECT dc.id FROM detalle_compras dc JOIN compras c ON c.id = dc.compra_id " +
                "WHERE dc.producto_id = ? ORDER BY c.fecha DESC, dc.id DESC LIMIT 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lote);
            ps.setDate(2, fechaProduccion == null ? null : Date.valueOf(fechaProduccion));
            ps.setDate(3, fechaVencimiento == null ? null : Date.valueOf(fechaVencimiento));
            ps.setLong(4, productoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar trazabilidad del producto", e);
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setId(rs.getLong("id"));
        producto.setCodigo(rs.getString("codigo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setImagenPath(rs.getString("imagen_path"));
        producto.setPrecioKgMinorista(rs.getBigDecimal("precio_kg_minorista"));
        producto.setPrecioKgMayorista(rs.getBigDecimal("precio_kg_mayorista"));
        producto.setUnidadMedida(rs.getString("unidad_medida"));
        producto.setStockDisponible(rs.getBigDecimal("stock_disponible"));
        producto.setActivo(rs.getBoolean("activo"));
        return producto;
    }

    private Producto mapearConLote(ResultSet rs) throws SQLException {
        Producto producto = mapear(rs);
        producto.setLoteActual(rs.getString("lote_actual"));
        java.sql.Date fechaProduccion = rs.getDate("fecha_produccion_actual");
        producto.setFechaProduccionActual(fechaProduccion == null ? null : fechaProduccion.toLocalDate());
        java.sql.Date fechaVencimiento = rs.getDate("fecha_vencimiento_actual");
        producto.setFechaVencimientoActual(fechaVencimiento == null ? null : fechaVencimiento.toLocalDate());
        return producto;
    }
}
