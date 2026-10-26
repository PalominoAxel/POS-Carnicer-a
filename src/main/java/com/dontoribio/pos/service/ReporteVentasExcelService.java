package com.dontoribio.pos.service;

import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Venta;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReporteVentasExcelService {

    private static final String[] CABECERAS = {
            "ID Venta", "Fecha / Hora", "Comprobante", "Cliente", "Método de Pago", "Total (S/)"
    };
    private static final int[] ANCHOS = {12, 22, 18, 28, 20, 16};

    /**
     * Prevención de inyección de fórmulas (CWE-1236).
     */
    private String neutralizarTexto(String texto) {
        if (texto == null) return "";
        if (StringUtils.startsWithAny(texto, "=", "+", "-", "@")) {
            return " " + texto;
        }
        return texto;
    }

    public void exportarHistorialVentas(List<Venta> ventas, File archivoDestino) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             OutputStream salida = new FileOutputStream(archivoDestino)) {

            Sheet hoja = libro.createSheet("Historial de Ventas");

            // 1. Estilo de Cabecera (Texto blanco, fondo azul oscuro)
            Font fuenteCabecera = libro.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(fuenteCabecera);
            estiloCabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloCabecera.setAlignment(HorizontalAlignment.CENTER);

            // 2. Estilo monetario (#,##0.00)
            CellStyle estiloMoneda = libro.createCellStyle();
            estiloMoneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            // 3. Fila 0 de Cabecera
            Row filaCab = hoja.createRow(0);
            for (int i = 0; i < CABECERAS.length; i++) {
                Cell celda = filaCab.createCell(i);
                celda.setCellValue(CABECERAS[i]);
                celda.setCellStyle(estiloCabecera);
                hoja.setColumnWidth(i, ANCHOS[i] * 256);
            }

            // 4. Llenar filas con los datos reales de Venta.java
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int numFila = 1;
            for (Venta v : ventas) {
                Row r = hoja.createRow(numFila);

                // ID Venta
                r.createCell(0).setCellValue(v.getId() != null ? v.getId() : 0L);

                // Fecha y Hora
                String fechaStr = v.getFecha() != null ? v.getFecha().format(formatter) : "";
                r.createCell(1).setCellValue(fechaStr);

                // Comprobante
                String comprobante = v.getTipoComprobante() != null ? v.getTipoComprobante().name() : "TICKET";
                r.createCell(2).setCellValue(neutralizarTexto(comprobante));

                // Cliente
                String cliente = (v.getCliente() != null && v.getCliente().getNombreRazonSocial() != null)
                        ? v.getCliente().getNombreRazonSocial()
                        : "PÚBLICO GENERAL";
                r.createCell(3).setCellValue(neutralizarTexto(cliente));

                // Métodos de Pago
                String metodosPago = (v.getPagos() != null && !v.getPagos().isEmpty())
                        ? v.getPagos().stream()
                                .map(Pago::getMetodoPago)
                                .map(Enum::name)
                                .collect(Collectors.joining(" + "))
                        : "EFECTIVO";
                r.createCell(4).setCellValue(neutralizarTexto(metodosPago));

                // Monto Total
                Cell cTotal = r.createCell(5);
                cTotal.setCellValue(v.getTotal() != null ? v.getTotal().doubleValue() : 0.0);
                cTotal.setCellStyle(estiloMoneda);

                numFila++;
            }

            // 5. Fila Final con Fórmula SUM nativa de Excel
            Row filaTotal = hoja.createRow(numFila);
            Cell celdaEtiqueta = filaTotal.createCell(4);
            celdaEtiqueta.setCellValue("TOTAL RECAUDADO:");

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloTotal = libro.createCellStyle();
            estiloTotal.setFont(negrita);
            celdaEtiqueta.setCellStyle(estiloTotal);

            Cell celdaSuma = filaTotal.createCell(5);
            if (numFila > 1) {
                celdaSuma.setCellFormula("SUM(F2:F" + numFila + ")");
            } else {
                celdaSuma.setCellValue(0.0);
            }
            celdaSuma.setCellStyle(estiloMoneda);

            // 6. Usabilidad: Congelar fila 0 y habilitar filtros automáticos
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, CABECERAS.length - 1));
            libro.setForceFormulaRecalculation(true);

            libro.write(salida);
        }
    }
}