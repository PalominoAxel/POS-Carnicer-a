package com.dontoribio.pos.dao;

import com.dontoribio.pos.model.Venta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Persistencia de ventas junto con sus detalles y pagos (operacion transaccional). */
public interface VentaDAO {

    /** Inserta la venta, sus detalles y pagos dentro de una unica transaccion. */
    Venta registrarVenta(Venta venta);

    Optional<Venta> buscarPorId(Long id);

    /** Lista las ventas del dia con cliente, cajero y pagos cargados (para el historial de ventas). */
    List<Venta> listarPorFecha(LocalDate fecha);

    /** Lista las ventas entre dos fechas (inclusive), con cliente, cajero y pagos cargados. */
    List<Venta> listarPorRangoFechas(LocalDate desde, LocalDate hasta);

    /** Carga la venta completa (cliente, cajero, detalles con producto y pagos) para reimprimir su comprobante. */
    Optional<Venta> buscarVentaCompleta(Long id);
}
