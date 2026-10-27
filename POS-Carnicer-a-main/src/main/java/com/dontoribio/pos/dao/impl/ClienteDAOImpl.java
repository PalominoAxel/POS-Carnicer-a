package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.ClienteDAO;
import com.dontoribio.pos.model.Cliente;
import com.dontoribio.pos.model.TipoCliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC de {@link ClienteDAO} contra la tabla `clientes`
 * (esquema esperado: id, tipo_cliente, nombre_razon_social, documento, telefono, direccion).
 */
public class ClienteDAOImpl implements ClienteDAO {

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        String sql = "SELECT id, tipo_cliente, nombre_razon_social, documento, telefono, direccion " +
                "FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente por id", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        String sql = "SELECT id, tipo_cliente, nombre_razon_social, documento, telefono, direccion " +
                "FROM clientes WHERE documento = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente por documento", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorNombre(String nombreRazonSocial) {
        String sql = "SELECT id, tipo_cliente, nombre_razon_social, documento, telefono, direccion " +
                "FROM clientes WHERE LOWER(nombre_razon_social) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreRazonSocial);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente por nombre", e);
        }
    }

    @Override
    public List<Cliente> listarTodos() {
        String sql = "SELECT id, tipo_cliente, nombre_razon_social, documento, telefono, direccion " +
                "FROM clientes ORDER BY nombre_razon_social";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar clientes", e);
        }
        return clientes;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        String sql = "INSERT INTO clientes (tipo_cliente, nombre_razon_social, documento, telefono, direccion) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getTipoCliente().name());
            ps.setString(2, cliente.getNombreRazonSocial());
            ps.setString(3, cliente.getDocumento());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getDireccion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cliente.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar cliente", e);
        }
        return cliente;
    }

    @Override
    public void actualizar(Cliente cliente) {
        String sql = "UPDATE clientes SET tipo_cliente = ?, nombre_razon_social = ?, documento = ?, " +
                "telefono = ?, direccion = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getTipoCliente().name());
            ps.setString(2, cliente.getNombreRazonSocial());
            ps.setString(3, cliente.getDocumento());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getDireccion());
            ps.setLong(6, cliente.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar cliente", e);
        }
    }

    @Override
    public void eliminar(Long id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar cliente", e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getLong("id"));
        cliente.setTipoCliente(TipoCliente.valueOf(rs.getString("tipo_cliente")));
        cliente.setNombreRazonSocial(rs.getString("nombre_razon_social"));
        cliente.setDocumento(rs.getString("documento"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setDireccion(rs.getString("direccion"));
        return cliente;
    }
}
