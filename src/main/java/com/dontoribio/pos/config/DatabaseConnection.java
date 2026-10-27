package com.dontoribio.pos.config;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto unico de acceso al pool de conexiones HikariCP. Por defecto usa una base de datos
 * H2 embebida en un archivo propio de cada instalacion (carpeta de datos del usuario), para
 * que la app funcione sin instalar ningun servidor de base de datos ni crear ningun archivo
 * de configuracion a mano. H2 corre en modo PostgreSQL para que la mayoria del SQL existente
 * (BIGSERIAL, NOW(), ALTER ... ADD COLUMN IF NOT EXISTS, etc.) funcione sin cambios; la
 * excepcion es RETURNING, que H2 no soporta y los DAO reemplazaron por getGeneratedKeys().
 * Si existe application.properties con un "db.url" propio (uso de desarrollo, o para quien
 * todavia quiera apuntar a un Postgres real), ese valor tiene prioridad.
 * Los DAO obtienen conexiones a traves de {@link #getConnection()} y nunca
 * instancian el DataSource directamente.
 */
public final class DatabaseConnection {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);

    private static HikariDataSource dataSource;

    /**
     * Error de inicializacion, si lo hubo (p. ej. falta application.properties o Postgres no
     * esta corriendo). Se cachea en vez de dejar que la primera falla reviente un bloque static:
     * eso produciria un ExceptionInInitializerError que ningun catch(RuntimeException) de la app
     * puede atrapar, y el boton de login/crear usuario "no haria nada". Asi, cada intento lanza
     * una RuntimeException normal y con mensaje claro.
     */
    private static RuntimeException errorInicializacion;

    private DatabaseConnection() {
    }

    private static synchronized HikariDataSource obtenerDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        if (errorInicializacion != null) {
            throw errorInicializacion;
        }
        try {
            HikariConfig config = new HikariConfig();
            // db.url solo esta presente si alguien creo application.properties a mano
            // (desarrollo, o para seguir usando un Postgres real). Si no existe, se usa el
            // H2 embebido por defecto y la app funciona sin ningun archivo de configuracion.
            String urlPropia = PropertiesLoader.get("db.url", null);
            if (urlPropia != null && !urlPropia.isBlank()) {
                config.setJdbcUrl(urlPropia);
                config.setUsername(PropertiesLoader.get("db.user", "sa"));
                config.setPassword(PropertiesLoader.get("db.password", ""));
                config.setDriverClassName(PropertiesLoader.get("db.driver", "org.h2.Driver"));
            } else {
                config.setJdbcUrl(construirUrlEmbebidaPorDefecto());
                config.setUsername("sa");
                config.setPassword("");
                config.setDriverClassName("org.h2.Driver");
            }
            /** Define el numero maximo de conexiones que mantendrá el pool*/
            config.setMaximumPoolSize(PropertiesLoader.getInt("db.pool.maximumPoolSize", 10));
            config.setMinimumIdle(PropertiesLoader.getInt("db.pool.minimumIdle", 2));
            config.setConnectionTimeout(PropertiesLoader.getLong("db.pool.connectionTimeout", 30000));
            config.setIdleTimeout(PropertiesLoader.getLong("db.pool.idleTimeout", 600000));
            config.setMaxLifetime(PropertiesLoader.getLong("db.pool.maxLifetime", 1800000));
            config.setPoolName(PropertiesLoader.get("db.pool.name", "DonToribioPool"));

            dataSource = new HikariDataSource(config);
            LOG.info("Pool de conexiones '{}' inicializado contra {}", config.getPoolName(), config.getJdbcUrl());
            return dataSource;
        } catch (RuntimeException e) {
            LOG.error("No se pudo inicializar el pool de conexiones a la base de datos", e);
            errorInicializacion = new RuntimeException(
                    "No se pudo conectar con la base de datos. Verifique que exista "
                            + "application.properties con las credenciales correctas y que "
                            + "PostgreSQL este en ejecucion.", e);
            throw errorInicializacion;
        }
    }

    /**
     * URL del H2 embebido dentro de la carpeta de datos de esta instalacion
     * ({@link AppPaths#carpetaDatos()}), no dentro de la carpeta de instalacion del
     * programa, para que sobreviva reinstalaciones/updates.
     */
    private static String construirUrlEmbebidaPorDefecto() {
        String rutaArchivo = new File(AppPaths.carpetaDatos(), "dontoribio").getAbsolutePath().replace('\\', '/');
        return "jdbc:h2:file:" + rutaArchivo + ";MODE=PostgreSQL;AUTO_SERVER=TRUE";
    }

    public static Connection getConnection() throws SQLException {
        return obtenerDataSource().getConnection();
    }

    /** Cierra el pool. Debe invocarse al finalizar la aplicacion (MainApp#stop). */
    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
