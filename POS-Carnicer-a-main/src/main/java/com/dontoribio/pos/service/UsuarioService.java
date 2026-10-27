package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.dao.impl.UsuarioDAOImpl;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.util.PasswordUtil;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de cuentas de acceso: roles, autorizacion y manejo de contrasenas. */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;

    public UsuarioService() {
        this(new UsuarioDAOImpl());
    }

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public boolean esAdministrador(Usuario usuario) {
        return usuario != null && usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo();
    }

    /**
     * Igual que {@link #esAdministrador}, pero vuelve a consultar el estado actual en la base
     * en lugar de confiar en el objeto Usuario de la sesion (que puede estar desactualizado).
     * Sin esto, si a un administrador le revocan el acceso (lo desactivan o lo bajan a CAJERO)
     * mientras su sesion del POS sigue abierta, esa sesion seguiria pudiendo autorizar acciones
     * de supervisor indefinidamente porque nunca vuelve a leer su estado real.
     */
    public boolean esAdministradorVigente(Usuario usuarioSesion) {
        if (usuarioSesion == null || usuarioSesion.getUsername() == null) {
            return false;
        }
        return usuarioDAO.buscarPorUsername(usuarioSesion.getUsername())
                .filter(this::esAdministrador)
                .isPresent();
    }

    /** Verifica que exista un administrador activo con ese usuario y contrasena. */
    public boolean verificarCredencialesAdministrador(String username, String password) {
        return usuarioDAO.buscarPorUsername(username)
                .filter(Usuario::isActivo)
                .filter(this::esAdministrador)
                .filter(u -> PasswordUtil.verificarPassword(password, u.getPasswordHash()))
                .isPresent();
    }

    /** Cambia la contrasena del propio usuario, validando primero la contrasena actual. */
    public boolean cambiarPasswordPropia(Usuario usuarioSesion, String passwordActual, String passwordNueva) {
        if (!PasswordUtil.verificarPassword(passwordActual, usuarioSesion.getPasswordHash())) {
            return false;
        }
        String hash = PasswordUtil.hashPassword(passwordNueva);
        usuarioDAO.actualizarPassword(usuarioSesion.getId(), hash);
        usuarioSesion.setPasswordHash(hash);
        return true;
    }

    /** Restablece la contrasena de cualquier usuario sin requerir la anterior (uso de administrador). */
    public void resetearPassword(Long usuarioId, String passwordNueva) {
        usuarioDAO.actualizarPassword(usuarioId, PasswordUtil.hashPassword(passwordNueva));
    }

    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos();
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioDAO.buscarPorUsername(username);
    }

    public Usuario crearUsuario(String nombreCompleto, String username, String password, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setUsername(username);
        usuario.setPasswordHash(PasswordUtil.hashPassword(password));
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuarioDAO.guardar(usuario);
    }

    public void actualizarDatos(Usuario usuario) {
        usuarioDAO.actualizar(usuario);
    }
}
