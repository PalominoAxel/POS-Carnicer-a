package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.ClienteDAO;
import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.model.Venta;
import com.dontoribio.pos.util.PosMonitoringServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/** Verifica el Pilar 2: contadores de ventas e histograma de reportes Excel. */
class MetricasNegocioTest {

    private static Venta ventaDePrueba() {
        Usuario u = new Usuario();
        u.setId(1L);
        Venta v = new Venta();
        v.setUsuario(u);
        v.setTotalesPersistidos(new BigDecimal("8.47"), new BigDecimal("10.00"));
        return v;
    }

    @Test
    void registrarVenta_exitosa_incrementaPosVentasTotal() {
        VentaService svc = new VentaService(mock(VentaDAO.class), mock(ClienteDAO.class));
        double antes = PosMonitoringServer.VENTAS_OK.count();

        svc.registrarVenta(ventaDePrueba());

        assertEquals(antes + 1, PosMonitoringServer.VENTAS_OK.count());
    }

    @Test
    void registrarVenta_conFalloDeBD_incrementaErroresYRelanzaLaExcepcion() {
        VentaDAO dao = mock(VentaDAO.class);
        doThrow(new RuntimeException("BD caida")).when(dao).registrarVenta(any());
        VentaService svc = new VentaService(dao, mock(ClienteDAO.class));
        double okAntes = PosMonitoringServer.VENTAS_OK.count();
        double errAntes = PosMonitoringServer.VENTAS_ERROR.count();

        assertThrows(RuntimeException.class, () -> svc.registrarVenta(ventaDePrueba()));

        assertEquals(errAntes + 1, PosMonitoringServer.VENTAS_ERROR.count());
        assertEquals(okAntes, PosMonitoringServer.VENTAS_OK.count());
    }

    @Test
    void exportarVentas_registraTiempoEnPosReporteExcelSeconds() throws Exception {
        long antes = PosMonitoringServer.REPORTE_TIMER.count();

        new ReporteExcelService().exportarVentas(List.of(), LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream());

        assertEquals(antes + 1, PosMonitoringServer.REPORTE_TIMER.count());
        assertTrue(PosMonitoringServer.REPORTE_TIMER.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) > 0);
    }

    @Test
    void exportarCompras_registraTiempoEnPosReporteExcelSeconds() throws Exception {
        long antes = PosMonitoringServer.REPORTE_TIMER.count();

        new ReporteExcelService().exportarCompras(List.of(), LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream());

        assertEquals(antes + 1, PosMonitoringServer.REPORTE_TIMER.count());
    }
}
