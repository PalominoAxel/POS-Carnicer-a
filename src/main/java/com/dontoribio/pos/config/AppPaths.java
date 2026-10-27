package com.dontoribio.pos.config;

import java.io.File;

/**
 * Rutas de archivos propios de esta instalacion (una por PC, nunca compartida en red):
 * %APPDATA%\DonToribioPOS en Windows, o ~/DonToribioPOS si no hay APPDATA (desarrollo o
 * no-Windows). Centraliza lo que antes estaba inline solo en DatabaseConnection, para que
 * cualquier otra clase que necesite un archivo persistente de esta instalacion (imagenes
 * subidas desde Catalogo, por ejemplo) use el mismo criterio.
 */
public final class AppPaths {

    private static final String NOMBRE_CARPETA_APP = "DonToribioPOS";

    private AppPaths() {
    }

    public static File carpetaBase() {
        String appData = System.getenv("APPDATA");
        File base = new File(appData != null && !appData.isBlank() ? appData : System.getProperty("user.home"),
                NOMBRE_CARPETA_APP);
        crearSiFalta(base);
        return base;
    }

    public static File carpetaDatos() {
        File dataDir = new File(carpetaBase(), "data");
        crearSiFalta(dataDir);
        return dataDir;
    }

    public static File carpetaImagenesProductos() {
        File imagenesDir = new File(carpetaBase(), "images/productos");
        crearSiFalta(imagenesDir);
        return imagenesDir;
    }

    private static void crearSiFalta(File carpeta) {
        if (!carpeta.exists() && !carpeta.mkdirs() && !carpeta.exists()) {
            throw new IllegalStateException("No se pudo crear la carpeta: " + carpeta.getAbsolutePath());
        }
    }
}
