package com.dontoribio.pos.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lector centralizado de application.properties (classpath). Este archivo es enteramente
 * OPCIONAL: la app funciona sin el (ver DatabaseConnection, que usa una base de datos
 * embebida por defecto). Solo existe para quien quiera sobreescribir algun valor a mano
 * (desarrollo, o apuntar a un Postgres real), asi que su ausencia o un error de lectura
 * nunca bloquean el arranque: simplemente no hay overrides y se usan los valores por defecto.
 * Se carga una sola vez y se reutiliza en toda la aplicacion.
 */
public final class PropertiesLoader {

    private static final String ARCHIVO_CONFIG = "application.properties";
    private static final Properties PROPIEDADES = new Properties();

    static {
        try (InputStream input = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(ARCHIVO_CONFIG)) {
            if (input != null) {
                PROPIEDADES.load(input);
            }
        } catch (IOException e) {
            // Archivo opcional: si esta corrupto, se ignora y se sigue sin overrides en vez
            // de impedir que la app arranque.
        }
    }

    private PropertiesLoader() {
    }

    public static String get(String key) {
        return PROPIEDADES.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPIEDADES.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String valor = PROPIEDADES.getProperty(key);
        return valor == null ? defaultValue : Integer.parseInt(valor.trim());
    }

    public static long getLong(String key, long defaultValue) {
        String valor = PROPIEDADES.getProperty(key);
        return valor == null ? defaultValue : Long.parseLong(valor.trim());
    }
}
