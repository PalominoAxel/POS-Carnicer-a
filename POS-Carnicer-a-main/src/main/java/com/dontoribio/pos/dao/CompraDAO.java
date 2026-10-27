package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Compra;

/** Operaciones de acceso a datos para el registro de compras (ingreso de mercaderia). */
public interface CompraDAO {

    /** Registra la cabecera y el detalle de la compra, e incrementa el stock de cada producto. */
    Compra registrarCompra(Compra compra);
}
