package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Linea de venta: un producto pesado dentro de una lista/cuenta de atencion. */
public class DetalleVenta {

    private Long id;
    private Producto producto;
    private BigDecimal pesoKg;
    private BigDecimal precioUnitario;
    private BigDecimal importe;

    public DetalleVenta() {
    }

    public DetalleVenta(Producto producto, BigDecimal pesoKg, BigDecimal precioUnitario) {
        this.producto = producto;
        this.pesoKg = pesoKg;
        this.precioUnitario = precioUnitario;
        this.importe = pesoKg.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }
}
