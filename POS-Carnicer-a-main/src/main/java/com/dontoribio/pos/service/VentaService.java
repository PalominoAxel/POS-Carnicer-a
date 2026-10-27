package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.ClienteDAO;
import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.dao.impl.ClienteDAOImpl;
import com.dontoribio.pos.dao.impl.VentaDAOImpl;
import com.dontoribio.pos.model.Cliente;
import com.dontoribio.pos.model.DetalleVenta;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.TipoCliente;
import com.dontoribio.pos.model.TipoComprobante;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.model.Venta;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de registro de venta: resolucion de cliente y armado/persistencia de la venta. */
public class VentaService {

    private static final String DOCUMENTO_CLIENTE_GENERICO = "00000000";

    private final VentaDAO ventaDAO;
    private final ClienteDAO clienteDAO;
    private Cliente clienteGenerico;

    public VentaService() {
        this(new VentaDAOImpl(), new ClienteDAOImpl());
    }

    public VentaService(VentaDAO ventaDAO, ClienteDAO clienteDAO) {
        this.ventaDAO = ventaDAO;
        this.clienteDAO = clienteDAO;
    }

    /** Cliente por defecto ("VTA PUBLICO") usado cuando la venta no tiene un cliente identificado. */
    public Cliente obtenerClienteGenerico() {
        if (clienteGenerico != null) {
            return clienteGenerico;
        }
        Optional<Cliente> existente = clienteDAO.buscarPorDocumento(DOCUMENTO_CLIENTE_GENERICO);
        if (existente.isPresent()) {
            clienteGenerico = existente.get();
            return clienteGenerico;
        }
        Cliente nuevo = new Cliente();
        nuevo.setTipoCliente(TipoCliente.MINORISTA);
        nuevo.setNombreRazonSocial("VTA PUBLICO");
        nuevo.setDocumento(DOCUMENTO_CLIENTE_GENERICO);
        clienteGenerico = clienteDAO.guardar(nuevo);
        return clienteGenerico;
    }

    /** Resuelve el cliente de la venta a partir del nombre ingresado, o el cliente generico si esta vacio. */
    public Cliente resolverClientePorNombre(String nombreCliente, TipoCliente tipoCliente) {
        if (nombreCliente == null || nombreCliente.isBlank()) {
            return obtenerClienteGenerico();
        }
        return clienteDAO.buscarPorNombre(nombreCliente).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setNombreRazonSocial(nombreCliente);
            nuevo.setTipoCliente(tipoCliente != null ? tipoCliente : TipoCliente.MINORISTA);
            return clienteDAO.guardar(nuevo);
        });
    }

    public Venta construirVenta(Usuario usuario, Cliente cliente, TipoComprobante tipoComprobante,
                                 String documentoCliente, List<DetalleVenta> items, MetodoPago metodoPago) {
        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setTipoComprobante(tipoComprobante);
        venta.setDocumentoCliente(documentoCliente);
        items.forEach(venta::agregarDetalle);
        venta.agregarPago(new Pago(metodoPago, venta.getTotal(), null));
        return venta;
    }

    public void registrarVenta(Venta venta) {
        ventaDAO.registrarVenta(venta);
    }
}
