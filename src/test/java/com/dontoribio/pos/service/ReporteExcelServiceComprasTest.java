package com.dontoribio.pos.service;

import com.dontoribio.pos.model.Compra;
import com.dontoribio.pos.model.DetalleCompra;
import com.dontoribio.pos.model.Producto;
import com.dontoribio.pos.model.Proveedor;
import com.dontoribio.pos.model.Usuario;
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

class ReporteExcelServiceComprasTest {

    private final ReporteExcelService servicio = new ReporteExcelService();

    private static Compra compra(String proveedor, String costoUnitario) {
        Compra compra = new Compra();
        compra.setFecha(LocalDateTime.of(2026, 10, 20, 9, 0));
        Proveedor p = new Proveedor();
        p.setNombreRazonSocial(proveedor);
        compra.setProveedor(p);
        Usuario u = new Usuario();
        u.setNombreCompleto("Admin Uno");
        compra.setUsuario(u);
        compra.agregarDetalle(new DetalleCompra(new Producto(), BigDecimal.ONE, new BigDecimal(costoUnitario)));
        return compra;
    }

    @Test
    void exportarCompras_generaHojaDeComprasYResumen() throws Exception {
        List<Compra> compras = List.of(
                compra("Avicola San Jose", "80.00"),
                compra("Carnes del Norte", "120.50"));
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        servicio.exportarCompras(compras, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), salida);

        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(salida.toByteArray()))) {
            assertEquals(2, libro.getNumberOfSheets());
            Sheet hoja = libro.getSheet("Compras");
            assertEquals("Fecha y hora", hoja.getRow(3).getCell(1).getStringCellValue());
            assertEquals("Avicola San Jose", hoja.getRow(4).getCell(2).getStringCellValue());
            assertEquals(120.50, hoja.getRow(5).getCell(4).getNumericCellValue(), 0.001);
            assertEquals("SUM(E5:E6)", hoja.getRow(6).getCell(4).getCellFormula());

            Sheet resumen = libro.getSheet("Resumen por proveedor");
            assertEquals("Avicola San Jose", resumen.getRow(1).getCell(0).getStringCellValue());
            assertEquals(1, resumen.getRow(1).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(80.00, resumen.getRow(1).getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void exportarCompras_rangoInvertido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> servicio.exportarCompras(
                List.of(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), new ByteArrayOutputStream()));
    }

    @Test
    void agruparMontosPorProveedor_agrupaConMultimap() {
        ListMultimap<String, BigDecimal> grupos = ReporteExcelService.agruparMontosPorProveedor(List.of(
                compra("Avicola San Jose", "10.00"),
                compra("Avicola San Jose", "20.00")));
        assertEquals(2, grupos.get("Avicola San Jose").size());
    }
}
