package com.dontoribio.pos.control;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/**
 * Campo de contrasena con boton de visibilidad: oculta los caracteres escritos por defecto
 * y los revela mientras el boton "Mostrar" esta activado (alternando a "Ocultar").
 * Usable tanto desde FXML (fx:id + promptText) como instanciado directamente en Java.
 */
public class CampoPasswordVisible extends HBox {

    private final PasswordField campoOculto = new PasswordField();
    private final TextField campoVisible = new TextField();
    private final Button botonAlternar = new Button("Mostrar");
    private boolean revelado = false;

    public CampoPasswordVisible() {
        campoVisible.setManaged(false);
        campoVisible.setVisible(false);
        campoVisible.textProperty().bindBidirectional(campoOculto.textProperty());

        campoOculto.setMaxWidth(Double.MAX_VALUE);
        campoVisible.setMaxWidth(Double.MAX_VALUE);

        StackPane contenedorCampo = new StackPane(campoOculto, campoVisible);
        HBox.setHgrow(contenedorCampo, Priority.ALWAYS);

        botonAlternar.setFocusTraversable(false);
        botonAlternar.setOnAction(e -> alternarVisibilidad());

        setSpacing(4.0);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(contenedorCampo, botonAlternar);
    }

    private void alternarVisibilidad() {
        revelado = !revelado;
        campoOculto.setVisible(!revelado);
        campoOculto.setManaged(!revelado);
        campoVisible.setVisible(revelado);
        campoVisible.setManaged(revelado);
        botonAlternar.setText(revelado ? "Ocultar" : "Mostrar");

        TextField campoActivo = revelado ? campoVisible : campoOculto;
        campoActivo.positionCaret(campoActivo.getText() == null ? 0 : campoActivo.getText().length());
        campoActivo.requestFocus();
    }

    public String getText() {
        return campoOculto.getText();
    }

    public void setText(String texto) {
        campoOculto.setText(texto);
    }

    public void clear() {
        campoOculto.clear();
    }

    public StringProperty textProperty() {
        return campoOculto.textProperty();
    }

    public void setPromptText(String texto) {
        campoOculto.setPromptText(texto);
        campoVisible.setPromptText(texto);
    }

    public String getPromptText() {
        return campoOculto.getPromptText();
    }
}
