package com.dontoribio.pos.controller;

import com.dontoribio.pos.dao.VentaDAO;
import com.dontoribio.pos.dao.impl.VentaDAOImpl;
import com.dontoribio.pos.model.Pago;
import com.dontoribio.pos.model.Venta;
import com.dontoribio.pos.service.TicketGeneratorService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/** Controlador del historial de ventas por rango de fechas, con opcion para reimprimir el comprobante de cada venta. */
public class HistorialVentasController {

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
        tablaVentas.setItems(FXCollections.observableArrayList(ventas));
        var total = ventas.stream().map(Venta::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String etiquetaPeriodo = desde.equals(hasta)
                ? (desde.equals(LocalDate.now()) ? "hoy" : "el " + desde)
                : "del " + desde + " al " + hasta;
        labelResumen.setText(ventas.size() + " venta(s) " + etiquetaPeriodo + " - Total: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    private void verComprobante(Venta ventaResumen) {
        ventaDAO.buscarVentaCompleta(ventaResumen.getId()).ifPresentOrElse(ventaCompleta -> {
            String ticket = TicketGeneratorService.generar(ventaCompleta, ventaCompleta.getUsuario(), null, null);
            TicketGeneratorService.mostrarVistaPrevia(ticket);
        }, () -> new Alert(Alert.AlertType.ERROR, "No se pudo cargar el comprobante de la venta").showAndWait());
    }
}
