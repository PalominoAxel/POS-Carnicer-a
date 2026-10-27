package com.dontoribio.pos.model;

/** Cliente minorista o mayorista al que se le factura una venta. */
public class Cliente {

    private Long id;
    private TipoCliente tipoCliente;
    private String nombreRazonSocial;
    private String documento; // DNI (minorista) o RUC (mayorista)
    private String telefono;
    private String direccion;

    public Cliente() {
    }

    public Cliente(Long id, TipoCliente tipoCliente, String nombreRazonSocial,
                   String documento, String telefono, String direccion) {
        this.id = id;
        this.tipoCliente = tipoCliente;
        this.nombreRazonSocial = nombreRazonSocial;
        this.documento = documento;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(TipoCliente tipoCliente) {
        this.tipoCliente = tipoCliente;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String toString() {
        return nombreRazonSocial;
    }
}
