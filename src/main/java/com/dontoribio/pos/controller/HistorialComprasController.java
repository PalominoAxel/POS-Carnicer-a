package com.dontoribio.pos.controller;

import com.dontoribio.pos.dao.CompraDAO;
import com.dontoribio.pos.dao.impl.CompraDAOImpl;
import com.dontoribio.pos.model.Compra;
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

/**
 * Controlador del historial de compras: por rango de fechas (igual que el historial de
 * ventas) o, alternativamente, buscando todas las compras hechas a un proveedor por nombre
 * sin importar la fecha.
 */
public class HistorialComprasController {

    private static final Logger LOG = LoggerFactory.getLogger(HistorialComprasController.class);
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    private TableView<Compra> tablaCompras;
    @FXML
    private TableColumn<Compra, String> colFechaHora;
    @FXML
    private TableColumn<Compra, String> colProveedor;
    @FXML
    private TableColumn<Compra, String> colUsuario;
    @FXML
    private TableColumn<Compra, String> colTotal;
    @FXML
    private TableColumn<Compra, Void> colVer;
    @FXML
    private Label labelResumen;
    @FXML
    private DatePicker datePickerDesde;
    @FXML
    private DatePicker datePickerHasta;
    @FXML
    private TextField campoBuscarProveedor;

    private final CompraDAO compraDAO = new CompraDAOImpl();
    private final ReporteExcelService reporteExcelService = new ReporteExcelService();

    /** Rango y compras actualmente mostradas en la tabla (lo que se exporta a Excel). */
    private LocalDate desdeActual = LocalDate.now();
    private LocalDate hastaActual = LocalDate.now();
    private List<Compra> comprasActuales = List.of();

    @FXML
    private void initialize() {
        colFechaHora.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFecha().format(FORMATO_FECHA_HORA)));
        colProveedor.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getProveedor().getNombreRazonSocial()));
        colUsuario.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getUsuario().getNombreCompleto()));
        colTotal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "S/ " + data.getValue().getTotal().setScale(2, RoundingMode.HALF_UP)));

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
        cargarComprasDeRango(LocalDate.now(), LocalDate.now());
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
        cargarComprasDeRango(desde, hasta);
    }

    @FXML
    private void onIrAHoy() {
        datePickerDesde.setValue(LocalDate.now());
        datePickerHasta.setValue(LocalDate.now());
        cargarComprasDeRango(LocalDate.now(), LocalDate.now());
    }

    /** Busca TODAS las compras (sin importar la fecha) hechas a un proveedor cuyo nombre contenga el texto ingresado. */
    @FXML
    private void onBuscarPorProveedor() {
        String nombre = campoBuscarProveedor.getText() == null ? "" : campoBuscarProveedor.getText().trim();
        if (nombre.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese el nombre del proveedor a buscar").showAndWait();
            return;
        }

        List<Compra> compras = compraDAO.listarPorProveedor(nombre);
        comprasActuales = compras;
        desdeActual = compras.stream().map(c -> c.getFecha().toLocalDate())
                .min(LocalDate::compareTo).orElse(LocalDate.now());
        hastaActual = compras.stream().map(c -> c.getFecha().toLocalDate())
                .max(LocalDate::compareTo).orElse(LocalDate.now());
        tablaCompras.setItems(FXCollections.observableArrayList(compras));
        var total = compras.stream().map(Compra::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        labelResumen.setText(compras.size() + " compra(s) al proveedor \"" + nombre + "\" - Total: S/ "
                + total.setScale(2, RoundingMode.HALF_UP));
    }

    private void cargarComprasDeRango(LocalDate desde, LocalDate hasta) {
        var compras = compraDAO.listarPorRangoFechas(desde, hasta);
        desdeActual = desde;
        hastaActual = hasta;
        comprasActuales = compras;
        tablaCompras.setItems(FXCollections.observableArrayList(compras));
        var total = compras.stream().map(Compra::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String etiquetaPeriodo = desde.equals(hasta)
                ? (desde.equals(LocalDate.now()) ? "hoy" : "el " + desde)
                : "del " + desde + " al " + hasta;
        labelResumen.setText(compras.size() + " compra(s) " + etiquetaPeriodo + " - Total: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    /** Exporta a Excel (.xlsx, Apache POI) las compras que se estan mostrando en la tabla. */
    @FXML
    private void onExportarExcel() {
        if (comprasActuales.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay compras para exportar").showAndWait();
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte de compras");
        selector.setInitialFileName(ReporteExcelService.nombreArchivoSugeridoCompras(desdeActual, hastaActual));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File archivo = selector.showSaveDialog(tablaCompras.getScene().getWindow());
        if (archivo == null) {
            return; // el usuario cancelo
        }
        try {
            reporteExcelService.exportarCompras(comprasActuales, desdeActual, hastaActual, archivo.toPath());
            new Alert(Alert.AlertType.INFORMATION, "Reporte guardado en:\n" + archivo.getAbsolutePath()).showAndWait();
        } catch (IOException e) {
            LOG.error("No se pudo exportar el reporte de compras a {}", archivo, e);
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el archivo: " + e.getMessage()).showAndWait();
        }
    }

    private void verComprobante(Compra compraResumen) {
        compraDAO.buscarCompraCompleta(compraResumen.getId()).ifPresentOrElse(compraCompleta -> {
            String ticket = TicketGeneratorService.generarCompra(compraCompleta, compraCompleta.getUsuario());
            TicketGeneratorService.mostrarVistaPrevia(ticket);
        }, () -> new Alert(Alert.AlertType.ERROR, "No se pudo cargar el comprobante de la compra").showAndWait());
    }
}
