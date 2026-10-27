package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.ClienteDAO;
import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.model.Cliente;
import com.dontoribio.pos.model.DetalleVenta;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Producto;
import com.dontoribio.pos.model.TipoCliente;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.model.Venta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de VentaService con los DAO simulados (Mockito), sin tocar la base de datos real.
 * Cubren la resolucion de cliente (generico vs. por nombre) y el armado de la venta,
 * que es exactamente la logica que se movio desde MainPOSController en el refactor de capas.
 */
class VentaServiceTest {

    @Mock
    private VentaDAO ventaDAO;
    @Mock
    private ClienteDAO clienteDAO;

    private VentaService ventaService;

    @BeforeEach
    void prepararMocks() {
        MockitoAnnotations.openMocks(this);
        ventaService = new VentaService(ventaDAO, clienteDAO);
    }

    @Test
    void obtenerClienteGenerico_cuandoYaExisteEnBD_loDevuelveSinCrearUnoNuevo() {
        Cliente generico = new Cliente();
        generico.setId(1L);
        generico.setNombreRazonSocial("VTA PUBLICO");
        when(clienteDAO.buscarPorDocumento("00000000")).thenReturn(Optional.of(generico));

        Cliente resultado = ventaService.obtenerClienteGenerico();

        assertSame(generico, resultado);
        verify(clienteDAO, never()).guardar(any());
    }

    @Test
    void obtenerClienteGenerico_cuandoNoExiste_creaUnoNuevo() {
        when(clienteDAO.buscarPorDocumento("00000000")).thenReturn(Optional.empty());
        when(clienteDAO.guardar(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        Cliente resultado = ventaService.obtenerClienteGenerico();

        assertEquals("VTA PUBLICO", resultado.getNombreRazonSocial());
        assertEquals(TipoCliente.MINORISTA, resultado.getTipoCliente());
        assertEquals("00000000", resultado.getDocumento());
    }

    @Test
    void obtenerClienteGenerico_llamadoDosVeces_soloConsultaLaBDUnaVez() {
        Cliente generico = new Cliente();
        generico.setId(1L);
        when(clienteDAO.buscarPorDocumento("00000000")).thenReturn(Optional.of(generico));

        ventaService.obtenerClienteGenerico();
        ventaService.obtenerClienteGenerico();

        verify(clienteDAO, org.mockito.Mockito.times(1)).buscarPorDocumento("00000000");
    }

    @Test
    void resolverClientePorNombre_nombreVacio_devuelveClienteGenerico() {
        Cliente generico = new Cliente();
        generico.setId(1L);
        when(clienteDAO.buscarPorDocumento("00000000")).thenReturn(Optional.of(generico));

        Cliente resultado = ventaService.resolverClientePorNombre("   ", TipoCliente.MINORISTA);

        assertSame(generico, resultado);
    }

    @Test
    void resolverClientePorNombre_existente_loDevuelveSinCrearUnoNuevo() {
        Cliente existente = new Cliente();
        existente.setId(5L);
        existente.setNombreRazonSocial("Juan Perez");
        when(clienteDAO.buscarPorNombre("Juan Perez")).thenReturn(Optional.of(existente));

        Cliente resultado = ventaService.resolverClientePorNombre("Juan Perez", TipoCliente.MAYORISTA);

        assertSame(existente, resultado);
        verify(clienteDAO, never()).guardar(any());
    }

    @Test
    void resolverClientePorNombre_noExistente_creaUnoNuevoConElTipoIndicado() {
        when(clienteDAO.buscarPorNombre("Nuevo Cliente")).thenReturn(Optional.empty());
        when(clienteDAO.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = ventaService.resolverClientePorNombre("Nuevo Cliente", TipoCliente.MAYORISTA);

        assertEquals("Nuevo Cliente", resultado.getNombreRazonSocial());
        assertEquals(TipoCliente.MAYORISTA, resultado.getTipoCliente());
    }

    @Test
    void resolverClientePorNombre_noExistenteYSinTipo_usaMinoristaPorDefecto() {
        when(clienteDAO.buscarPorNombre("Nuevo Cliente")).thenReturn(Optional.empty());
        when(clienteDAO.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = ventaService.resolverClientePorNombre("Nuevo Cliente", null);

        assertEquals(TipoCliente.MINORISTA, resultado.getTipoCliente());
    }

    @Test
    void construirVenta_calculaElTotalYAgregaElPagoPorElMontoCompleto() {
        Producto producto = new Producto();
        producto.setId(10L);
        DetalleVenta detalle = new DetalleVenta(producto, new BigDecimal("2.000"), new BigDecimal("15.00"));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        Venta venta = ventaService.construirVenta(usuario, cliente, null, "12345678",
                List.of(detalle), MetodoPago.EFECTIVO);

        assertEquals(new BigDecimal("30.00"), venta.getTotal());
        assertEquals(1, venta.getPagos().size());
        assertEquals(new BigDecimal("30.00"), venta.getPagos().get(0).getMonto());
        assertEquals(MetodoPago.EFECTIVO, venta.getPagos().get(0).getMetodoPago());
        assertSame(usuario, venta.getUsuario());
        assertSame(cliente, venta.getCliente());
        assertEquals("12345678", venta.getDocumentoCliente());
    }

    @Test
    void registrarVenta_delegaDirectamenteEnElDAO() {
        Venta venta = new Venta();

        ventaService.registrarVenta(venta);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaDAO).registrarVenta(captor.capture());
        assertSame(venta, captor.getValue());
    }
}
