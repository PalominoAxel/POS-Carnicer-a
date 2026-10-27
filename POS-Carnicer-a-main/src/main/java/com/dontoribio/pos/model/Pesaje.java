package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lectura puntual de peso capturada desde la balanza (real o simulada). */
public class Pesaje {

    private BigDecimal pesoKg;
    private Producto producto;
    private BigDecimal importeCalculado;
    private LocalDateTime fechaHora;
    private boolean estable;

    public Pesaje() {
    }

    public Pesaje(BigDecimal pesoKg, Producto producto, BigDecimal importeCalculado,
                  LocalDateTime fechaHora, boolean estable) {
        this.pesoKg = pesoKg;
        this.producto = producto;
        this.importeCalculado = importeCalculado;
        this.fechaHora = fechaHora;
        this.estable = estable;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getImporteCalculado() {
        return importeCalculado;
    }

    public void setImporteCalculado(BigDecimal importeCalculado) {
        this.importeCalculado = importeCalculado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isEstable() {
        return estable;
    }

    public void setEstable(boolean estable) {
        this.estable = estable;
    }
}
