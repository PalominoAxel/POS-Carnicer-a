package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC de {@link UsuarioDAO} contra la tabla `usuarios`
 * (esquema esperado: id, username, password_hash, nombre_completo, rol, activo, fecha_creacion).
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        // Comparacion case-insensitive: evita que "Juan" y "juan" se traten como cuentas
        // distintas (tanto para encontrar la cuenta correcta al loguear como para bloquear
        // duplicados al crear usuarios).
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, activo, fecha_creacion " +
                "FROM usuarios WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por username", e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, activo, fecha_creacion " +
                "FROM usuarios ORDER BY nombre_completo";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios", e);
        }
        return usuarios;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        // fecha_creacion se fija aqui (en vez de dejarla al DEFAULT NOW() de la columna) para
        // poder leerla de vuelta sin una segunda consulta: RETURNING no es portable entre
        // motores (H2 no lo soporta), asi que el id se recupera con getGeneratedKeys().
        LocalDateTime fechaCreacion = LocalDateTime.now();
        String sql = "INSERT INTO usuarios (username, password_hash, nombre_completo, rol, activo, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getNombreCompleto());
            ps.setString(4, usuario.getRol().name());
            ps.setBoolean(5, usuario.isActivo());
            ps.setTimestamp(6, Timestamp.valueOf(fechaCreacion));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getLong(1));
                    usuario.setFechaCreacion(fechaCreacion);
                }
            }
        } catch (SQLException e) {
            // 23505 = unique_violation en Postgres: dos altas casi simultaneas con el mismo
            // username pueden pasar el chequeo previo en la app y chocar recien aqui, en la BD
            // (que es quien realmente lo garantiza). Se traduce a un mensaje claro en vez del
            // error crudo de SQL.
            if ("23505".equals(e.getSQLState())) {
                throw new RuntimeException("Ya existe un usuario con ese nombre, elija otro", e);
            }
            throw new RuntimeException("Error al guardar usuario", e);
        }
        return usuario;
    }

    @Override
    public void actualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET username = ?, nombre_completo = ?, rol = ?, activo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getNombreCompleto());
            ps.setString(3, usuario.getRol().name());
            ps.setBoolean(4, usuario.isActivo());
            ps.setLong(5, usuario.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar usuario", e);
        }
    }

    @Override
    public void actualizarPassword(Long id, String nuevoPasswordHash) {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoPasswordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la contrasena del usuario", e);
        }
    }

    @Override
    public void eliminar(Long id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar usuario", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setUsername(rs.getString("username"));
        usuario.setPasswordHash(rs.getString("password_hash"));
        usuario.setNombreCompleto(rs.getString("nombre_completo"));
        usuario.setRol(Rol.valueOf(rs.getString("rol")));
        usuario.setActivo(rs.getBoolean("activo"));
        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        if (fechaCreacion != null) {
            usuario.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        return usuario;
    }
}
