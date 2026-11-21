package com.dontoribio.pos.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/** Configuracion comun, perfil y variables externas del POS de escritorio. */
public final class PropertiesLoader {
    private static final Map<String, String> VARIABLES = Map.of(
            "db.url", "DB_URL", "db.user", "DB_USERNAME",
            "db.password", "DB_PASSWORD", "db.driver", "DB_DRIVER");
    private static final Properties PROPIEDADES = cargar(
            PropertiesLoader.class.getClassLoader(), System.getenv(), System.getProperties());

    private PropertiesLoader() { }

    static Properties cargar(ClassLoader recursos, Map<String, String> entorno, Properties sistema) {
        Properties resultado = new Properties();
        leer(recursos, "application.properties", resultado);
        String perfil = sistema.getProperty("pos.profile",
                entorno.getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
        if (!perfil.equals("dev") && !perfil.equals("prod")) {
            throw new IllegalArgumentException("El perfil debe ser dev o prod");
        }
        leer(recursos, "application-" + perfil + ".properties", resultado);
        if (perfil.equals("dev")) leer(recursos, "application-local.properties", resultado);
        resultado.setProperty("app.profile", perfil);
        for (var variable : VARIABLES.entrySet()) {
            String valor = entorno.get(variable.getValue());
            if (valor != null) resultado.setProperty(variable.getKey(), valor);
        }
        // Solo se resuelven referencias externas completas, nunca se imprimen sus valores.
        for (String nombre : resultado.stringPropertyNames()) {
            if (VARIABLES.containsKey(nombre) && entorno.containsKey(VARIABLES.get(nombre))) continue;
            String valor = resultado.getProperty(nombre);
            if (valor.startsWith("${") && valor.endsWith("}")) {
                String variable = valor.substring(2, valor.length() - 1);
                String externo = entorno.get(variable);
                if (externo == null) throw new IllegalArgumentException("Falta la variable " + variable);
                resultado.setProperty(nombre, externo);
            }
        }
        if (perfil.equals("prod")) {
            for (String nombre : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD"}) {
                String valor = entorno.get(nombre);
                if (valor == null || valor.isBlank()) {
                    throw new IllegalArgumentException("Produccion requiere la variable " + nombre);
                }
            }
            resultado.setProperty("db.schema.initialize", "false");
        }
        return resultado;
    }

    private static void leer(ClassLoader recursos, String nombre, Properties destino) {
        try (InputStream input = recursos.getResourceAsStream(nombre)) {
            if (input != null) destino.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + nombre, e);
        }
    }

    public static String get(String key) { return PROPIEDADES.getProperty(key); }
    public static String get(String key, String defaultValue) {
        return PROPIEDADES.getProperty(key, defaultValue);
    }
    public static int getInt(String key, int defaultValue) {
        String valor = get(key);
        return valor == null ? defaultValue : Integer.parseInt(valor.trim());
    }
    public static long getLong(String key, long defaultValue) {
        String valor = get(key);
        return valor == null ? defaultValue : Long.parseLong(valor.trim());
    }
}
