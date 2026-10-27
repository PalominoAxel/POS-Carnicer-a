package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Proveedor;

import java.util.List;
import java.util.Optional;

/** Operaciones de acceso a datos para proveedores de mercaderia. */
public interface ProveedorDAO {

    Optional<Proveedor> buscarPorId(Long id);

    List<Proveedor> listarTodos();

    Proveedor guardar(Proveedor proveedor);

    void actualizar(Proveedor proveedor);

    void eliminar(Long id);
}
