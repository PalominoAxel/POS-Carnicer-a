package com.dontoribio.pos.dao.impl;

import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.dao.CajaDAO;
import com.dontoribio.pos.model.Caja;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Implementacion JDBC de {@link CajaDAO}. Esquema esperado: tabla caja, pagos, ventas. */
public class CajaDAOImpl implements CajaDAO {

    private static final Logger LOG = LoggerFactory.getLogger(CajaDAOImpl.class);

    @Override
    public Caja abrirCaja(Caja caja) {
        String sql = "INSERT INTO caja (usuario_id, fecha_apertura, monto_inicial) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime fechaApertura = caja.getFechaApertura() != null ? caja.getFechaApertura() : LocalDateTime.now();
            ps.setLong(1, caja.getUsuario().getId());
            ps.setTimestamp(2, Timestamp.valueOf(fechaApertura));
            ps.setBigDecimal(3, caja.getMontoInicial());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    caja.setId(rs.getLong("id"));
                }
            }
            caja.setFechaApertura(fechaApertura);
            return caja;
        } catch (SQLException e) {
            LOG.error("Error al abrir caja (usuario={})", caja.getUsuario() == null ? null : caja.getUsuario().getId(), e);
            throw new RuntimeException("Error al abrir caja", e);
        }
    }

    @Override
    public Optional<Caja> buscarCajaAbierta() {
        String sql = "SELECT c.id, c.usuario_id, c.fecha_apertura, c.monto_inicial, u.nombre_completo, u.rol " +
                "FROM caja c JOIN usuarios u ON u.id = c.usuario_id " +
                "WHERE c.fecha_cierre IS NULL ORDER BY c.fecha_apertura DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            Caja caja = new Caja();
            caja.setId(rs.getLong("id"));
            caja.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
            caja.setMontoInicial(rs.getBigDecimal("monto_inicial"));
            Usuario usuario = new Usuario();
            usuario.setId(rs.getLong("usuario_id"));
            usuario.setNombreCompleto(rs.getString("nombre_completo"));
            caja.setUsuario(usuario);
            return Optional.of(caja);
        } catch (SQLException e) {
            LOG.error("Error al buscar caja abierta (posible tabla 'caja' inexistente, revisar schema.sql)", e);
            throw new RuntimeException("Error al buscar caja abierta", e);
        }
    }

    @Override
    public void cerrarCaja(Long id, LocalDateTime fechaCierre, BigDecimal montoFinal) {
        String sql = "UPDATE caja SET fecha_cierre = ?, monto_final = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fechaCierre));
            ps.setBigDecimal(2, montoFinal);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cerrar caja", e);
        }
    }

    @Override
    public Map<MetodoPago, BigDecimal> resumenVentasDesde(LocalDateTime desde) {
        String sql = "SELECT p.metodo_pago, SUM(p.monto) AS total " +
                "FROM pagos p JOIN ventas v ON v.id = p.venta_id " +
                "WHERE v.fecha >= ? GROUP BY p.metodo_pago";
        Map<MetodoPago, BigDecimal> resumen = new EnumMap<>(MetodoPago.class);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resumen.put(MetodoPago.valueOf(rs.getString("metodo_pago")), rs.getBigDecimal("total"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular el resumen de ventas", e);
        }
        return resumen;
    }
}
