package com.dontoribio.pos.service;

/** Error de conexion o comunicacion con el dispositivo de pesaje. */
public class BalanzaException extends Exception {

    public BalanzaException(String message) {
        super(message);
    }

    public BalanzaException(String message, Throwable cause) {
        super(message, cause);
    }
}
