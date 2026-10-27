package com.dontoribio.pos.service;

/**
 * Callback invocado por una implementacion de {@link IBalanzaService} cada vez
 * que hay una nueva lectura de peso, o cuando ocurre un error de lectura/conexion.
 * Las implementaciones garantizan que estos metodos se invocan en el hilo de UI
 * de JavaFX (via Platform.runLater), por lo que es seguro actualizar controles desde aqui.
 */
public interface PesoListener {

    void onPesoRecibido(double pesoKg);

    void onError(String mensaje);
}
