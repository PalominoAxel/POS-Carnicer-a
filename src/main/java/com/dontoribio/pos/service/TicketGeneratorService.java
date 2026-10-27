package com.dontoribio.pos.service;

import com.dontoribio.pos.model.Compra;
import com.dontoribio.pos.model.DetalleCompra;
import com.dontoribio.pos.model.DetalleVenta;
import com.dontoribio.pos.model.TipoComprobante;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.model.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera el texto de la boleta de venta en formato monoespaciado,
 * listo para mostrarse en pantalla o enviarse a una ticketera termica de 80mm (42 columnas).
 */
public final class TicketGeneratorService {

    private static final int ANCHO = 42;
    private static final String RAZON_SOCIAL = "DON TORIBIO S.A.C.";
    private static final String RUC = "RUC: 20123456789";
    private static final String DIRECCION = "Av. Los Carniceros 123 - Lima";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private TicketGeneratorService() {
    }

    /**
     * Abre una ventana de vista previa con el texto del ticket (fuente monoespaciada)
     * y un boton "Imprimir" que envia el contenido a la impresora predeterminada del sistema.
     */
    public static void mostrarVistaPrevia(String ticketTexto) {
        javafx.scene.control.TextArea areaTicket = new javafx.scene.control.TextArea(ticketTexto);
        areaTicket.setEditable(false);
        areaTicket.setWrapText(false);
        areaTicket.setFont(javafx.scene.text.Font.font("Consolas", 12));
        areaTicket.setPrefColumnCount(ANCHO);

        javafx.scene.control.Button botonImprimir = new javafx.scene.control.Button("Imprimir");
        javafx.scene.control.Button botonCerrar = new javafx.scene.control.Button("Cerrar");

        javafx.scene.layout.HBox botones = new javafx.scene.layout.HBox(20.0, botonImprimir, botonCerrar);
        areaTicket.setPrefHeight(620);
        javafx.scene.layout.VBox raiz = new javafx.scene.layout.VBox(20.0, areaTicket, botones);
        raiz.setPadding(new javafx.geometry.Insets(20.0));

        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.setTitle("Vista previa - Boleta de venta");
        ventana.setScene(new javafx.scene.Scene(raiz, 440, 720));

        botonImprimir.setOnAction(e -> imprimir(ticketTexto, ventana));
        botonCerrar.setOnAction(e -> ventana.close());

        ventana.show();
    }

