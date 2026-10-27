package com.dontoribio.pos.service;

import com.dontoribio.pos.model.Compra;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Venta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Genera reportes de ventas en formato Excel (.xlsx) usando Apache POI.
 * <p>
 * El libro tiene dos hojas:
 * <ol>
 *     <li><b>Ventas</b>: una fila por venta del rango, con fila de total al final.</li>
 *     <li><b>Resumen por pago</b>: cantidad y monto agrupado por metodo de pago
 *     (la agrupacion se hace con un {@code ListMultimap} de Guava).</li>
 * </ol>
 * Es parte de la capa de servicio (Modelo en MVC): no conoce JavaFX, solo recibe datos
 * y escribe el archivo, por eso se puede probar sin interfaz grafica.
 */
public class ReporteExcelService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FORMATO_MONEDA = "\"S/\" #,##0.00";
    private static final String FORMATO_FECHA_EXCEL = "dd/mm/yyyy hh:mm";

    /** Encabezados de la hoja de ventas; ImmutableList evita que se modifiquen por error. */
    static final ImmutableList<String> ENCABEZADOS_VENTAS = ImmutableList.of(
            "N.", "Fecha y hora", "Cliente", "Cajero", "Comprobante", "Metodo de pago", "Total");

    /** Escribe el reporte de ventas en la ruta indicada (crea o reemplaza el archivo). */
    public void exportarVentas(List<Venta> ventas, LocalDate desde, LocalDate hasta, Path destino) throws IOException {
        Preconditions.checkNotNull(destino, "Debe indicar el archivo de destino");
        try (OutputStream salida = Files.newOutputStream(destino)) {
            exportarVentas(ventas, desde, hasta, salida);
        }
    }

    /** Escribe el reporte en cualquier flujo de salida (util para pruebas unitarias). */
    public void exportarVentas(List<Venta> ventas, LocalDate desde, LocalDate hasta, OutputStream salida) throws IOException {
        Preconditions.checkNotNull(ventas, "La lista de ventas no puede ser nula");
        Preconditions.checkNotNull(desde, "Fecha 'desde' requerida");
        Preconditions.checkNotNull(hasta, "Fecha 'hasta' requerida");
        Preconditions.checkArgument(!desde.isAfter(hasta),
                "La fecha de inicio (%s) no puede ser posterior a la fecha de fin (%s)", desde, hasta);
        Preconditions.checkNotNull(salida, "Flujo de salida requerido");

        try (Workbook libro = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(libro);
            crearHojaVentas(libro, estilos, ventas, desde, hasta);
            crearHojaResumenPorPago(libro, estilos, ventas);
            libro.write(salida);
        }
    }

    /** Nombre de archivo sugerido, p. ej. "ventas_2026-10-01_a_2026-10-20.xlsx". */
    public static String nombreArchivoSugerido(LocalDate desde, LocalDate hasta) {
        return desde.equals(hasta)
                ? "ventas_" + desde + ".xlsx"
                : "ventas_" + desde + "_a_" + hasta + ".xlsx";
    }

    /** Escribe el reporte de compras en la ruta indicada (crea o reemplaza el archivo). */
    public void exportarCompras(List<Compra> compras, LocalDate desde, LocalDate hasta, Path destino) throws IOException {
        Preconditions.checkNotNull(destino, "Debe indicar el archivo de destino");
        try (OutputStream salida = Files.newOutputStream(destino)) {
            exportarCompras(compras, desde, hasta, salida);
        }
    }

    /** Escribe el reporte de compras en cualquier flujo de salida (util para pruebas unitarias). */
    public void exportarCompras(List<Compra> compras, LocalDate desde, LocalDate hasta, OutputStream salida) throws IOException {
        Preconditions.checkNotNull(compras, "La lista de compras no puede ser nula");
        Preconditions.checkNotNull(desde, "Fecha 'desde' requerida");
        Preconditions.checkNotNull(hasta, "Fecha 'hasta' requerida");
        Preconditions.checkArgument(!desde.isAfter(hasta),
                "La fecha de inicio (%s) no puede ser posterior a la fecha de fin (%s)", desde, hasta);
        Preconditions.checkNotNull(salida, "Flujo de salida requerido");

        try (Workbook libro = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(libro);
            crearHojaCompras(libro, estilos, compras, desde, hasta);
            crearHojaResumenPorProveedor(libro, estilos, compras);
            libro.write(salida);
        }
    }

    /** Nombre de archivo sugerido, p. ej. "compras_2026-10-01_a_2026-10-20.xlsx". */
    public static String nombreArchivoSugeridoCompras(LocalDate desde, LocalDate hasta) {
        return desde.equals(hasta)
                ? "compras_" + desde + ".xlsx"
                : "compras_" + desde + "_a_" + hasta + ".xlsx";
    }

    // ---------------------------------------------------------------------------------
    // Hoja 1: detalle de ventas
    // ---------------------------------------------------------------------------------

    private void crearHojaVentas(Workbook libro, Estilos estilos, List<Venta> ventas, LocalDate desde, LocalDate hasta) {
        Sheet hoja = libro.createSheet("Ventas");
        int ultimaColumna = ENCABEZADOS_VENTAS.size() - 1;

        Row titulo = hoja.createRow(0);
        crearCelda(titulo, 0, "Don Toribio S.A.C. - Reporte de ventas", estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaColumna));

        Row periodo = hoja.createRow(1);
        crearCelda(periodo, 0, "Periodo: " + desde.format(FORMATO_FECHA) + " al " + hasta.format(FORMATO_FECHA), null);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, ultimaColumna));

        Row encabezado = hoja.createRow(3);
        for (int i = 0; i < ENCABEZADOS_VENTAS.size(); i++) {
            crearCelda(encabezado, i, ENCABEZADOS_VENTAS.get(i), estilos.encabezado);
        }

        int filaActual = 4;
        BigDecimal totalGeneral = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            Row fila = hoja.createRow(filaActual);
            fila.createCell(0).setCellValue(filaActual - 3);

            Cell celdaFecha = fila.createCell(1);
            if (venta.getFecha() != null) {
                celdaFecha.setCellValue(venta.getFecha());
                celdaFecha.setCellStyle(estilos.fecha);
            }

            crearCelda(fila, 2, venta.getCliente() == null ? "" : Strings.nullToEmpty(venta.getCliente().getNombreRazonSocial()), null);
            crearCelda(fila, 3, venta.getUsuario() == null ? "" : Strings.nullToEmpty(venta.getUsuario().getNombreCompleto()), null);
            crearCelda(fila, 4, venta.getTipoComprobante() == null ? "" : venta.getTipoComprobante().name(), null);
            crearCelda(fila, 5, describirPagos(venta.getPagos()), null);

            BigDecimal total = venta.getTotal() == null ? BigDecimal.ZERO : venta.getTotal();
            Cell celdaTotal = fila.createCell(6);
            celdaTotal.setCellValue(total.doubleValue());
            celdaTotal.setCellStyle(estilos.moneda);

            totalGeneral = totalGeneral.add(total);
            filaActual++;
        }

        Row filaTotal = hoja.createRow(filaActual);
        crearCelda(filaTotal, 5, "TOTAL", estilos.totalEtiqueta);
        Cell celdaTotalGeneral = filaTotal.createCell(6);
        if (ventas.isEmpty()) {
            celdaTotalGeneral.setCellValue(0);
        } else {
            // Formula de Excel: si el usuario corrige un monto en el archivo, el total se recalcula solo.
            celdaTotalGeneral.setCellFormula("SUM(G5:G" + filaActual + ")");
        }
        celdaTotalGeneral.setCellStyle(estilos.totalMoneda);

        hoja.createFreezePane(0, 4);
        for (int i = 0; i <= ultimaColumna; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    /** Une los metodos de pago de una venta con Joiner de Guava: "EFECTIVO + YAPE". */
    static String describirPagos(List<Pago> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return "";
        }
        return Joiner.on(" + ").skipNulls().join(
                pagos.stream().map(p -> p.getMetodoPago() == null ? null : p.getMetodoPago().name())
                        .collect(Collectors.toList()));
    }

    // ---------------------------------------------------------------------------------
    // Hoja 2: resumen por metodo de pago
    // ---------------------------------------------------------------------------------

    private void crearHojaResumenPorPago(Workbook libro, Estilos estilos, List<Venta> ventas) {
        Sheet hoja = libro.createSheet("Resumen por pago");

        Row encabezado = hoja.createRow(0);
        crearCelda(encabezado, 0, "Metodo de pago", estilos.encabezado);
        crearCelda(encabezado, 1, "N. de pagos", estilos.encabezado);
        crearCelda(encabezado, 2, "Monto", estilos.encabezado);

        ListMultimap<String, BigDecimal> montosPorMetodo = agruparMontosPorMetodo(ventas);

        int filaActual = 1;
        for (String metodo : montosPorMetodo.keySet()) {
            List<BigDecimal> montos = montosPorMetodo.get(metodo);
            BigDecimal suma = montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            Row fila = hoja.createRow(filaActual++);
            crearCelda(fila, 0, metodo, null);
            fila.createCell(1).setCellValue(montos.size());
            Cell celdaMonto = fila.createCell(2);
            celdaMonto.setCellValue(suma.doubleValue());
            celdaMonto.setCellStyle(estilos.moneda);
        }

        for (int i = 0; i < 3; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    /**
     * Agrupa todos los pagos por metodo usando un Multimap de Guava
     * (una clave con varios valores, sin tener que escribir Map&lt;String, List&lt;BigDecimal&gt;&gt; a mano).
     * Las claves quedan ordenadas alfabeticamente.
     */
    static ListMultimap<String, BigDecimal> agruparMontosPorMetodo(List<Venta> ventas) {
        ListMultimap<String, BigDecimal> resultado = MultimapBuilder.treeKeys().arrayListValues().build();
        for (Venta venta : ventas) {
            for (Pago pago : venta.getPagos()) {
                if (pago.getMetodoPago() != null && pago.getMonto() != null) {
                    resultado.put(pago.getMetodoPago().name(), pago.getMonto());
                }
            }
        }
        return resultado;
    }

    // ---------------------------------------------------------------------------------
    // Hoja 1 (compras): detalle de compras
    // ---------------------------------------------------------------------------------

    /** Encabezados de la hoja de compras; ImmutableList evita que se modifiquen por error. */
    static final ImmutableList<String> ENCABEZADOS_COMPRAS = ImmutableList.of(
            "N.", "Fecha y hora", "Proveedor", "Registrado por", "Total");

    private void crearHojaCompras(Workbook libro, Estilos estilos, List<Compra> compras, LocalDate desde, LocalDate hasta) {
        Sheet hoja = libro.createSheet("Compras");
        int ultimaColumna = ENCABEZADOS_COMPRAS.size() - 1;

        Row titulo = hoja.createRow(0);
        crearCelda(titulo, 0, "Don Toribio S.A.C. - Reporte de compras", estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaColumna));

        Row periodo = hoja.createRow(1);
        crearCelda(periodo, 0, "Periodo: " + desde.format(FORMATO_FECHA) + " al " + hasta.format(FORMATO_FECHA), null);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, ultimaColumna));

        Row encabezado = hoja.createRow(3);
        for (int i = 0; i < ENCABEZADOS_COMPRAS.size(); i++) {
            crearCelda(encabezado, i, ENCABEZADOS_COMPRAS.get(i), estilos.encabezado);
        }

        int filaActual = 4;
        BigDecimal totalGeneral = BigDecimal.ZERO;
        for (Compra compra : compras) {
            Row fila = hoja.createRow(filaActual);
            fila.createCell(0).setCellValue(filaActual - 3);

            Cell celdaFecha = fila.createCell(1);
            if (compra.getFecha() != null) {
                celdaFecha.setCellValue(compra.getFecha());
                celdaFecha.setCellStyle(estilos.fecha);
            }

            crearCelda(fila, 2, compra.getProveedor() == null ? "" : Strings.nullToEmpty(compra.getProveedor().getNombreRazonSocial()), null);
            crearCelda(fila, 3, compra.getUsuario() == null ? "" : Strings.nullToEmpty(compra.getUsuario().getNombreCompleto()), null);

            BigDecimal total = compra.getTotal() == null ? BigDecimal.ZERO : compra.getTotal();
            Cell celdaTotal = fila.createCell(4);
            celdaTotal.setCellValue(total.doubleValue());
            celdaTotal.setCellStyle(estilos.moneda);

            totalGeneral = totalGeneral.add(total);
            filaActual++;
        }

        Row filaTotal = hoja.createRow(filaActual);
        crearCelda(filaTotal, 3, "TOTAL", estilos.totalEtiqueta);
        Cell celdaTotalGeneral = filaTotal.createCell(4);
        if (compras.isEmpty()) {
            celdaTotalGeneral.setCellValue(0);
        } else {
            celdaTotalGeneral.setCellFormula("SUM(E5:E" + filaActual + ")");
        }
        celdaTotalGeneral.setCellStyle(estilos.totalMoneda);

        hoja.createFreezePane(0, 4);
        for (int i = 0; i <= ultimaColumna; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    // ---------------------------------------------------------------------------------
    // Hoja 2 (compras): resumen por proveedor
    // ---------------------------------------------------------------------------------

    private void crearHojaResumenPorProveedor(Workbook libro, Estilos estilos, List<Compra> compras) {
        Sheet hoja = libro.createSheet("Resumen por proveedor");

        Row encabezado = hoja.createRow(0);
        crearCelda(encabezado, 0, "Proveedor", estilos.encabezado);
        crearCelda(encabezado, 1, "N. de compras", estilos.encabezado);
        crearCelda(encabezado, 2, "Monto", estilos.encabezado);

        ListMultimap<String, BigDecimal> montosPorProveedor = agruparMontosPorProveedor(compras);

        int filaActual = 1;
        for (String proveedor : montosPorProveedor.keySet()) {
            List<BigDecimal> montos = montosPorProveedor.get(proveedor);
            BigDecimal suma = montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            Row fila = hoja.createRow(filaActual++);
            crearCelda(fila, 0, proveedor, null);
            fila.createCell(1).setCellValue(montos.size());
            Cell celdaMonto = fila.createCell(2);
            celdaMonto.setCellValue(suma.doubleValue());
            celdaMonto.setCellStyle(estilos.moneda);
        }

        for (int i = 0; i < 3; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    /** Agrupa los montos de cada compra por nombre de proveedor, igual que {@link #agruparMontosPorMetodo}. */
    static ListMultimap<String, BigDecimal> agruparMontosPorProveedor(List<Compra> compras) {
        ListMultimap<String, BigDecimal> resultado = MultimapBuilder.treeKeys().arrayListValues().build();
        for (Compra compra : compras) {
            if (compra.getProveedor() != null && compra.getTotal() != null) {
                resultado.put(compra.getProveedor().getNombreRazonSocial(), compra.getTotal());
            }
        }
        return resultado;
    }

    // ---------------------------------------------------------------------------------
    // Utilidades de POI
    // ---------------------------------------------------------------------------------

    private static void crearCelda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
    }

    /** Estilos reutilizables: en POI conviene crearlos una vez por libro, no por celda. */
    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle encabezado;
        final CellStyle moneda;
        final CellStyle fecha;
        final CellStyle totalEtiqueta;
        final CellStyle totalMoneda;

        Estilos(Workbook libro) {
            DataFormat formato = libro.createDataFormat();

            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            titulo = libro.createCellStyle();
            titulo.setFont(fuenteTitulo);

            Font fuenteNegrita = libro.createFont();
            fuenteNegrita.setBold(true);

            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            encabezado = libro.createCellStyle();
            encabezado.setFont(fuenteEncabezado);
            encabezado.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setBorderBottom(BorderStyle.THIN);

            moneda = libro.createCellStyle();
            moneda.setDataFormat(formato.getFormat(FORMATO_MONEDA));

            fecha = libro.createCellStyle();
            fecha.setDataFormat(formato.getFormat(FORMATO_FECHA_EXCEL));

            totalEtiqueta = libro.createCellStyle();
            totalEtiqueta.setFont(fuenteNegrita);
            totalEtiqueta.setBorderTop(BorderStyle.THIN);

            totalMoneda = libro.createCellStyle();
            totalMoneda.cloneStyleFrom(moneda);
            totalMoneda.setFont(fuenteNegrita);
            totalMoneda.setBorderTop(BorderStyle.THIN);
        }
    }
}
