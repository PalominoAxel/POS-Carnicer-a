package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Linea de compra: un producto recibido de un proveedor dentro de una {@link Compra}.
 * Lote, fecha de produccion y fecha de vencimiento son datos de trazabilidad opcionales:
 * el usuario puede registrarlos si el proveedor los provee, pero no son obligatorios para
 * registrar la compra.
 */
public class DetalleCompra {

    private Long id;
    private Producto producto;
    private BigDecimal pesoKg;
    private BigDecimal costoUnitario;
    private BigDecimal importe;
    private String lote;
    private LocalDate fechaProduccion;
    private LocalDate fechaVencimiento;

    public DetalleCompra() {
    }

    public DetalleCompra(Producto producto, BigDecimal pesoKg, BigDecimal costoUnitario) {
        this.producto = producto;
        this.pesoKg = pesoKg;
        this.costoUnitario = costoUnitario;
        this.importe = pesoKg.multiply(costoUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public DetalleCompra(Producto producto, BigDecimal pesoKg, BigDecimal costoUnitario,
                          String lote, LocalDate fechaProduccion, LocalDate fechaVencimiento) {
        this(producto, pesoKg, costoUnitario);
        this.lote = lote;
        this.fechaProduccion = fechaProduccion;
        this.fechaVencimiento = fechaVencimiento;
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

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public LocalDate getFechaProduccion() {
        return fechaProduccion;
    }

    public void setFechaProduccion(LocalDate fechaProduccion) {
        this.fechaProduccion = fechaProduccion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