    private static void imprimir(String ticketTexto, javafx.stage.Stage ventanaPropietaria) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,
                    "No se encontro una impresora disponible").showAndWait();
            return;
        }
        if (!job.showPrintDialog(ventanaPropietaria)) {
            return;
        }
        javafx.scene.text.Text nodoTexto = new javafx.scene.text.Text(ticketTexto);
        nodoTexto.setFont(javafx.scene.text.Font.font("Consolas", 10));
        boolean exito = job.printPage(nodoTexto);
        if (exito) {
            job.endJob();
        }
    }

    /** Genera el texto completo de la boleta a partir de una venta ya registrada. */
    public static String generar(Venta venta, Usuario cajero, BigDecimal montoRecibido, BigDecimal vuelto) {
        StringBuilder sb = new StringBuilder();

        sb.append(centrar(RAZON_SOCIAL)).append('\n');
        sb.append(centrar(RUC)).append('\n');
        sb.append(centrar(DIRECCION)).append('\n');
        LocalDateTime fecha = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();
        sb.append(centrar(fecha.format(FORMATO_FECHA))).append('\n');
        sb.append(centrar(etiquetaComprobante(venta.getTipoComprobante()))).append('\n');
        sb.append(linea()).append('\n');

        String nombreCliente = venta.getCliente() != null ? venta.getCliente().getNombreRazonSocial() : "Cliente varios";
        String tipoCliente = venta.getCliente() != null && venta.getCliente().getTipoCliente() != null
                ? venta.getCliente().getTipoCliente().name() : "MINORISTA";
        sb.append("Cliente: ").append(nombreCliente).append('\n');
        sb.append("Tipo: ").append(tipoCliente).append('\n');
        if (venta.getDocumentoCliente() != null && !venta.getDocumentoCliente().isBlank()) {
            String etiquetaDoc = venta.getTipoComprobante() == TipoComprobante.FACTURA ? "RUC: " : "DNI: ";
            sb.append(etiquetaDoc).append(venta.getDocumentoCliente()).append('\n');
        }
        sb.append("Atendido por: ").append(cajero != null ? cajero.getNombreCompleto() : "-").append('\n');
        sb.append(linea()).append('\n');

        sb.append(fila("Producto", "Peso", "P.Unit", "Importe")).append('\n');
        sb.append(linea()).append('\n');
        for (DetalleVenta detalle : venta.getDetalles()) {
            String nombre = detalle.getProducto().getNombre();
            for (String parte : dividirTexto(nombre, ANCHO)) {
                sb.append(parte).append('\n');
            }
            sb.append(fila("",
                    detalle.getPesoKg().stripTrailingZeros().toPlainString() + "kg",
                    detalle.getPrecioUnitario().toPlainString(),
                    detalle.getImporte().toPlainString())).append('\n');
        }
        sb.append(linea()).append('\n');

        sb.append(filaMonto("Subtotal:", venta.getSubtotal())).append('\n');
        sb.append(filaMonto("TOTAL:", venta.getTotal())).append('\n');
        if (montoRecibido != null) {
            sb.append(filaMonto("Recibido:", montoRecibido)).append('\n');
        }
        if (vuelto != null) {
            sb.append(filaMonto("Vuelto:", vuelto)).append('\n');
        }
        sb.append(linea()).append('\n');

        sb.append(centrar("Gracias por su compra!")).append('\n');
        sb.append(centrar("Vuelva pronto")).append('\n');

        return sb.toString();
    }

    /** Genera el texto del comprobante de una compra (ingreso de mercaderia) ya registrada. */
    public static String generarCompra(Compra compra, Usuario usuario) {
        StringBuilder sb = new StringBuilder();

        sb.append(centrar(RAZON_SOCIAL)).append('\n');
        sb.append(centrar(RUC)).append('\n');
        sb.append(centrar(DIRECCION)).append('\n');
        LocalDateTime fecha = compra.getFecha() != null ? compra.getFecha() : LocalDateTime.now();
        sb.append(centrar(fecha.format(FORMATO_FECHA))).append('\n');
        sb.append(centrar("Tipo de operacion: COMPRA")).append('\n');
        sb.append(linea()).append('\n');

        String nombreProveedor = compra.getProveedor() != null ? compra.getProveedor().getNombreRazonSocial() : "-";
        sb.append("Proveedor: ").append(nombreProveedor).append('\n');
        sb.append("Registrado por: ").append(usuario != null ? usuario.getNombreCompleto() : "-").append('\n');
        sb.append(linea()).append('\n');

        sb.append(fila("Producto", "Peso", "Costo", "Importe")).append('\n');
        sb.append(linea()).append('\n');
        for (DetalleCompra detalle : compra.getDetalles()) {
            String nombre = detalle.getProducto().getNombre();
            for (String parte : dividirTexto(nombre, ANCHO)) {
                sb.append(parte).append('\n');
            }
            sb.append(fila("",
                    detalle.getPesoKg().stripTrailingZeros().toPlainString() + "kg",
                    detalle.getCostoUnitario().toPlainString(),
                    detalle.getImporte().toPlainString())).append('\n');
        }
        sb.append(linea()).append('\n');

        sb.append(filaMonto("TOTAL:", compra.getTotal())).append('\n');
        sb.append(linea()).append('\n');

        sb.append(centrar("Ingreso de mercaderia registrado")).append('\n');

        return sb.toString();
    }

    private static String etiquetaComprobante(TipoComprobante tipo) {
        if (tipo == null) {
            return "TICKET SIMPLE";
        }
        return switch (tipo) {
            case BOLETA -> "BOLETA DE VENTA";
            case FACTURA -> "FACTURA";
            default -> "TICKET SIMPLE";
        };
    }

    private static String linea() {
        return "-".repeat(ANCHO);
    }

    private static String centrar(String texto) {
        if (texto.length() >= ANCHO) {
            return texto.substring(0, ANCHO);
        }
        int espacios = (ANCHO - texto.length()) / 2;
        return " ".repeat(espacios) + texto;
    }

    /** Distribuye 4 campos en el ancho del ticket: nombre a la izquierda, resto alineados a la derecha. */
    private static String fila(String producto, String peso, String precio, String importe) {
        String colProducto = pad(producto, 14);
        String colPeso = padIzquierda(peso, 8);
        String colPrecio = padIzquierda(precio, 8);
        String colImporte = padIzquierda(importe, ANCHO - 14 - 8 - 8);
        return colProducto + colPeso + colPrecio + colImporte;
    }

    private static String filaMonto(String etiqueta, BigDecimal monto) {
        String valor = "S/ " + monto.setScale(2, java.math.RoundingMode.HALF_UP);
        String espacio = " ".repeat(Math.max(1, ANCHO - etiqueta.length() - valor.length()));
        return etiqueta + espacio + valor;
    }

    private static String pad(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        return texto + " ".repeat(ancho - texto.length());
    }

    private static String padIzquierda(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(texto.length() - ancho);
        }
        return " ".repeat(ancho - texto.length()) + texto;
    }

    private static java.util.List<String> dividirTexto(String texto, int ancho) {
        java.util.List<String> partes = new java.util.ArrayList<>();
        for (int i = 0; i < texto.length(); i += ancho) {
            partes.add(texto.substring(i, Math.min(i + ancho, texto.length())));
        }
        return partes;
    }
}
