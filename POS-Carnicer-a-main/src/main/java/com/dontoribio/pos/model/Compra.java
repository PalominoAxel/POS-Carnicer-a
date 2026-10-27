package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Recepcion de mercaderia comprada a un {@link Proveedor}; incrementa el stock del catalogo. */
public class Compra {

    private Long id;
    private LocalDateTime fecha;
    private Proveedor proveedor;
    private Usuario usuario;
    private List<DetalleCompra> detalles = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    public Compra() {
    }

    public void agregarDetalle(DetalleCompra detalle) {
        detalles.add(detalle);
        recalcularTotal();
    }

    private void recalcularTotal() {
        total = detalles.stream()
                .map(DetalleCompra::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
