package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Producto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Operaciones de acceso a datos para el catalogo de productos carnicos. */
public interface ProductoDAO {

    Optional<Producto> buscarPorId(Long id);

    Optional<Producto> buscarPorCodigo(String codigo);

    List<Producto> listarActivos();

    List<Producto> listarTodos();

    Producto guardar(Producto producto);

    void actualizar(Producto producto);

    void eliminar(Long id);

    /**
     * Actualiza lote/fecha de produccion/fecha de vencimiento en el registro de compra
     * mas reciente de este producto. Devuelve false si el producto no tiene ninguna
     * compra registrada (no hay donde guardar esos datos).
     */
    boolean actualizarTrazabilidad(Long productoId, String lote, LocalDate fechaProduccion, LocalDate fechaVencimiento);
}
