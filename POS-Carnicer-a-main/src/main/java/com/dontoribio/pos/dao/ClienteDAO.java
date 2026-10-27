package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Cliente;

import java.util.List;
import java.util.Optional;

/** Operaciones de acceso a datos para clientes minoristas y mayoristas. */
public interface ClienteDAO {

    Optional<Cliente> buscarPorId(Long id);

    Optional<Cliente> buscarPorDocumento(String documento);

    Optional<Cliente> buscarPorNombre(String nombreRazonSocial);

    List<Cliente> listarTodos();

    Cliente guardar(Cliente cliente);

    void actualizar(Cliente cliente);

    void eliminar(Long id);
}
