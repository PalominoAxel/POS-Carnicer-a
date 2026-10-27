package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Caja;
import com.dontoribio.pos.model.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** Persistencia de arqueos de caja (apertura/cierre de turno). */
public interface CajaDAO {

    Caja abrirCaja(Caja caja);

    /** Ultima caja sin cerrar (fecha_cierre nula), si existe. */
    Optional<Caja> buscarCajaAbierta();

    void cerrarCaja(Long id, LocalDateTime fechaCierre, BigDecimal montoFinal);

    /** Suma el monto cobrado por cada metodo de pago desde la fecha indicada (inclusive). */
    Map<MetodoPago, BigDecimal> resumenVentasDesde(LocalDateTime desde);
}
