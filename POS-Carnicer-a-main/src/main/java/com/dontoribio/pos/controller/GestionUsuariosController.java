package com.dontoribio.pos.controller;

import com.dontoribio.pos.model.Rol;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.service.UsuarioService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

/**
 * Controlador de la pantalla de gestion de usuarios: alta de cuentas con su rol y
 * restablecimiento de contrasena de cualquier usuario (sin necesitar la anterior),
 * para cuando alguien la olvida. Solo se abre tras autorizacion de supervisor
 * (ver MainPOSController#onAbrirGestionUsuarios).
 */
public class GestionUsuariosController {

    @FXML
    private TableView<Usuario> tablaUsuarios;
    @FXML
    private TableColumn<Usuario, String> colNombreCompleto;
    @FXML
    private TableColumn<Usuario, String> colUsername;
    @FXML
    private TableColumn<Usuario, Rol> colRol;
    @FXML
    private TableColumn<Usuario, Boolean> colActivo;

    @FXML
    private TextField campoNombreCompleto;
    @FXML
    private TextField campoUsername;
    @FXML
    private ComboBox<Rol> comboRol;
    @FXML
    private CheckBox checkActivo;
    @FXML
    private PasswordField campoNuevaPassword;
    @FXML
    private PasswordField campoConfirmarPassword;
    @FXML
    private Button botonGuardar;

    private final UsuarioService usuarioService = new UsuarioService();
    private ObservableList<Usuario> usuarios;
    private Usuario usuarioSeleccionado;
    private Usuario usuarioSesion;

    /** Usuario que abrio esta pantalla; se usa para evitar que se desactive o se quite el rol a si mismo. */
    public void setUsuarioSesion(Usuario usuario) {
        this.usuarioSesion = usuario;
    }

    @FXML
    private void initialize() {
        colNombreCompleto.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        comboRol.setItems(FXCollections.observableArrayList(Rol.values()));

        tablaUsuarios.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> seleccionarUsuario(nuevo));

        cargarTabla();
        limpiarFormulario();
    }

    private void cargarTabla() {
        usuarios = FXCollections.observableArrayList(usuarioService.listarTodos());
        tablaUsuarios.setItems(usuarios);
    }

    private void seleccionarUsuario(Usuario usuario) {
        usuarioSeleccionado = usuario;
        if (usuario == null) {
            limpiarFormulario();
            return;
        }
        campoNombreCompleto.setText(usuario.getNombreCompleto());
        campoUsername.setText(usuario.getUsername());
        comboRol.setValue(usuario.getRol());
        checkActivo.setSelected(usuario.isActivo());
        campoNuevaPassword.clear();
        campoConfirmarPassword.clear();
        botonGuardar.setText("Guardar cambios");
    }

    @FXML
    private void onNuevoUsuario() {
        tablaUsuarios.getSelectionModel().clearSelection();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        usuarioSeleccionado = null;
        campoNombreCompleto.clear();
        campoUsername.clear();
        comboRol.setValue(Rol.CAJERO);
        checkActivo.setSelected(true);
        campoNuevaPassword.clear();
        campoConfirmarPassword.clear();
        botonGuardar.setText("Crear usuario");
    }

    @FXML
    private void onGuardar() {
        String nombreCompleto = campoNombreCompleto.getText() == null ? "" : campoNombreCompleto.getText().trim();
        String username = campoUsername.getText() == null ? "" : campoUsername.getText().trim();
        Rol rol = comboRol.getValue();
        boolean activo = checkActivo.isSelected();
        String nuevaPassword = campoNuevaPassword.getText();
        String confirmarPassword = campoConfirmarPassword.getText();

        if (nombreCompleto.isEmpty() || username.isEmpty() || rol == null) {
            new Alert(Alert.AlertType.WARNING, "Complete nombre, usuario y rol").showAndWait();
            return;
        }

        boolean esNuevo = usuarioSeleccionado == null;

        if (esNuevo && (nuevaPassword == null || nuevaPassword.isEmpty())) {
            new Alert(Alert.AlertType.WARNING, "Ingrese una contrasena para el nuevo usuario").showAndWait();
            return;
        }
        boolean cambiaPassword = nuevaPassword != null && !nuevaPassword.isEmpty();
        if (cambiaPassword) {
            if (nuevaPassword.length() < 6) {
                new Alert(Alert.AlertType.WARNING, "La contrasena debe tener al menos 6 caracteres").showAndWait();
                return;
            }
            if (!nuevaPassword.equals(confirmarPassword)) {
                new Alert(Alert.AlertType.WARNING, "Las contrasenas no coinciden").showAndWait();
                return;
            }
        }

        if (!esNuevo && usuarioSesion != null && usuarioSeleccionado.getId().equals(usuarioSesion.getId())
                && usuarioSeleccionado.getRol() == Rol.ADMINISTRADOR
                && (!activo || rol != Rol.ADMINISTRADOR)) {
            new Alert(Alert.AlertType.WARNING,
                    "No puede desactivarse ni quitarse el rol de administrador a si mismo desde aqui. "
                            + "Pidale a otro administrador que lo haga.").showAndWait();
            return;
        }

        boolean cambiaUsername = esNuevo || !username.equals(usuarioSeleccionado.getUsername());
        if (cambiaUsername) {
            Optional<Usuario> existente = usuarioService.buscarPorUsername(username);
            if (existente.isPresent() && (esNuevo || !existente.get().getId().equals(usuarioSeleccionado.getId()))) {
                new Alert(Alert.AlertType.WARNING, "Ya existe un usuario con ese nombre, elija otro").showAndWait();
                return;
            }
        }

        try {
            if (esNuevo) {
                usuarioService.crearUsuario(nombreCompleto, username, nuevaPassword, rol);
            } else {
                usuarioSeleccionado.setNombreCompleto(nombreCompleto);
                usuarioSeleccionado.setUsername(username);
                usuarioSeleccionado.setRol(rol);
                usuarioSeleccionado.setActivo(activo);
                usuarioService.actualizarDatos(usuarioSeleccionado);
                if (cambiaPassword) {
                    usuarioService.resetearPassword(usuarioSeleccionado.getId(), nuevaPassword);
                }
            }
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el usuario: " + e.getMessage()).showAndWait();
            return;
        }

        cargarTabla();
        limpiarFormulario();
        new Alert(Alert.AlertType.INFORMATION, "Usuario guardado correctamente").showAndWait();
    }

    @FXML
    private void onCerrar() {
        ((javafx.stage.Stage) botonGuardar.getScene().getWindow()).close();
    }
}
