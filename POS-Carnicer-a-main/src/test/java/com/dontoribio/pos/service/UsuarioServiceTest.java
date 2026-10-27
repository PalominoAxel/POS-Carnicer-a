package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de UsuarioService: la pieza clave de la autorizacion de supervisor
 * (AutorizacionSupervisor) y del cambio/reseteo de contrasenas agregados para RBAC.
 */
class UsuarioServiceTest {

    private static final String PASSWORD_PLANA = "clave123";

    @Mock
    private UsuarioDAO usuarioDAO;

    private UsuarioService usuarioService;

    @BeforeEach
    void prepararMocks() {
        MockitoAnnotations.openMocks(this);
        usuarioService = new UsuarioService(usuarioDAO);
    }

    private Usuario crearUsuario(Rol rol, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin1");
        usuario.setPasswordHash(PasswordUtil.hashPassword(PASSWORD_PLANA));
        usuario.setRol(rol);
        usuario.setActivo(activo);
        return usuario;
    }

    @Test
    void esAdministrador_conRolAdministrador_esTrue() {
        assertTrue(usuarioService.esAdministrador(crearUsuario(Rol.ADMINISTRADOR, true)));
    }

    @Test
    void esAdministrador_conRolCajero_esFalse() {
        assertFalse(usuarioService.esAdministrador(crearUsuario(Rol.CAJERO, true)));
    }

    @Test
    void esAdministrador_conUsuarioNulo_esFalse() {
        assertFalse(usuarioService.esAdministrador(null));
    }

    @Test
    void esAdministrador_conAdministradorInactivo_esFalse() {
        assertFalse(usuarioService.esAdministrador(crearUsuario(Rol.ADMINISTRADOR, false)));
    }

    @Test
    void esAdministradorVigente_siSigueSiendoAdminActivoEnBD_esTrue() {
        Usuario sesionEnMemoria = crearUsuario(Rol.ADMINISTRADOR, true);
        when(usuarioDAO.buscarPorUsername("admin1")).thenReturn(Optional.of(crearUsuario(Rol.ADMINISTRADOR, true)));

        assertTrue(usuarioService.esAdministradorVigente(sesionEnMemoria));
    }

    @Test
    void esAdministradorVigente_siLoDesactivaronMientrasLaSesionSeguiaAbierta_esFalse() {
        // La sesion en memoria quedo marcada "activo=true" desde que inicio sesion, pero en la
        // base de datos ya lo desactivaron (ej. un admin le revoco el acceso). No debe alcanzar
        // para seguir autorizando acciones de supervisor.
        Usuario sesionEnMemoria = crearUsuario(Rol.ADMINISTRADOR, true);
        when(usuarioDAO.buscarPorUsername("admin1")).thenReturn(Optional.of(crearUsuario(Rol.ADMINISTRADOR, false)));

        assertFalse(usuarioService.esAdministradorVigente(sesionEnMemoria));
    }

    @Test
    void esAdministradorVigente_conUsuarioNulo_esFalse() {
        assertFalse(usuarioService.esAdministradorVigente(null));
    }

    @Test
    void verificarCredencialesAdministrador_conDatosCorrectos_esTrue() {
        when(usuarioDAO.buscarPorUsername("admin1")).thenReturn(Optional.of(crearUsuario(Rol.ADMINISTRADOR, true)));

        assertTrue(usuarioService.verificarCredencialesAdministrador("admin1", PASSWORD_PLANA));
    }

    @Test
    void verificarCredencialesAdministrador_conPasswordIncorrecta_esFalse() {
        when(usuarioDAO.buscarPorUsername("admin1")).thenReturn(Optional.of(crearUsuario(Rol.ADMINISTRADOR, true)));

        assertFalse(usuarioService.verificarCredencialesAdministrador("admin1", "otra-clave"));
    }

    @Test
    void verificarCredencialesAdministrador_siEsCajero_esFalseAunqueLaClaveSeaCorrecta() {
        when(usuarioDAO.buscarPorUsername("cajero1")).thenReturn(Optional.of(crearUsuario(Rol.CAJERO, true)));

        assertFalse(usuarioService.verificarCredencialesAdministrador("cajero1", PASSWORD_PLANA));
    }

    @Test
    void verificarCredencialesAdministrador_siEstaInactivo_esFalse() {
        when(usuarioDAO.buscarPorUsername("admin1")).thenReturn(Optional.of(crearUsuario(Rol.ADMINISTRADOR, false)));

        assertFalse(usuarioService.verificarCredencialesAdministrador("admin1", PASSWORD_PLANA));
    }

    @Test
    void verificarCredencialesAdministrador_siElUsuarioNoExiste_esFalse() {
        when(usuarioDAO.buscarPorUsername("fantasma")).thenReturn(Optional.empty());

        assertFalse(usuarioService.verificarCredencialesAdministrador("fantasma", PASSWORD_PLANA));
    }

    @Test
    void cambiarPasswordPropia_conLaActualCorrecta_laActualizaYDevuelveTrue() {
        Usuario usuario = crearUsuario(Rol.CAJERO, true);

        boolean resultado = usuarioService.cambiarPasswordPropia(usuario, PASSWORD_PLANA, "nueva-clave");

        assertTrue(resultado);
        verify(usuarioDAO).actualizarPassword(eq(usuario.getId()), anyString());
        assertTrue(PasswordUtil.verificarPassword("nueva-clave", usuario.getPasswordHash()));
    }

    @Test
    void cambiarPasswordPropia_conLaActualIncorrecta_noActualizaYDevuelveFalse() {
        Usuario usuario = crearUsuario(Rol.CAJERO, true);

        boolean resultado = usuarioService.cambiarPasswordPropia(usuario, "clave-equivocada", "nueva-clave");

        assertFalse(resultado);
        verify(usuarioDAO, never()).actualizarPassword(anyLong(), anyString());
    }

    @Test
    void resetearPassword_hasheaLaNuevaYLaGuardaPorId() {
        usuarioService.resetearPassword(42L, "clave-reseteada");

        verify(usuarioDAO).actualizarPassword(eq(42L), anyString());
    }

    @Test
    void crearUsuario_hasheaLaPasswordYGuardaConElRolIndicado() {
        when(usuarioDAO.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = usuarioService.crearUsuario("Nuevo Cajero", "cajero2", "clave123", Rol.CAJERO);

        assertTrue(creado.isActivo());
        assertTrue(PasswordUtil.verificarPassword("clave123", creado.getPasswordHash()));
        assertFalse(creado.getPasswordHash().equals("clave123"));
    }
}
