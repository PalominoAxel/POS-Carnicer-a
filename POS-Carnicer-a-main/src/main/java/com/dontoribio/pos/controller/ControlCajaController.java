package com.dontoribio.pos.controller;

import com.dontoribio.pos.model.Caja;
import com.dontoribio.pos.model.MetodoPago;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.service.CajaService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de la pantalla de Control de Caja: muestra el resumen del turno
 * (monto inicial, ventas por metodo de pago, efectivo esperado) y permite abrir/cerrar la caja.
 */
public class ControlCajaController {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label labelEstadoTurno;
    @FXML
    private Label labelInfoTurno;
    @FXML
    private Label labelMontoInicial;
    @FXML
    private Label labelVentasEfectivo;
    @FXML
    private Label labelYape;
    @FXML
    private Label labelPlin;
    @FXML
    private Label labelTarjeta;
    @FXML
    private Label labelTransferencia;
    @FXML
    private Label labelEfectivoEsperado;
    @FXML
    private Button botonAccion;

    private final CajaService cajaService = new CajaService();
    private Usuario usuarioSesion;
    private Caja cajaActual;

    public void setUsuarioSesion(Usuario usuario) {
        this.usuarioSesion = usuario;
    }

    /** Recibe la caja del turno activo (o null si no hay ninguna abierta) desde MainPOSController. */
    public void setCajaActual(Caja caja) {
        this.cajaActual = caja;
        actualizarVista();
    }

    /** Usado por MainPOSController para sincronizar su estado al cerrar esta ventana. */
    public Caja getCajaActual() {
        return cajaActual;
    }

    private void actualizarVista() {
        if (cajaActual == null) {
            labelEstadoTurno.setText("CAJA CERRADA");
            labelEstadoTurno.getStyleClass().removeAll("estado-caja-abierta");
            if (!labelEstadoTurno.getStyleClass().contains("estado-caja-cerrada")) {
                labelEstadoTurno.getStyleClass().add("estado-caja-cerrada");
            }
            labelInfoTurno.setText("No hay una caja abierta actualmente. Las ventas estan bloqueadas hasta abrir una nueva caja.");
            labelMontoInicial.setText("S/ 0.00");
            labelVentasEfectivo.setText("S/ 0.00");
            labelYape.setText("S/ 0.00");
            labelPlin.setText("S/ 0.00");
            labelTarjeta.setText("S/ 0.00");
            labelTransferencia.setText("S/ 0.00");
            labelEfectivoEsperado.setText("S/ 0.00");
            botonAccion.setText("ABRIR CAJA");
            return;
        }

        labelEstadoTurno.setText("CAJA ABIERTA");
        labelEstadoTurno.getStyleClass().removeAll("estado-caja-cerrada");
        if (!labelEstadoTurno.getStyleClass().contains("estado-caja-abierta")) {
            labelEstadoTurno.getStyleClass().add("estado-caja-abierta");
        }
        labelInfoTurno.setText("Turno abierto desde " + cajaActual.getFechaApertura().format(FORMATO_FECHA_HORA));
        botonAccion.setText("CERRAR CAJA");

        Map<MetodoPago, BigDecimal> resumen = cajaService.resumenVentasDesde(cajaActual.getFechaApertura());
        BigDecimal efectivo = resumen.getOrDefault(MetodoPago.EFECTIVO, BigDecimal.ZERO);
        BigDecimal yape = resumen.getOrDefault(MetodoPago.YAPE, BigDecimal.ZERO);
        BigDecimal plin = resumen.getOrDefault(MetodoPago.PLIN, BigDecimal.ZERO);
        BigDecimal tarjeta = resumen.getOrDefault(MetodoPago.TARJETA, BigDecimal.ZERO);
        BigDecimal transferencia = resumen.getOrDefault(MetodoPago.TRANSFERENCIA, BigDecimal.ZERO);
        BigDecimal efectivoEsperado = cajaService.efectivoEsperado(cajaActual, resumen);

        labelMontoInicial.setText("S/ " + cajaActual.getMontoInicial().setScale(2, RoundingMode.HALF_UP));
        labelVentasEfectivo.setText("S/ " + efectivo.setScale(2, RoundingMode.HALF_UP));
        labelYape.setText("S/ " + yape.setScale(2, RoundingMode.HALF_UP));
        labelPlin.setText("S/ " + plin.setScale(2, RoundingMode.HALF_UP));
        labelTarjeta.setText("S/ " + tarjeta.setScale(2, RoundingMode.HALF_UP));
        labelTransferencia.setText("S/ " + transferencia.setScale(2, RoundingMode.HALF_UP));
        labelEfectivoEsperado.setText("S/ " + efectivoEsperado);
    }

    @FXML
    private void onAccionCaja() {
        if (cajaActual == null) {
            abrirCaja();
        } else {
            cerrarCaja();
        }
    }

    private void abrirCaja() {
        BigDecimal montoInicial = solicitarMontoInicialCaja();
        if (montoInicial == null) {
            return;
        }
        cajaActual = cajaService.abrirCaja(usuarioSesion, montoInicial);
        actualizarVista();
        new Alert(Alert.AlertType.INFORMATION, "Caja abierta correctamente").showAndWait();
    }

    /** Pide el monto inicial de caja hasta recibir un numero valido y no negativo, o null si el usuario cancela. */
    private BigDecimal solicitarMontoInicialCaja() {
        while (true) {
            TextInputDialog dialogo = new TextInputDialog("0.00");
            dialogo.setTitle("Apertura de caja");
            dialogo.setHeaderText("Inicio de turno - " + usuarioSesion.getNombreCompleto());
            dialogo.setContentText("Monto inicial de efectivo en caja (S/):");

            Optional<String> respuesta = dialogo.showAndWait();
            if (respuesta.isEmpty()) {
                return null;
            }

            BigDecimal parseado = parsearMonto(respuesta.get());
            if (parseado != null && parseado.signum() >= 0) {
                return parseado;
            }
            new Alert(Alert.AlertType.WARNING, "El monto inicial debe ser un numero valido y no negativo").showAndWait();
        }
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void cerrarCaja() {
        Map<MetodoPago, BigDecimal> resumen = cajaService.resumenVentasDesde(cajaActual.getFechaApertura());
        StringBuilder detalle = new StringBuilder();
        detalle.append("Monto inicial: S/ ").append(cajaActual.getMontoInicial().setScale(2, RoundingMode.HALF_UP)).append('\n');
        for (MetodoPago metodo : MetodoPago.values()) {
            BigDecimal monto = resumen.getOrDefault(metodo, BigDecimal.ZERO);
            detalle.append(metodo.name()).append(": S/ ").append(monto.setScale(2, RoundingMode.HALF_UP)).append('\n');
        }
        BigDecimal totalVentas = cajaService.totalVentas(resumen);
        detalle.append("Total vendido: S/ ").append(totalVentas.setScale(2, RoundingMode.HALF_UP));

        BigDecimal montoFinal = cajaService.efectivoEsperado(cajaActual, resumen);
        detalle.append("\nEfectivo esperado en caja: S/ ").append(montoFinal);

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, detalle.toString(), ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Cierre de caja");
        confirmacion.setHeaderText("Resumen del turno");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        cajaService.cerrarCaja(cajaActual.getId(), LocalDateTime.now(), montoFinal);
        cajaActual = null;
        actualizarVista();
        new Alert(Alert.AlertType.INFORMATION, "Caja cerrada correctamente").showAndWait();
    }
}
