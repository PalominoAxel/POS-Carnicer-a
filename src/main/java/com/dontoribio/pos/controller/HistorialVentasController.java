package com.dontoribio.pos.controller;

import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.dao.impl.VentaDAOImpl;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Venta;
import com.dontoribio.pos.service.TicketGeneratorService;
import com.dontoribio.pos.service.ReporteVentasExcelService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.io.File;
import java.io.IOException;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


/** Controlador del historial de ventas del dia, con opcion para reimprimir el comprobante de cada venta. */
public class HistorialVentasController {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TableView<Venta> tablaVentas;
    @FXML
    private TableColumn<Venta, String> colHora;
    @FXML
    private TableColumn<Venta, String> colCliente;
    @FXML
    private TableColumn<Venta, String> colTotal;
    @FXML
    private TableColumn<Venta, String> colMetodoPago;
    @FXML
    private TableColumn<Venta, String> colComprobante;
    @FXML
    private TableColumn<Venta, Void> colVer;
    @FXML
    private Label labelResumen;

    private final VentaDAO ventaDAO = new VentaDAOImpl();
    private final ReporteVentasExcelService reportesService = new ReporteVentasExcelService();

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFecha().format(FORMATO_HORA)));
        colCliente.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCliente().getNombreRazonSocial()));
        colTotal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "S/ " + data.getValue().getTotal().setScale(2, RoundingMode.HALF_UP)));
        colMetodoPago.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getPagos().stream()
                        .map(Pago::getMetodoPago)
                        .map(Enum::name)
                        .collect(Collectors.joining(" + "))));
        colComprobante.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTipoComprobante().name()));

        colVer.setCellFactory(columna -> new TableCell<>() {
            private final Button boton = new Button("Ver");

            {
                boton.setOnAction(e -> verComprobante(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : boton);
            }
        });

        cargarVentasDelDia();
    }

    private void cargarVentasDelDia() {
        // En lugar de llamar a ventaDAO.listarPorFecha(LocalDate.now()); cargamos datos de prueba:
        java.util.List<Venta> ventas = new java.util.ArrayList<>();

        // Venta 1: Minorista
        Venta v1 = new Venta();
        v1.setId(101L);
        v1.setFecha(java.time.LocalDateTime.now().minusHours(3));
        v1.setTotalesPersistidos(new java.math.BigDecimal("185.50"), new java.math.BigDecimal("185.50"));
        v1.setTipoComprobante(com.dontoribio.pos.model.TipoComprobante.BOLETA);
        com.dontoribio.pos.model.Cliente c1 = new com.dontoribio.pos.model.Cliente();
        c1.setNombreRazonSocial("Juan Pérez");
        v1.setCliente(c1);
        v1.agregarPago(new com.dontoribio.pos.model.Pago(com.dontoribio.pos.model.MetodoPago.EFECTIVO, new java.math.BigDecimal("185.50"), "EFE-01"));
        ventas.add(v1);

        // Venta 2: Mayorista / Restaurante
        Venta v2 = new Venta();
        v2.setId(102L);
        v2.setFecha(java.time.LocalDateTime.now().minusHours(2));
        v2.setTotalesPersistidos(new java.math.BigDecimal("490.00"), new java.math.BigDecimal("490.00"));
        v2.setTipoComprobante(com.dontoribio.pos.model.TipoComprobante.FACTURA);
        com.dontoribio.pos.model.Cliente c2 = new com.dontoribio.pos.model.Cliente();
        c2.setNombreRazonSocial("Restaurante Sabor Criollo");
        v2.setCliente(c2);
        v2.agregarPago(new com.dontoribio.pos.model.Pago(com.dontoribio.pos.model.MetodoPago.YAPE, new java.math.BigDecimal("490.00"), "YAPE-9841"));
        ventas.add(v2);

        // Venta 3: Minimarket
        Venta v3 = new Venta();
        v3.setId(103L);
        v3.setFecha(java.time.LocalDateTime.now().minusMinutes(45));
        v3.setTotalesPersistidos(new java.math.BigDecimal("1250.00"), new java.math.BigDecimal("1250.00"));
        v3.setTipoComprobante(com.dontoribio.pos.model.TipoComprobante.BOLETA);
        com.dontoribio.pos.model.Cliente c3 = new com.dontoribio.pos.model.Cliente();
        c3.setNombreRazonSocial("Minimarket El Sol");
        v3.setCliente(c3);
        v3.agregarPago(new com.dontoribio.pos.model.Pago(com.dontoribio.pos.model.MetodoPago.TARJETA, new java.math.BigDecimal("1250.00"), "OP-4412"));
        ventas.add(v3);

        // Poblar la tabla de JavaFX
        tablaVentas.setItems(FXCollections.observableArrayList(ventas));
        var total = ventas.stream().map(Venta::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        labelResumen.setText(ventas.size() + " venta(s) hoy - Total: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    private void verComprobante(Venta ventaResumen) {
        ventaDAO.buscarVentaCompleta(ventaResumen.getId()).ifPresentOrElse(ventaCompleta -> {
            String ticket = TicketGeneratorService.generar(ventaCompleta, ventaCompleta.getUsuario(), null, null);
            TicketGeneratorService.mostrarVistaPrevia(ticket);
        }, () -> new Alert(Alert.AlertType.ERROR, "No se pudo cargar el comprobante de la venta").showAndWait());
    }
    @FXML
private void onExportarExcelClicked() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Guardar Reporte de Ventas en Excel");
    fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx)", "*.xlsx")
    );
    fileChooser.setInitialFileName("reporte_ventas_dontoribio.xlsx");

    // Tomamos la ventana actual a través de la tabla de ventas
    Stage stage = (Stage) tablaVentas.getScene().getWindow();
    File destino = fileChooser.showSaveDialog(stage);

    if (destino != null) {
        try {
            // tablaVentas.getItems() obtiene la lista de ventas que se está mostrando actualmente
            reportesService.exportarHistorialVentas(tablaVentas.getItems(), destino);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportación Exitosa");
            alert.setHeaderText(null);
            alert.setContentText("El reporte Excel se guardó correctamente en:\n" + destino.getAbsolutePath());
            alert.showAndWait();

        } catch (IOException e) {
            Alert alertError = new Alert(Alert.AlertType.ERROR);
            alertError.setTitle("Error de Exportación");
            alertError.setHeaderText("No se pudo generar el archivo");
            alertError.setContentText("Detalle técnico: " + e.getMessage());
            alertError.showAndWait();
        }
    }
    }
}
