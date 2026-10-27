package com.dontoribio.pos.controller;

import com.dontoribio.pos.MainApp;
import com.dontoribio.pos.control.CampoPasswordVisible;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.service.ConfiguracionService;
import com.dontoribio.pos.service.UsuarioService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Asistente de primera configuracion: se muestra en vez del login solo cuando la instalacion
 * todavia no tiene ninguna clave de recuperacion guardada (ver
 * {@link ConfiguracionService#necesitaConfiguracionInicial()}), es decir, la primera vez que
 * se abre la app en una PC nueva. Reemplaza al usuario administrador fijo que antes traia el
 * sistema: aqui el propio cliente define el nombre de su negocio, crea su primer usuario
 * administrador y elige SU PROPIA clave de recuperacion (ninguna de las dos la conoce el
 * vendedor del sistema).
 */
public class ConfiguracionInicialController {

    private static final int LONGITUD_MINIMA_PASSWORD = 6;
    private static final int LONGITUD_MINIMA_CLAVE_RECUPERACION = 6;

    @FXML
    private TextField campoNombreNegocio;
    @FXML
    private TextField campoUsernameAdmin;
    @FXML
    private TextField campoNombreCompletoAdmin;
    @FXML
    private CampoPasswordVisible campoPasswordAdmin;
    @FXML
    private CampoPasswordVisible campoConfirmarPasswordAdmin;
    @FXML
    private CampoPasswordVisible campoClaveRecuperacion;
    @FXML
    private CampoPasswordVisible campoConfirmarClaveRecuperacion;
    @FXML
    private Label labelMensaje;
    @FXML
    private Button botonFinalizar;

    private final UsuarioService usuarioService = new UsuarioService();
    private final ConfiguracionService configuracionService = new ConfiguracionService();

    @FXML
    private void initialize() {
        labelMensaje.setText("");
    }

    @FXML
    private void onFinalizarConfiguracion() {
        String nombreNegocio = campoNombreNegocio.getText() == null ? "" : campoNombreNegocio.getText().trim();
        String username = campoUsernameAdmin.getText() == null ? "" : campoUsernameAdmin.getText().trim();
        String nombreCompleto = campoNombreCompletoAdmin.getText() == null ? "" : campoNombreCompletoAdmin.getText().trim();
        String password = campoPasswordAdmin.getText();
        String confirmarPassword = campoConfirmarPasswordAdmin.getText();
        String claveRecuperacion = campoClaveRecuperacion.getText();
        String confirmarClaveRecuperacion = campoConfirmarClaveRecuperacion.getText();

        if (nombreNegocio.isEmpty() || username.isEmpty() || nombreCompleto.isEmpty()
                || password == null || password.isEmpty() || claveRecuperacion == null || claveRecuperacion.isEmpty()) {
            labelMensaje.setText("Complete todos los campos");
            return;
        }
        if (username.matches(".*\\s.*")) {
            labelMensaje.setText("El usuario no puede contener espacios");
            return;
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            labelMensaje.setText("La contrasena debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
            return;
        }
        if (!password.equals(confirmarPassword)) {
            labelMensaje.setText("Las contrasenas no coinciden");
            return;
        }
        if (claveRecuperacion.length() < LONGITUD_MINIMA_CLAVE_RECUPERACION) {
            labelMensaje.setText("La clave de recuperacion debe tener al menos "
                    + LONGITUD_MINIMA_CLAVE_RECUPERACION + " caracteres");
            return;
        }
        if (!claveRecuperacion.equals(confirmarClaveRecuperacion)) {
            labelMensaje.setText("Las claves de recuperacion no coinciden");
            return;
        }
        if (ConfiguracionService.CLAVE_MAESTRA_SOPORTE.equals(claveRecuperacion)) {
            labelMensaje.setText("Esa clave de recuperacion no esta disponible, elija otra");
            return;
        }

        try {
            usuarioService.crearUsuario(nombreCompleto, username, password, Rol.ADMINISTRADOR);
            configuracionService.guardarConfiguracionInicial(nombreNegocio, claveRecuperacion);
        } catch (RuntimeException e) {
            labelMensaje.setText("No se pudo completar la configuracion: " + e.getMessage());
            return;
        }

        new Alert(Alert.AlertType.INFORMATION,
                "Configuracion completada. Guarde en un lugar seguro la clave de recuperacion: "
                        + "la va a necesitar si alguna vez olvida su contrasena de administrador. "
                        + "Ya puede iniciar sesion.")
                .showAndWait();

        MainApp.getInstance().mostrarPantallaLogin();
    }
}
