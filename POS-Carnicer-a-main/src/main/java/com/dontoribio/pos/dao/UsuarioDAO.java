package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Usuario;

import java.util.List;
import java.util.Optional;

/** Operaciones de acceso a datos para usuarios (login y administracion de cuentas). */
public interface UsuarioDAO {

    Optional<Usuario> buscarPorUsername(String username);

    List<Usuario> listarTodos();

    Usuario guardar(Usuario usuario);

    void actualizar(Usuario usuario);

    void actualizarPassword(Long id, String nuevoPasswordHash);

    void eliminar(Long id);
}
