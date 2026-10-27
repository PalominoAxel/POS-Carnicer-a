package com.dontoribio.pos.service;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Balanza simulada por software: genera lecturas de peso aleatorias y estables
 * (fluctuacion pequena alrededor de un valor base) para poder desarrollar y
 * demostrar el POS sin depender de hardware fisico conectado.
 *
 * La lectura continua corre en un {@link Task} de JavaFX ejecutado en un hilo
 * secundario; cada nueva lectura se publica al listener mediante
 * {@link Platform#runLater(Runnable)} para respetar el hilo de UI.
 */
public class BalanzaSimuladaService implements IBalanzaService {

    private static final long INTERVALO_LECTURA_MS = 400;

    private boolean conectado = false;
    private Thread hiloLectura;
    private Task<Void> tareaLectura;
    private double pesoBase = 0.0;

    @Override
    public void conectar() {
        conectado = true;
        pesoBase = 1.0 + ThreadLocalRandom.current().nextDouble(0, 2.0);
    }

    @Override
    public void desconectar() {
        detenerLecturaContinua();
        conectado = false;
    }

    @Override
    public boolean isConectado() {
        return conectado;
    }

    @Override
    public void iniciarLecturaContinua(PesoListener listener) {
        if (!conectado) {
            Platform.runLater(() -> listener.onError("La balanza simulada no esta conectada"));
            return;
        }
        detenerLecturaContinua();

        tareaLectura = new Task<>() {
            @Override
            protected Void call() {
                while (!isCancelled()) {
                    double ruido = ThreadLocalRandom.current().nextDouble(-0.05, 0.05);
                    double pesoActual = Math.max(0, pesoBase + ruido);
                    // Redondeo a 3 decimales, tipico de balanzas comerciales
                    double pesoRedondeado = Math.round(pesoActual * 1000.0) / 1000.0;

                    Platform.runLater(() -> listener.onPesoRecibido(pesoRedondeado));

                    try {
                        Thread.sleep(INTERVALO_LECTURA_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            }
        };

        hiloLectura = new Thread(tareaLectura, "balanza-simulada-lectura");
        hiloLectura.setDaemon(true);
        hiloLectura.start();
    }

    @Override
    public void detenerLecturaContinua() {
        if (tareaLectura != null) {
            tareaLectura.cancel();
        }
        if (hiloLectura != null) {
            hiloLectura.interrupt();
        }
    }
}
