package com.dontoribio.pos;

/**
 * Punto de entrada del jar empaquetado (java -jar / jpackage). No extiende
 * javafx.application.Application: si la clase Main-Class del manifiesto
 * extendiera Application directamente, java -jar fuera de un modulo JPMS
 * lanza "Error: JavaFX runtime components are missing". Delegar el arranque
 * a MainApp.main() evita ese problema.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
