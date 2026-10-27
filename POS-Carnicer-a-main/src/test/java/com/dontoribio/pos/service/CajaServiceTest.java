package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.CajaDAO;
import com.dontoribio.pos.model.Caja;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CajaService con el DAO simulado. Cubren los calculos que antes estaban
 * duplicados entre MainPOSController y ControlCajaController (total vendido, efectivo esperado).
 */
class CajaServiceTest {

    @Mock
    private CajaDAO cajaDAO;

    private CajaService cajaService;

    @BeforeEach
    void prepararMocks() {
        MockitoAnnotations.openMocks(this);
        cajaService = new CajaService(cajaDAO);
    }

    @Test
    void abrirCaja_fijaUsuarioYEscalaElMontoInicialAntesDeGuardar() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(cajaDAO.abrirCaja(any(Caja.class))).thenAnswer(inv -> inv.getArgument(0));

        Caja resultado = cajaService.abrirCaja(usuario, new BigDecimal("100"));

        ArgumentCaptor<Caja> captor = ArgumentCaptor.forClass(Caja.class);
        verify(cajaDAO).abrirCaja(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getMontoInicial());
        assertEquals(new BigDecimal("100.00"), resultado.getMontoInicial());
    }

    @Test
    void cerrarCaja_delegaDirectamenteEnElDAO() {
        LocalDateTime fechaCierre = LocalDateTime.of(2026, 10, 7, 20, 0);
        BigDecimal montoFinal = new BigDecimal("250.00");

        cajaService.cerrarCaja(3L, fechaCierre, montoFinal);

        verify(cajaDAO).cerrarCaja(3L, fechaCierre, montoFinal);
    }

    @Test
    void buscarCajaAbierta_delegaDirectamenteEnElDAO() {
        Caja caja = new Caja();
        when(cajaDAO.buscarCajaAbierta()).thenReturn(Optional.of(caja));

        Optional<Caja> resultado = cajaService.buscarCajaAbierta();

        assertSame(caja, resultado.orElseThrow());
    }

    @Test
    void totalVentas_sumaElMontoDeTodosLosMetodosDePago() {
        Map<MetodoPago, BigDecimal> resumen = new EnumMap<>(MetodoPago.class);
        resumen.put(MetodoPago.EFECTIVO, new BigDecimal("100.00"));
        resumen.put(MetodoPago.YAPE, new BigDecimal("50.00"));
        resumen.put(MetodoPago.TARJETA, new BigDecimal("25.50"));

        BigDecimal total = cajaService.totalVentas(resumen);

        assertEquals(new BigDecimal("175.50"), total);
    }

    @Test
    void totalVentas_conResumenVacio_devuelveCero() {
        BigDecimal total = cajaService.totalVentas(new EnumMap<>(MetodoPago.class));

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void efectivoEsperado_sumaMontoInicialMasLoCobradoEnEfectivo() {
        Caja caja = new Caja();
        caja.setMontoInicial(new BigDecimal("50.00"));
        Map<MetodoPago, BigDecimal> resumen = new EnumMap<>(MetodoPago.class);
        resumen.put(MetodoPago.EFECTIVO, new BigDecimal("120.00"));
        resumen.put(MetodoPago.YAPE, new BigDecimal("999.00")); // no debe contar, solo efectivo

        BigDecimal esperado = cajaService.efectivoEsperado(caja, resumen);

        assertEquals(new BigDecimal("170.00"), esperado);
    }

    @Test
    void efectivoEsperado_sinVentasEnEfectivo_esIgualAlMontoInicial() {
        Caja caja = new Caja();
        caja.setMontoInicial(new BigDecimal("50.00"));

        BigDecimal esperado = cajaService.efectivoEsperado(caja, new EnumMap<>(MetodoPago.class));

        assertEquals(new BigDecimal("50.00"), esperado);
    }
}
