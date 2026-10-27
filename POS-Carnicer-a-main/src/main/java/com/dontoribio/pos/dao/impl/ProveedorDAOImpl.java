package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.ProveedorDAO;
import com.dontoribio.pos.model.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC de {@link ProveedorDAO} contra la tabla `proveedores`
 * (esquema esperado: id, nombre_razon_social, documento, telefono).
 */
public class ProveedorDAOImpl implements ProveedorDAO {

    @Override
    public Optional<Proveedor> buscarPorId(Long id) {
        String sql = "SELECT id, nombre_razon_social, documento, telefono FROM proveedores WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar proveedor por id", e);
        }
    }

    @Override
    public List<Proveedor> listarTodos() {
        String sql = "SELECT id, nombre_razon_social, documento, telefono FROM proveedores ORDER BY nombre_razon_social";
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                proveedores.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar proveedores", e);
        }
        return proveedores;
    }

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        String sql = "INSERT INTO proveedores (nombre_razon_social, documento, telefono) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proveedor.getNombreRazonSocial());
            ps.setString(2, proveedor.getDocumento());
            ps.setString(3, proveedor.getTelefono());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    proveedor.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar proveedor", e);
        }
        return proveedor;
    }

    @Override
    public void actualizar(Proveedor proveedor) {
        String sql = "UPDATE proveedores SET nombre_razon_social = ?, documento = ?, telefono = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proveedor.getNombreRazonSocial());
            ps.setString(2, proveedor.getDocumento());
            ps.setString(3, proveedor.getTelefono());
            ps.setLong(4, proveedor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar proveedor", e);
        }
    }

    @Override
    public void eliminar(Long id) {
        String sql = "DELETE FROM proveedores WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar proveedor", e);
        }
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(rs.getLong("id"));
        proveedor.setNombreRazonSocial(rs.getString("nombre_razon_social"));
        proveedor.setDocumento(rs.getString("documento"));
        proveedor.setTelefono(rs.getString("telefono"));
        return proveedor;
    }
}
