package com.dontoribio.pos.service;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Balanza fisica conectada por puerto serial RS-232/USB, usando jSerialComm.
 * Implementa el mismo contrato {@link IBalanzaService} que la version simulada
 * para que el resto de la aplicacion sea agnostica al hardware real.
 *
 * Nota: el formato de trama (ej. "ST,GS,+001.234kg\r\n") depende del modelo de
 * balanza comercial; {@link #parsearPeso(String)} debe ajustarse al protocolo
 * especifico del fabricante utilizado en produccion.
 */
public class BalanzaRealService implements IBalanzaService {

    private final String nombrePuerto;
    private final int baudRate;
    private SerialPort puertoSerial;
    private Task<Void> tareaLectura;
    private Thread hiloLectura;

    public BalanzaRealService(String nombrePuerto, int baudRate) {
        this.nombrePuerto = nombrePuerto;
        this.baudRate = baudRate;
    }

    @Override
    public void conectar() throws BalanzaException {
        puertoSerial = SerialPort.getCommPort(nombrePuerto);
        puertoSerial.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        puertoSerial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (!puertoSerial.openPort()) {
            throw new BalanzaException("No se pudo abrir el puerto " + nombrePuerto);
        }
    }

    @Override
    public void desconectar() {
        detenerLecturaContinua();
        if (puertoSerial != null && puertoSerial.isOpen()) {
            puertoSerial.closePort();
        }
    }

    @Override
    public boolean isConectado() {
        return puertoSerial != null && puertoSerial.isOpen();
    }

    @Override
    public void iniciarLecturaContinua(PesoListener listener) {
        if (!isConectado()) {
            Platform.runLater(() -> listener.onError("La balanza no esta conectada al puerto " + nombrePuerto));
            return;
        }
        detenerLecturaContinua();

        tareaLectura = new Task<>() {
            @Override
            protected Void call() {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(puertoSerial.getInputStream(), StandardCharsets.US_ASCII))) {
                    while (!isCancelled() && puertoSerial.isOpen()) {
                        String linea = reader.readLine();
                        if (linea == null || linea.isBlank()) {
                            continue;
                        }
                        try {
                            double peso = parsearPeso(linea);
                            Platform.runLater(() -> listener.onPesoRecibido(peso));
                        } catch (NumberFormatException ex) {
                            Platform.runLater(() -> listener.onError("Trama invalida de la balanza: " + linea));
                        }
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> listener.onError("Error de lectura serial: " + e.getMessage()));
                }
                return null;
            }
        };

        hiloLectura = new Thread(tareaLectura, "balanza-real-lectura");
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

    /**
     * Extrae el valor numerico de peso (kg) desde la trama recibida.
     * Ajustar segun el protocolo del modelo de balanza comercial utilizado.
     */
    private double parsearPeso(String trama) {
        String limpio = trama.replaceAll("[^0-9.+-]", "");
        return Double.parseDouble(limpio);
    }
}
