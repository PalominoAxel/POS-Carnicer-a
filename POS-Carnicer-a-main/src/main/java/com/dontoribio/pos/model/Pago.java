package com.dontoribio.pos.model;

import java.math.BigDecimal;

/** Pago aplicado a una venta. Una venta puede tener varios pagos (pago mixto). */
public class Pago {

    private Long id;
    private MetodoPago metodoPago;
    private BigDecimal monto;
    private String referencia; // N° operacion Yape/Plin/transferencia, ultimos digitos tarjeta, etc.

    public Pago() {
    }

    public Pago(MetodoPago metodoPago, BigDecimal monto, String referencia) {
        this.metodoPago = metodoPago;
        this.monto = monto;
        this.referencia = referencia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
