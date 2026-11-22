package com.dontoribio.pos.security;

import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccessControlRbacTest {

    private boolean puedeGestionarCatalogoYUsuarios(Usuario usuario) {
        return usuario != null && usuario.isActivo() && usuario.getRol() == Rol.ADMINISTRADOR;
    }

    @Test
    @DisplayName("OWASP A01:2025 - Un usuario CAJERO no debe tener permisos de ADMINISTRADOR")
    void cuandoRolEsCajero_debeDenegarOperacionesAdministrativas() {
        Usuario cajero = new Usuario();
        cajero.setUsername("vendedor1");
        cajero.setRol(Rol.CAJERO);
        cajero.setActivo(true);
        assertFalse(puedeGestionarCatalogoYUsuarios(cajero));
    }

    @Test
    @DisplayName("OWASP A01:2025 - Un usuario null debe ser rechazado")
    void cuandoNoHayUsuarioAutenticado_debeRechazarOperacion() {
        assertFalse(puedeGestionarCatalogoYUsuarios(null));
    }
}