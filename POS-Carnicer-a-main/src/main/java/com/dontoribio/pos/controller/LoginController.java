package com.dontoribio.pos.controller;

import com.dontoribio.pos.MainApp;
import com.dontoribio.pos.control.CampoPasswordVisible;
import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.dao.impl.UsuarioDAOImpl;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.util.PasswordUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/** Controlador de la pantalla de autenticacion. Valida credenciales contra BCrypt. */
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    /** Clave maestra para desbloquear el ingreso o restablecer una contrasena olvidada. */
    private static final String CODIGO_AUTENTICACION = "Moises955";
    private static final int MAX_INTENTOS_FALLIDOS = 4;
    private static final long DURACION_BLOQUEO_MS = 60_000L;
    private static final int LONGITUD_MINIMA_PASSWORD = 6;

    @FXML
    private TextField campoUsername;

    @FXML
    private CampoPasswordVisible campoPassword;

    @FXML
    private Label labelError;

    @FXML
    private Button botonIniciarSesion;

    @FXML
    private Hyperlink enlaceOlvidoPassword;

    @FXML
    private VBox panelBloqueo;

    @FXML
    private Label labelBloqueo;

    @FXML
    private CampoPasswordVisible campoCodigoDesbloqueo;

    @FXML
    private VBox panelRecuperacion;

    @FXML
    private TextField campoUsernameRecuperacion;

    @FXML
    private CampoPasswordVisible campoNuevaPassword;

    @FXML
    private CampoPasswordVisible campoConfirmarPassword;

    @FXML
    private CampoPasswordVisible campoCodigoRecuperacion;

    @FXML
    private Label labelErrorRecuperacion;

    @FXML
    private Button botonCambiarPasswordOlvidada;

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    private int intentosFallidos = 0;
    private long bloqueadoHasta = 0L;
    private Timeline temporizadorBloqueo;

    @FXML
    private void initialize() {
        labelError.setText("");
    }

    @FXML
    private void onIniciarSesion(ActionEvent event) {
        if (estaBloqueado()) {
            return;
        }

        String username = campoUsername.getText() == null ? "" : campoUsername.getText().trim();
        String password = campoPassword.getText();

        if (username.isEmpty() || password == null || password.isEmpty()) {
            labelError.setText("Ingrese usuario y contrasena");
            return;
        }

        Optional<Usuario> usuarioOpt;
        try {
            usuarioOpt = usuarioDAO.buscarPorUsername(username);
        } catch (RuntimeException e) {
            LOG.error("No se pudo conectar con la base de datos al intentar iniciar sesion (username={})", username, e);
            labelError.setText("No se pudo conectar con la base de datos");
            return;
        }

        boolean credencialesValidas;
        try {
            credencialesValidas = usuarioOpt.isPresent() && usuarioOpt.get().isActivo()
                    && PasswordUtil.verificarPassword(password, usuarioOpt.get().getPasswordHash());
        } catch (IllegalArgumentException e) {
            labelError.setText("El usuario tiene una contrasena mal configurada");
            return;
        }

        if (!credencialesValidas) {
            registrarIntentoFallido();
            return;
        }

        intentosFallidos = 0;
        Usuario usuario = usuarioOpt.get();
        try {
            MainApp.getInstance().mostrarPantallaPrincipal(usuario);
        } catch (RuntimeException e) {
            labelError.setText("No se pudo abrir el POS: " + e.getMessage());
        }
    }

    private boolean estaBloqueado() {
        return System.currentTimeMillis() < bloqueadoHasta;
    }

    private void registrarIntentoFallido() {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            activarBloqueo();
        } else {
            labelError.setText("Usuario o contrasena incorrectos");
        }
    }

    /** Invalida el ingreso por 1 minuto tras 4 intentos fallidos y habilita el desbloqueo por codigo. */
    private void activarBloqueo() {
        bloqueadoHasta = System.currentTimeMillis() + DURACION_BLOQUEO_MS;
        botonIniciarSesion.setDisable(true);
        campoCodigoDesbloqueo.clear();
        panelBloqueo.setVisible(true);
        panelBloqueo.setManaged(true);
        labelError.setText("Demasiados intentos fallidos. Ingreso bloqueado.");

        if (temporizadorBloqueo != null) {
            temporizadorBloqueo.stop();
        }
        temporizadorBloqueo = new Timeline(new KeyFrame(Duration.seconds(1), e -> actualizarCuentaRegresiva()));
        temporizadorBloqueo.setCycleCount(Timeline.INDEFINITE);
        actualizarCuentaRegresiva();
        temporizadorBloqueo.play();
    }

    private void actualizarCuentaRegresiva() {
        long restanteMs = bloqueadoHasta - System.currentTimeMillis();
        if (restanteMs <= 0) {
            desactivarBloqueo();
            return;
        }
        long segundosRestantes = (restanteMs + 999) / 1000;
        labelBloqueo.setText("Intente nuevamente en " + segundosRestantes
                + " s, o ingrese el codigo de autenticacion para desbloquear ahora.");
    }

    private void desactivarBloqueo() {
        if (temporizadorBloqueo != null) {
            temporizadorBloqueo.stop();
        }
        bloqueadoHasta = 0L;
        intentosFallidos = 0;
        botonIniciarSesion.setDisable(false);
        panelBloqueo.setVisible(false);
        panelBloqueo.setManaged(false);
        labelError.setText("");
    }

    @FXML
    private void onDesbloquearConCodigo(ActionEvent event) {
        if (CODIGO_AUTENTICACION.equals(campoCodigoDesbloqueo.getText())) {
            desactivarBloqueo();
            labelError.setText("Ingreso desbloqueado. Intente iniciar sesion nuevamente.");
        } else {
            labelBloqueo.setText("Codigo de autenticacion incorrecto.");
        }
    }

    @FXML
    private void onAbrirRecuperacion(ActionEvent event) {
        if (panelRecuperacion.isVisible()) {
            cerrarPanelRecuperacion();
        } else {
            abrirPanelRecuperacion();
        }
    }

    /**
     * Abre el panel de restablecimiento de contrasena como un formulario independiente:
     * copia el usuario ya escrito arriba (si lo hay) como punto de partida, pero el panel
     * no depende de los campos de login para funcionar. Mientras esta abierto, se deshabilita
     * el inicio de sesion y el boton "Cambiar contrasena" pasa a ser el boton por defecto
     * (Enter), para que escribir y presionar Enter en estos campos no dispare el login.
     */
    private void abrirPanelRecuperacion() {
        campoUsernameRecuperacion.setText(campoUsername.getText() == null ? "" : campoUsername.getText().trim());
        campoNuevaPassword.clear();
        campoConfirmarPassword.clear();
        campoCodigoRecuperacion.clear();
        labelErrorRecuperacion.setText("");

        panelRecuperacion.setVisible(true);
        panelRecuperacion.setManaged(true);

        campoUsername.setDisable(true);
        campoPassword.setDisable(true);
        botonIniciarSesion.setDisable(true);
        botonIniciarSesion.setDefaultButton(false);
        botonCambiarPasswordOlvidada.setDefaultButton(true);

        campoUsernameRecuperacion.requestFocus();
    }

    private void cerrarPanelRecuperacion() {
        panelRecuperacion.setVisible(false);
        panelRecuperacion.setManaged(false);

        botonCambiarPasswordOlvidada.setDefaultButton(false);
        botonIniciarSesion.setDefaultButton(true);
        if (!estaBloqueado()) {
            campoUsername.setDisable(false);
            campoPassword.setDisable(false);
            botonIniciarSesion.setDisable(false);
        }
    }

    @FXML
    private void onCancelarRecuperacion(ActionEvent event) {
        cerrarPanelRecuperacion();
    }

    @FXML
    private void onCambiarPasswordOlvidada(ActionEvent event) {
        String nueva = campoNuevaPassword.getText();
        if (nueva == null || nueva.length() < LONGITUD_MINIMA_PASSWORD) {
            labelErrorRecuperacion.setText("La nueva contrasena debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
            return;
        }
        if (!nueva.equals(campoConfirmarPassword.getText())) {
            labelErrorRecuperacion.setText("Las contrasenas no coinciden");
            return;
        }

        String username = campoUsernameRecuperacion.getText() == null ? "" : campoUsernameRecuperacion.getText().trim();
        if (username.isEmpty()) {
            labelErrorRecuperacion.setText("Ingrese el nombre de usuario de la cuenta a restablecer");
            return;
        }

        if (!CODIGO_AUTENTICACION.equals(campoCodigoRecuperacion.getText())) {
            labelErrorRecuperacion.setText("Codigo de autenticacion incorrecto");
            return;
        }

        Optional<Usuario> usuarioOpt;
        try {
            usuarioOpt = usuarioDAO.buscarPorUsername(username);
        } catch (RuntimeException e) {
            LOG.error("No se pudo conectar con la base de datos al restablecer contrasena (username={})", username, e);
            labelErrorRecuperacion.setText("No se pudo conectar con la base de datos");
            return;
        }
        if (usuarioOpt.isEmpty()) {
            labelErrorRecuperacion.setText("No existe un usuario con ese nombre");
            return;
        }

        usuarioDAO.actualizarPassword(usuarioOpt.get().getId(), PasswordUtil.hashPassword(nueva));
        desactivarBloqueo();
        cerrarPanelRecuperacion();
        campoUsername.setText(username);
        campoPassword.clear();
        campoNuevaPassword.clear();
        campoConfirmarPassword.clear();
        campoCodigoRecuperacion.clear();
        new Alert(Alert.AlertType.INFORMATION, "Contrasena actualizada correctamente. Ya puede iniciar sesion.")
                .showAndWait();
    }

    @FXML
    private void onAbrirCrearUsuario(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dontoribio/pos/view/crear_usuario.fxml"));
            Parent root = loader.load();

            Stage ventana = new Stage();
            ventana.setTitle("Crear usuario");
            ventana.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 360, 420);
            scene.getStylesheets().add(getClass().getResource("/com/dontoribio/pos/view/styles.css").toExternalForm());
            ventana.setScene(scene);
            ventana.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir la pantalla de creacion de usuario: " + e.getMessage())
                    .showAndWait();
        }
    }
}
