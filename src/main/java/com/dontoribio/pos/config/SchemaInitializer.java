package com.dontoribio.pos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Crea/actualiza las tablas (schema.sql) en cada arranque y siembra el catalogo inicial
 * (productos.sql) solo en el primer arranque de cada instalacion, para que el comprador
 * nunca tenga que correr SQL a mano (antes habia que ejecutar estos scripts por consola
 * contra Postgres). schema.sql es idempotente (CREATE TABLE IF NOT EXISTS, ALTER TABLE ADD
 * COLUMN IF NOT EXISTS) y se vuelve a correr siempre, para que una instalacion ya existente
 * tambien reciba tablas/columnas nuevas de una version mas reciente; productos.sql no lo es
 * (inserta filas) y solo corre si la tabla "usuarios" todavia no existia.
 */
public final class SchemaInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaInitializer.class);

    /**
     * Imagenes por defecto de los cortes de mayor rotacion (ver
     * MainPOSController#CORTES_RAPIDOS), empaquetadas en el classpath bajo
     * src/main/resources/img/productos/. Se usa para completar instalaciones que ya
     * existian antes de que productos.sql incluyera imagen_path (ver backfillImagenes).
     */
    private static final Map<String, String> IMAGENES_POR_DEFECTO = Map.of(
            "CER-001", "img/productos/panceta de cerdo.JPG",
            "CER-002", "img/productos/chuleta de cerdo.JPG",
            "CER-003", "img/productos/pierna de cerdo.JPG",
            "CER-004", "img/productos/brazo de cerdo.JPG",
            "CER-006", "img/productos/cerdo entero.JPG",
            "RES-002", "img/productos/lomo fino.JPG"
    );

    private SchemaInitializer() {
    }

    /**
     * Prepara la base de datos y devuelve true si esta instalacion no tenia ningun usuario
     * todavia (base de datos nueva: hay que mostrar el asistente de configuracion inicial en
     * vez del login, ver MainApp#start). Una instalacion que ya tenia usuarios (de una
     * version anterior a este asistente) sigue yendo directo al login normalmente.
     */
    public static boolean inicializarSiFalta() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean primerArranque = !existeTabla(conn, "USUARIOS");

            ejecutarScript(conn, "/db/schema.sql");
            if (primerArranque) {
                LOG.info("Primer arranque: sembrando el catalogo inicial de productos");
                ejecutarScript(conn, "/db/productos.sql");
            }
            backfillImagenesPorDefecto(conn);
            return primerArranque;
        } catch (SQLException | IOException e) {
            throw new RuntimeException("No se pudo preparar la base de datos en el primer arranque", e);
        }
    }

    /**
     * Completa imagen_path de instalaciones ya existentes (creadas antes de que
     * productos.sql incluyera esa columna), sin tocar nada si el producto ya tiene una
     * imagen asignada (propia o por defecto). Corre en cada arranque; es barato (solo
     * actualiza filas con imagen_path nulo) y seguro de repetir.
     */
    private static void backfillImagenesPorDefecto(Connection conn) throws SQLException {
        String sql = "UPDATE productos SET imagen_path = ? WHERE codigo = ? AND imagen_path IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entrada : IMAGENES_POR_DEFECTO.entrySet()) {
                ps.setString(1, entrada.getValue());
                ps.setString(2, entrada.getKey());
                ps.executeUpdate();
            }
        }
    }

    private static boolean existeTabla(Connection conn, String nombreTabla) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, nombreTabla, null)) {
            return rs.next();
        }
    }

    private static void ejecutarScript(Connection conn, String recurso) throws IOException, SQLException {
        String sql = leerRecursoSinComentarios(recurso);
        try (Statement st = conn.createStatement()) {
            for (String sentencia : sql.split(";")) {
                String limpio = sentencia.strip();
                if (!limpio.isEmpty()) {
                    st.execute(limpio);
                }
            }
        }
    }

    /** Lee un .sql del classpath quitando las lineas de comentario completas ("-- ..."). */
    private static String leerRecursoSinComentarios(String recurso) throws IOException {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(recurso)) {
            if (input == null) {
                throw new IOException("No se encontro el recurso " + recurso + " en el classpath");
            }
            StringBuilder contenido = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (!linea.strip().startsWith("--")) {
                        contenido.append(linea).append('\n');
                    }
                }
            }
            return contenido.toString();
        }
    }
}
