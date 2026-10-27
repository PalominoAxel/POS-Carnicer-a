package com.dontoribio.pos.controller;

import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.dao.impl.VentaDAOImpl;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Venta;
import com.dontoribio.pos.service.ReporteExcelService;
import com.dontoribio.pos.service.TicketGeneratorService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Controlador del historial de ventas por rango de fechas, con opcion para reimprimir el comprobante de cada venta. */
public class HistorialVentasController {

    private static final Logger LOG = LoggerFactory.getLogger(HistorialVentasController.class);
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    private TableView<Venta> tablaVentas;
    @FXML
    private TableColumn<Venta, String> colFechaHora;
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
    @FXML
    private DatePicker datePickerDesde;
    @FXML
    private DatePicker datePickerHasta;

    private final VentaDAO ventaDAO = new VentaDAOImpl();
    private final ReporteExcelService reporteExcelService = new ReporteExcelService();

    /** Rango y ventas actualmente mostrados en la tabla (lo que se exporta a Excel). */
    private LocalDate desdeActual = LocalDate.now();
    private LocalDate hastaActual = LocalDate.now();
    private List<Venta> ventasActuales = List.of();

    @FXML
    private void initialize() {
        colFechaHora.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFecha().format(FORMATO_FECHA_HORA)));
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

        datePickerDesde.setValue(LocalDate.now());
        datePickerHasta.setValue(LocalDate.now());
        cargarVentasDeRango(LocalDate.now(), LocalDate.now());
    }

    @FXML
    private void onBuscarRango() {
        LocalDate desde = datePickerDesde.getValue();
        LocalDate hasta = datePickerHasta.getValue();
        if (desde == null || hasta == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione la fecha de inicio y la fecha de fin").showAndWait();
            return;
        }
        if (desde.isAfter(hasta)) {
            new Alert(Alert.AlertType.WARNING, "La fecha de inicio no puede ser posterior a la fecha de fin").showAndWait();
            return;
        }
        cargarVentasDeRango(desde, hasta);
    }

    @FXML
    private void onIrAHoy() {
        datePickerDesde.setValue(LocalDate.now());
        datePickerHasta.setValue(LocalDate.now());
        cargarVentasDeRango(LocalDate.now(), LocalDate.now());
    }

    private void cargarVentasDeRango(LocalDate desde, LocalDate hasta) {
        var ventas = ventaDAO.listarPorRangoFechas(desde, hasta);
        desdeActual = desde;
        hastaActual = hasta;
        ventasActuales = ventas;
        tablaVentas.setItems(FXCollections.observableArrayList(ventas));
        var total = ventas.stream().map(Venta::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String etiquetaPeriodo = desde.equals(hasta)
                ? (desde.equals(LocalDate.now()) ? "hoy" : "el " + desde)
                : "del " + desde + " al " + hasta;
        labelResumen.setText(ventas.size() + " venta(s) " + etiquetaPeriodo + " - Total: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    /** Exporta a Excel (.xlsx, Apache POI) las ventas que se estan mostrando en la tabla. */
    @FXML
    private void onExportarExcel() {
        if (ventasActuales.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay ventas en el rango seleccionado para exportar").showAndWait();
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte de ventas");
        selector.setInitialFileName(ReporteExcelService.nombreArchivoSugerido(desdeActual, hastaActual));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File archivo = selector.showSaveDialog(tablaVentas.getScene().getWindow());
        if (archivo == null) {
            return; // el usuario cancelo
        }
        try {
            reporteExcelService.exportarVentas(ventasActuales, desdeActual, hastaActual, archivo.toPath());
            new Alert(Alert.AlertType.INFORMATION, "Reporte guardado en:\n" + archivo.getAbsolutePath()).showAndWait();
        } catch (IOException e) {
            LOG.error("No se pudo exportar el reporte de ventas a {}", archivo, e);
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el archivo: " + e.getMessage()).showAndWait();
        }
    }

    private void verComprobante(Venta ventaResumen) {
        ventaDAO.buscarVentaCompleta(ventaResumen.getId()).ifPresentOrElse(ventaCompleta -> {
            String ticket = TicketGeneratorService.generar(ventaCompleta, ventaCompleta.getUsuario(), null, null);
            TicketGeneratorService.mostrarVistaPrevia(ticket);
        }, () -> new Alert(Alert.AlertType.ERROR, "No se pudo cargar el comprobante de la venta").showAndWait());
    }
}
