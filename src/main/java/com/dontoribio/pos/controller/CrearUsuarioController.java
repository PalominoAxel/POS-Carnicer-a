package com.dontoribio.pos.controller;

import com.dontoribio.pos.control.CampoPasswordVisible;
import com.dontoribio.pos.dao.UsuarioDAO;
import com.dontoribio.pos.dao.impl.UsuarioDAOImpl;
import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.service.ConfiguracionService;
import com.dontoribio.pos.util.PasswordUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador de la pantalla de autoservicio (accesible desde el login, sin sesion) para
 * crear un usuario de acceso al POS. Todas las cuentas creadas aqui quedan con rol CAJERO;
 * crear un ADMINISTRADOR requiere entrar con una cuenta existente y usar "Gestionar usuarios"
 * (ver GestionUsuariosController), para que la escalacion de privilegios no sea posible
 * solo con la clave de recuperacion del negocio.
 * Requiere la clave de recuperacion (la que el propio negocio elige en la configuracion
 * inicial, o la clave maestra de soporte del vendedor) para evitar que cualquiera con acceso
 * a la PC cree cuentas sin autorizacion.
 */
public class CrearUsuarioController {

    @FXML
    private TextField campoNombreCompleto;
    @FXML
    private TextField campoUsername;
    @FXML
    private CampoPasswordVisible campoPassword;
    @FXML
    private CampoPasswordVisible campoConfirmarPassword;
    @FXML
    private CampoPasswordVisible campoClaveRecuperacion;
    @FXML
    private Label labelMensaje;
    @FXML
    private Button botonCrearUsuario;

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final ConfiguracionService configuracionService = new ConfiguracionService();

    @FXML
    private void initialize() {
        labelMensaje.setText("");
    }

    @FXML
    private void onCrearUsuario() {
        String nombreCompleto = campoNombreCompleto.getText() == null ? "" : campoNombreCompleto.getText().trim();
        String username = campoUsername.getText() == null ? "" : campoUsername.getText().trim();
        String password = campoPassword.getText();
        String confirmarPassword = campoConfirmarPassword.getText();
        String claveRecuperacion = campoClaveRecuperacion.getText();

        if (nombreCompleto.isEmpty() || username.isEmpty() || password == null || password.isEmpty()) {
            labelMensaje.setText("Complete nombre, usuario y contrasena");
            return;
        }
        if (username.matches(".*\\s.*")) {
            labelMensaje.setText("El usuario no puede contener espacios");
            return;
        }
        if (password.length() < 6) {
            labelMensaje.setText("La contrasena debe tener al menos 6 caracteres");
            return;
        }
        if (!password.equals(confirmarPassword)) {
            labelMensaje.setText("Las contrasenas no coinciden");
            return;
        }

        if (!configuracionService.esClaveDeRecuperacionValida(claveRecuperacion)) {
            labelMensaje.setText("Clave de recuperacion incorrecta");
            return;
        }

        try {
            if (usuarioDAO.buscarPorUsername(username).isPresent()) {
                labelMensaje.setText("Ya existe un usuario con ese nombre, elija otro");
                return;
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPasswordHash(PasswordUtil.hashPassword(password));
            usuario.setNombreCompleto(nombreCompleto);
            usuario.setRol(Rol.CAJERO);
            usuario.setActivo(true);
            usuarioDAO.guardar(usuario);
        } catch (RuntimeException e) {
            labelMensaje.setText("No se pudo crear el usuario: " + e.getMessage());
            return;
        }

        new Alert(Alert.AlertType.INFORMATION, "Usuario creado correctamente. Ya puede iniciar sesion.").showAndWait();
        cerrarVentana();
    }

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        ((Stage) botonCrearUsuario.getScene().getWindow()).close();
    }
}
