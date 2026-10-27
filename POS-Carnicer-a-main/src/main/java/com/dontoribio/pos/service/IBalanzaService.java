package com.dontoribio.pos.service;

/**
 * Contrato de comunicacion con la balanza (patron Strategy). Permite alternar
 * de forma transparente entre {@link BalanzaSimuladaService} (desarrollo/demo)
 * y {@link BalanzaRealService} (lectura fisica por puerto serial RS-232/USB)
 * sin que el resto de la aplicacion (controllers) conozca la diferencia.
 */
public interface IBalanzaService {

    /** Abre la conexion con el dispositivo (o inicializa el simulador). */
    void conectar() throws BalanzaException;

    /** Cierra la conexion y detiene cualquier lectura en curso. */
    void desconectar();

    boolean isConectado();

    /**
     * Inicia la lectura continua de peso en un hilo secundario (javafx.concurrent.Task).
     * Las notificaciones al listener se realizan mediante Platform.runLater
     * para no bloquear ni violar el hilo de UI de JavaFX.
     */
    void iniciarLecturaContinua(PesoListener listener);

    /** Detiene la lectura continua sin cerrar la conexion. */
    void detenerLecturaContinua();
}
