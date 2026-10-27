package com.dontoribio.pos.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lector centralizado de application.properties (classpath).
 * Se carga una sola vez y se reutiliza en toda la aplicacion.
 */
public final class PropertiesLoader {

    private static final String ARCHIVO_CONFIG = "application.properties";
    private static final Properties PROPIEDADES = new Properties();

    static {
        try (InputStream input = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(ARCHIVO_CONFIG)) {
            if (input == null) {
                throw new IllegalStateException(
                        "No se encontro el archivo " + ARCHIVO_CONFIG + " en el classpath");
            }
            PROPIEDADES.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer " + ARCHIVO_CONFIG, e);
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
