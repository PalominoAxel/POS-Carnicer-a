package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.ConfiguracionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Implementacion JDBC de {@link ConfiguracionDAO} contra la tabla `configuracion` (clave/valor). */
public class ConfiguracionDAOImpl implements ConfiguracionDAO {

    @Override
    public Optional<String> obtener(String clave) {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString("valor")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer configuracion", e);
        }
    }

    @Override
    public void guardar(String clave, String valor) {
        // Sin ON CONFLICT/MERGE: esa sintaxis difiere entre H2 y Postgres. Un UPDATE y, si no
        // afecto ninguna fila, un INSERT, funciona igual en ambos motores.
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sqlUpdate = "UPDATE configuracion SET valor = ? WHERE clave = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, valor);
                ps.setString(2, clave);
                if (ps.executeUpdate() > 0) {
                    return;
                }
            }
            String sqlInsert = "INSERT INTO configuracion (clave, valor) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setString(1, clave);
                ps.setString(2, valor);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar configuracion", e);
        }
    }
}
