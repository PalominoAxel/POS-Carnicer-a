package com.dontoribio.pos.util;

import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.service.UsuarioService;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Autorizacion de supervisor para acciones sensibles (editar precios, desactivar productos,
 * gestionar usuarios). Si quien esta logueado ya es administrador, autoriza sin preguntar nada
 * (no agrega friccion al negocio donde una sola persona hace todo); si es cajero, pide usuario
 * y contrasena de un administrador antes de continuar.
 */
public final class AutorizacionSupervisor {

    private static final UsuarioService USUARIO_SERVICE = new UsuarioService();

    private AutorizacionSupervisor() {
    }

    public static boolean verificarPermiso(Usuario usuarioSesion, String accion) {
        if (USUARIO_SERVICE.esAdministradorVigente(usuarioSesion)) {
            return true;
        }

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Autorizacion requerida");
        dialogo.setHeaderText("Se requiere autorizacion de un administrador para " + accion + ".");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoUsername = new TextField();
        campoUsername.setPromptText("Usuario administrador");
        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Contrasena");

        GridPane grid = new GridPane();
        grid.setHgap(8.0);
        grid.setVgap(8.0);
        grid.setPadding(new Insets(10.0));
        grid.addRow(0, new Label("Usuario:"), campoUsername);
        grid.addRow(1, new Label("Contrasena:"), campoPassword);
        dialogo.getDialogPane().setContent(grid);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return false;
        }

        boolean autorizado = USUARIO_SERVICE.verificarCredencialesAdministrador(
                campoUsername.getText(), campoPassword.getText());
        if (!autorizado) {
            new Alert(Alert.AlertType.ERROR, "Usuario o contrasena de administrador incorrectos").showAndWait();
        }
        return autorizado;
    }
}
