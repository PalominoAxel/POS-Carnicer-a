package com.dontoribio.pos;

import atlantafx.base.theme.PrimerLight;
import com.dontoribio.pos.config.DatabaseConnection;
import com.dontoribio.pos.controller.MainPOSController;
import com.dontoribio.pos.model.Usuario;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/** Punto de entrada de la aplicacion JavaFX del POS Don Toribio S.A.C. */
public class MainApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);
    private static final String RUTA_VIEWS = "/com/dontoribio/pos/view/";

    private static MainApp instancia;
    private Stage stagePrincipal;

    public static MainApp getInstance() {
        return instancia;
    }

    @Override
    public void start(Stage stage) throws IOException {
        instancia = this;
        this.stagePrincipal = stage;

        Thread.setDefaultUncaughtExceptionHandler((thread, excepcion) ->
                LOG.error("Excepcion no capturada en el hilo '{}'", thread.getName(), excepcion));

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        LOG.info("Iniciando Don Toribio POS");

        stage.setTitle("Don Toribio S.A.C. - Punto de Venta");
        mostrarPantallaLogin();
        stage.show();
    }

    public void mostrarPantallaLogin() {
        cargarEscena("login.fxml", 480, 360);
    }

    /** Invocado por LoginController tras una autenticacion exitosa. */
    public void mostrarPantallaPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RUTA_VIEWS + "main_pos.fxml"));
            Parent root = loader.load();

            MainPOSController controller = loader.getController();
            controller.setUsuarioSesion(usuario);

            aplicarEscena(root, 1200, 720);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar main_pos.fxml", e);
        }
    }

    private void cargarEscena(String fxml, double ancho, double alto) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(RUTA_VIEWS + fxml)));
            aplicarEscena(root, ancho, alto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar " + fxml, e);
        }
    }

    private void aplicarEscena(Parent root, double ancho, double alto) {
        Scene scene = new Scene(root, ancho, alto);
        scene.getStylesheets().add(
                getClass().getResource(RUTA_VIEWS + "styles.css").toExternalForm());
        stagePrincipal.setScene(scene);
        stagePrincipal.centerOnScreen();
    }

    @Override
    public void stop() {
        LOG.info("Cerrando Don Toribio POS");
        DatabaseConnection.cerrarPool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
