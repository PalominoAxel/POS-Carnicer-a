package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Compra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Operaciones de acceso a datos para el registro de compras (ingreso de mercaderia). */
public interface CompraDAO {

    /** Registra la cabecera y el detalle de la compra, e incrementa el stock de cada producto. */
    Compra registrarCompra(Compra compra);

    /** Cabeceras de compra (sin detalle) dentro del rango de fechas, mas recientes primero. */
    List<Compra> listarPorRangoFechas(LocalDate desde, LocalDate hasta);

    /** Cabeceras de compra (sin detalle) de cualquier fecha hechas a un proveedor cuyo nombre contenga el texto dado. */
    List<Compra> listarPorProveedor(String nombreProveedor);

    /** Compra completa (cabecera + detalle de productos) para reimprimir su comprobante. */
    Optional<Compra> buscarCompraCompleta(Long id);
}
