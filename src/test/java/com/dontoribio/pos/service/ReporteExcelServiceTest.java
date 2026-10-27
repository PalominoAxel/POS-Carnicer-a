package com.dontoribio.pos.service;

import com.dontoribio.pos.model.Cliente;
import com.dontoribio.pos.model.DetalleVenta;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Producto;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.model.Venta;
import com.google.common.collect.ListMultimap;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteExcelServiceTest {

    private final ReporteExcelService servicio = new ReporteExcelService();

    private static Venta venta(String cliente, String total, MetodoPago... metodos) {
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.of(2026, 10, 20, 10, 30));
        Cliente c = new Cliente();
        c.setNombreRazonSocial(cliente);
        venta.setCliente(c);
        Usuario u = new Usuario();
        u.setNombreCompleto("Cajero Uno");
        venta.setUsuario(u);
        venta.agregarDetalle(new DetalleVenta(new Producto(), BigDecimal.ONE, new BigDecimal(total)));
        for (MetodoPago metodo : metodos) {
            venta.agregarPago(new Pago(metodo, new BigDecimal(total), null));
        }
        return venta;
    }

    @Test
    void exportarVentas_generaHojaDeVentasYResumen() throws Exception {
        List<Venta> ventas = List.of(
                venta("Juan Perez", "30.00", MetodoPago.EFECTIVO),
                venta("Maria Lopez", "45.50", MetodoPago.EFECTIVO));
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        servicio.exportarVentas(ventas, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), salida);

        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(salida.toByteArray()))) {
            assertEquals(2, libro.getNumberOfSheets());
            Sheet hoja = libro.getSheet("Ventas");
            assertEquals("Fecha y hora", hoja.getRow(3).getCell(1).getStringCellValue());
            assertEquals("Juan Perez", hoja.getRow(4).getCell(2).getStringCellValue());
            assertEquals(45.50, hoja.getRow(5).getCell(6).getNumericCellValue(), 0.001);
            assertEquals("SUM(G5:G6)", hoja.getRow(6).getCell(6).getCellFormula());

            Sheet resumen = libro.getSheet("Resumen por pago");
            assertEquals("EFECTIVO", resumen.getRow(1).getCell(0).getStringCellValue());
            assertEquals(2, resumen.getRow(1).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(75.50, resumen.getRow(1).getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void exportarVentas_rangoInvertido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> servicio.exportarVentas(
                List.of(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), new ByteArrayOutputStream()));
    }

    @Test
    void exportarVentas_listaNula_lanzaNullPointer() {
        assertThrows(NullPointerException.class, () -> servicio.exportarVentas(
                null, LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream()));
    }

    @Test
    void agruparMontosPorMetodo_agrupaConMultimap() {
        ListMultimap<String, BigDecimal> grupos = ReporteExcelService.agruparMontosPorMetodo(List.of(
                venta("A", "10.00", MetodoPago.EFECTIVO),
                venta("B", "20.00", MetodoPago.EFECTIVO)));
        assertEquals(2, grupos.get("EFECTIVO").size());
    }

    @Test
    void describirPagos_unePagosConJoiner() {
        Venta v = venta("A", "10.00", MetodoPago.EFECTIVO, MetodoPago.EFECTIVO);
        assertEquals("EFECTIVO + EFECTIVO", ReporteExcelService.describirPagos(v.getPagos()));
    }
}
