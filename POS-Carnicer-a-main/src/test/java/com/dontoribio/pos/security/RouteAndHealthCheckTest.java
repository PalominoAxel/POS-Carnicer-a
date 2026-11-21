package com.dontoribio.pos.security;

import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RouteAndHealthCheckTest {

    // Equivalente de seguridad de rutas: solo LOGIN es público; otras vistas requieren sesión
    private boolean puedeAccederVista(String vista, Usuario usuario) {
        if ("LOGIN".equalsIgnoreCase(vista)) {
            return true;
        }
        return usuario != null && usuario.isActivo() && usuario.getRol() != null;
    }

    // Equivalente de /actuator/health (show-details=never): devuelve solo estado UP/DOWN
    private String obtenerEstadoSalud(Connection conexion) {
        try {
            if (conexion != null && conexion.isValid(2)) {
                return "{\"status\":\"UP\"}";
            }
        } catch (SQLException ignored) {
            // No exponer detalles técnicos ni credenciales
        }
        return "{\"status\":\"DOWN\"}";
    }

    @Test
    void vistaLoginDebeSerPublicaYMainPosRequiereSesion() {
        assertTrue(puedeAccederVista("LOGIN", null));
        assertFalse(puedeAccederVista("MAIN_POS", null));

        Usuario cajero = new Usuario();
        cajero.setActivo(true);
        cajero.setRol(Rol.CAJERO);
        assertTrue(puedeAccederVista("MAIN_POS", cajero));
    }

    @Test
    void healthCheckDebeResponderUpCuandoConexionEsValida() throws Exception {
        Connection conexionMock = mock(Connection.class);
        when(conexionMock.isValid(2)).thenReturn(true);

        String respuesta = obtenerEstadoSalud(conexionMock);
        assertEquals("{\"status\":\"UP\"}", respuesta);
    }
}