package com.dontoribio.pos.dao;

import java.util.Optional;

/** Acceso a la tabla `configuracion` (clave/valor): datos propios de esta instalacion. */
public interface ConfiguracionDAO {

    Optional<String> obtener(String clave);

    /** Crea o reemplaza el valor de una clave (upsert). */
    void guardar(String clave, String valor);
}
