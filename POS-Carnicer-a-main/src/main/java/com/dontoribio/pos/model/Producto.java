package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Producto carnico vendido por peso (kg). */
public class Producto {

    private Long id;
    private String codigo;
    private String nombre;
    private String imagenPath;
    private BigDecimal precioKgMinorista;
    private BigDecimal precioKgMayorista;
    private String unidadMedida; // kg, g, unidad, etc.
    private BigDecimal stockDisponible;
    private boolean activo;

    // Datos de trazabilidad de solo lectura: provienen de la ultima compra registrada
    // para este producto (ver ProductoDAOImpl). No se editan desde el catalogo.
    private String loteActual;
    private LocalDate fechaProduccionActual;
    private LocalDate fechaVencimientoActual;

    public Producto() {
    }

    public Producto(Long id, String codigo, String nombre, BigDecimal precioKgMinorista,
                     BigDecimal precioKgMayorista, String unidadMedida,
                     BigDecimal stockDisponible, boolean activo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioKgMinorista = precioKgMinorista;
        this.precioKgMayorista = precioKgMayorista;
        this.unidadMedida = unidadMedida;
        this.stockDisponible = stockDisponible;
        this.activo = activo;
    }

    /** Obtiene el precio por kg segun el tipo de cliente. */
    public BigDecimal getPrecioPara(TipoCliente tipoCliente) {
        return tipoCliente == TipoCliente.MAYORISTA ? precioKgMayorista : precioKgMinorista;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagenPath() {
        return imagenPath;
    }

    public void setImagenPath(String imagenPath) {
        this.imagenPath = imagenPath;
    }

    public BigDecimal getPrecioKgMinorista() {
        return precioKgMinorista;
    }

    public void setPrecioKgMinorista(BigDecimal precioKgMinorista) {
        this.precioKgMinorista = precioKgMinorista;
    }

    public BigDecimal getPrecioKgMayorista() {
        return precioKgMayorista;
    }

    public void setPrecioKgMayorista(BigDecimal precioKgMayorista) {
        this.precioKgMayorista = precioKgMayorista;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(BigDecimal stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getLoteActual() {
        return loteActual;
    }

    public void setLoteActual(String loteActual) {
        this.loteActual = loteActual;
    }

    public LocalDate getFechaProduccionActual() {
        return fechaProduccionActual;
    }

    public void setFechaProduccionActual(LocalDate fechaProduccionActual) {
        this.fechaProduccionActual = fechaProduccionActual;
    }

    public LocalDate getFechaVencimientoActual() {
        return fechaVencimientoActual;
    }

    public void setFechaVencimientoActual(LocalDate fechaVencimientoActual) {
        this.fechaVencimientoActual = fechaVencimientoActual;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
