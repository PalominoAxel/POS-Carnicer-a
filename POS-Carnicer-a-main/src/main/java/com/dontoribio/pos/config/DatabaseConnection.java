package com.dontoribio.pos.config;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto unico de acceso al pool de conexiones HikariCP hacia PostgreSQL.
 * Los DAO obtienen conexiones a traves de {@link #getConnection()} y nunca
 * instancian el DataSource directamente.
 */
public final class DatabaseConnection {
/**
 * Unica instancia privada y estatica de la fuente de datos HikariDataSource
 * 
*/
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);

    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(PropertiesLoader.get("db.url"));
        config.setUsername(PropertiesLoader.get("db.user"));
        config.setPassword(PropertiesLoader.get("db.password"));
        config.setDriverClassName(PropertiesLoader.get("db.driver", "org.postgresql.Driver"));
/** Define el numero maximo de conexiones que mantendrá el pool*/
        config.setMaximumPoolSize(PropertiesLoader.getInt("db.pool.maximumPoolSize", 10));
        config.setMinimumIdle(PropertiesLoader.getInt("db.pool.minimumIdle", 2));
        config.setConnectionTimeout(PropertiesLoader.getLong("db.pool.connectionTimeout", 30000));
        config.setIdleTimeout(PropertiesLoader.getLong("db.pool.idleTimeout", 600000));
        config.setMaxLifetime(PropertiesLoader.getLong("db.pool.maxLifetime", 1800000));
        config.setPoolName(PropertiesLoader.get("db.pool.name", "DonToribioPool"));

        try {
            DATA_SOURCE = new HikariDataSource(config);
            LOG.info("Pool de conexiones '{}' inicializado contra {}", config.getPoolName(), config.getJdbcUrl());
        } catch (RuntimeException e) {
            LOG.error("No se pudo inicializar el pool de conexiones a la base de datos", e);
            throw e;
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /** Cierra el pool. Debe invocarse al finalizar la aplicacion (MainApp#stop). */
    public static void cerrarPool() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}
