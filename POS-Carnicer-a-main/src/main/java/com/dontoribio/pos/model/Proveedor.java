package com.dontoribio.pos.model;

/** Proveedor de mercaderia (carnes, embutidos, etc.) del modulo de compras. */
public class Proveedor {

    private Long id;
    private String nombreRazonSocial;
    private String documento;
    private String telefono;

    public Proveedor() {
    }

    public Proveedor(Long id, String nombreRazonSocial, String documento, String telefono) {
        this.id = id;
        this.nombreRazonSocial = nombreRazonSocial;
        this.documento = documento;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreRazonSocial() {
        return nombreRazonSocial;
    }

    public void setNombreRazonSocial(String nombreRazonSocial) {
        this.nombreRazonSocial = nombreRazonSocial;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @Override
    public String toString() {
        return nombreRazonSocial;
    }
}
