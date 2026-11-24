
package com.dontoribio.pos.util;

import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.service.UsuarioService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CryptoBCryptSecurityTest {

    private static final String PASSWORD = "ClaveSegura123";

    @Test
    void debeGenerarHashBCrypt() {
        String hash = PasswordUtil.hashPassword(PASSWORD);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$12$"));
        assertNotEquals(PASSWORD, hash);
    }

    @Test
    void debeGenerarSaltDiferenteParaCadaHash() {
        String hash1 = PasswordUtil.hashPassword(PASSWORD);
        String hash2 = PasswordUtil.hashPassword(PASSWORD);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void debeVerificarPasswordCorrecta() {
        String hash = PasswordUtil.hashPassword(PASSWORD);

        assertTrue(PasswordUtil.verificarPassword(PASSWORD, hash));
    }

    @Test
    void noDebeAceptarPasswordIncorrecta() {
        String hash = PasswordUtil.hashPassword(PASSWORD);

        assertFalse(PasswordUtil.verificarPassword("OtraClave123", hash));
    }

    @Test
    void debeRechazarValoresNulosOVacios() {
        assertFalse(PasswordUtil.verificarPassword(null, "hash"));
        assertFalse(PasswordUtil.verificarPassword(PASSWORD, null));
        assertFalse(PasswordUtil.verificarPassword(PASSWORD, ""));
        assertFalse(PasswordUtil.verificarPassword(PASSWORD, "   "));
    }

    @Test
    void servicioDebeGuardarHashAlCrearUsuario() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        UsuarioService servicio = new UsuarioService(dao);

        when(dao.guardar(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        servicio.crearUsuario(
                "Cajero de prueba",
                "cajero_prueba",
                PASSWORD,
                Rol.CAJERO
        );

        verify(dao).guardar(argThat(usuario ->
                usuario.getPasswordHash() != null
                && usuario.getPasswordHash().startsWith("$2a$12$")
                && !usuario.getPasswordHash().equals(PASSWORD)
                && usuario.getUsername().equals("cajero_prueba")
                && usuario.getNombreCompleto().equals("Cajero de prueba")
        ));
    }

    @Test
    void servicioDebeGuardarHashAlCambiarPassword() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        UsuarioService servicio = new UsuarioService(dao);

        String hashAnterior = PasswordUtil.hashPassword(PASSWORD);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("cajero_prueba");
        usuario.setPasswordHash(hashAnterior);
        usuario.setNombreCompleto("Cajero de prueba");
        usuario.setRol(Rol.CAJERO);
        usuario.setActivo(true);

        String nuevaPassword = "NuevaClave456";

        servicio.cambiarPasswordPropia(
                usuario,
                PASSWORD,
                nuevaPassword
        );

        verify(dao).actualizarPassword(eq(1L), argThat(hash ->
                hash != null
                && hash.startsWith("$2a$12$")
                && !hash.equals(nuevaPassword)
                && PasswordUtil.verificarPassword(nuevaPassword, hash)
        ));
    }

    @Test
    void servicioDebeGuardarHashAlResetearPassword() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        UsuarioService servicio = new UsuarioService(dao);

        String nuevaPassword = "ClaveTemporal789";

        servicio.resetearPassword(1L, nuevaPassword);

        verify(dao).actualizarPassword(eq(1L), argThat(hash ->
                hash != null
                && hash.startsWith("$2a$12$")
                && !hash.equals(nuevaPassword)
                && PasswordUtil.verificarPassword(nuevaPassword, hash)
        ));
    }
}
