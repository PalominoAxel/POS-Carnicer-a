package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.CajaDAO;
import com.dontoribio.pos.dao.impl.CajaDAOImpl;
import com.dontoribio.pos.model.Caja;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** Reglas de negocio de apertura/cierre de caja y resumen de ventas del turno. */
public class CajaService {

    private final CajaDAO cajaDAO;

    public CajaService() {
        this(new CajaDAOImpl());
    }

    public CajaService(CajaDAO cajaDAO) {
        this.cajaDAO = cajaDAO;
    }

    public Optional<Caja> buscarCajaAbierta() {
        return cajaDAO.buscarCajaAbierta();
    }

    public Caja abrirCaja(Usuario usuario, BigDecimal montoInicial) {
        Caja caja = new Caja();
        caja.setUsuario(usuario);
        caja.setMontoInicial(montoInicial.setScale(2, RoundingMode.HALF_UP));
        return cajaDAO.abrirCaja(caja);
    }

    public void cerrarCaja(Long cajaId, LocalDateTime fechaCierre, BigDecimal montoFinal) {
        cajaDAO.cerrarCaja(cajaId, fechaCierre, montoFinal);
    }

    public Map<MetodoPago, BigDecimal> resumenVentasDesde(LocalDateTime desde) {
        return cajaDAO.resumenVentasDesde(desde);
    }

    /** Suma lo cobrado por todos los metodos de pago del resumen. */
    public BigDecimal totalVentas(Map<MetodoPago, BigDecimal> resumen) {
        BigDecimal total = BigDecimal.ZERO;
        for (MetodoPago metodo : MetodoPago.values()) {
            total = total.add(resumen.getOrDefault(metodo, BigDecimal.ZERO));
        }
        return total;
    }

    /** Efectivo que deberia haber en caja: monto inicial + lo cobrado en efectivo. */
    public BigDecimal efectivoEsperado(Caja caja, Map<MetodoPago, BigDecimal> resumen) {
        BigDecimal efectivo = resumen.getOrDefault(MetodoPago.EFECTIVO, BigDecimal.ZERO);
        return caja.getMontoInicial().add(efectivo).setScale(2, RoundingMode.HALF_UP);
    }
}
