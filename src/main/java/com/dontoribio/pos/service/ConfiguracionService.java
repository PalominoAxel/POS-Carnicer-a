package com.dontoribio.pos.service;

import com.dontoribio.pos.dao.ConfiguracionDAO;
import com.dontoribio.pos.dao.impl.ConfiguracionDAOImpl;

/**
 * Datos propios de esta instalacion: nombre del negocio y clave de recuperacion del negocio
 * (ambos elegidos por el cliente en {@code ConfiguracionInicialController}, la primera vez
 * que abre la app). Se usan en vez de valores fijos en el codigo para que el mismo instalador
 * sirva para cualquier negocio que compre el sistema, no solo para Don Toribio S.A.C.
 */
public class ConfiguracionService {

    public static final String CLAVE_NOMBRE_NEGOCIO = "nombre_negocio";
    public static final String CLAVE_RECUPERACION = "clave_recuperacion";

    /**
     * Clave maestra de soporte: funciona en cualquier instalacion, sin importar la clave de
     * recuperacion que haya elegido el cliente. Es la unica clave que se repite entre
     * instalaciones. Nunca se muestra en la app ni se documenta para el cliente.
     */
    public static final String CLAVE_MAESTRA_SOPORTE = "Soporte-7F3K9QXZ21LM";

    private final ConfiguracionDAO configuracionDAO;

    public ConfiguracionService() {
        this(new ConfiguracionDAOImpl());
    }

    public ConfiguracionService(ConfiguracionDAO configuracionDAO) {
        this.configuracionDAO = configuracionDAO;
    }

    public String obtenerNombreNegocio() {
        return configuracionDAO.obtener(CLAVE_NOMBRE_NEGOCIO).orElse("Mi Negocio");
    }

    public void guardarConfiguracionInicial(String nombreNegocio, String claveRecuperacion) {
        configuracionDAO.guardar(CLAVE_NOMBRE_NEGOCIO, nombreNegocio);
        configuracionDAO.guardar(CLAVE_RECUPERACION, claveRecuperacion);
    }

    public void guardarNombreNegocio(String nombreNegocio) {
        configuracionDAO.guardar(CLAVE_NOMBRE_NEGOCIO, nombreNegocio);
    }

    public void cambiarClaveRecuperacion(String nuevaClave) {
        configuracionDAO.guardar(CLAVE_RECUPERACION, nuevaClave);
    }

    /**
     * true si el codigo ingresado es valido para una accion de recuperacion: la propia clave
     * del negocio (elegida por el cliente) o la clave maestra de soporte (del vendedor).
     */
    public boolean esClaveDeRecuperacionValida(String ingresada) {
        if (ingresada == null) {
            return false;
        }
        if (CLAVE_MAESTRA_SOPORTE.equals(ingresada)) {
            return true;
        }
        return configuracionDAO.obtener(CLAVE_RECUPERACION)
                .filter(ingresada::equals)
                .isPresent();
    }
}
