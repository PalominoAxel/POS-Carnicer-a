package com.dontoribio.pos.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Properties;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class ProfilesConfigurationTest {
    private final ClassLoader recursos = getClass().getClassLoader();
    private Map<String, String> produccion() {
        return Map.of("SPRING_PROFILES_ACTIVE", "prod", "DB_URL", "jdbc:h2:mem:perfil",
                "DB_USERNAME", "pos_app", "DB_PASSWORD", "clave-solo-prueba");
    }

    @Test void devPermiteArranqueLocal() {
        var config = PropertiesLoader.cargar(recursos, Map.of(), new Properties());
        assertEquals("dev", config.getProperty("app.profile"));
        assertEquals("true", config.getProperty("db.schema.initialize"));
    }
    @Test void entornoPrevaleceSobreArchivos() {
        var config = PropertiesLoader.cargar(recursos, Map.of("DB_USERNAME", "externo"), new Properties());
        assertEquals("externo", config.getProperty("db.user"));
    }
    @Test void prodResuelveCredencialesExternasYDesactivaDDL() {
        var config = PropertiesLoader.cargar(recursos, produccion(), new Properties());
        assertEquals("pos_app", config.getProperty("db.user"));
        assertEquals("clave-solo-prueba", config.getProperty("db.password"));
        assertEquals("false", config.getProperty("db.schema.initialize"));
    }
    @Test void passwordExternaSeConservaLiteral() {
        var entorno = new java.util.HashMap<>(produccion());
        entorno.put("DB_PASSWORD", "${texto-literal}");
        assertEquals("${texto-literal}", PropertiesLoader.cargar(recursos, entorno,
                new Properties()).getProperty("db.password"));
    }
    @Test void prodRechazaPasswordAusente() {
        var entorno = new java.util.HashMap<>(produccion());
        entorno.remove("DB_PASSWORD");
        var error = assertThrows(IllegalArgumentException.class,
                () -> PropertiesLoader.cargar(recursos, entorno, new Properties()));
        assertTrue(error.getMessage().contains("DB_PASSWORD"));
    }
    @Test void prodRechazaPasswordVacia() {
        var entorno = new java.util.HashMap<>(produccion());
        entorno.put("DB_PASSWORD", " ");
        assertThrows(IllegalArgumentException.class,
                () -> PropertiesLoader.cargar(recursos, entorno, new Properties()));
    }
    @Test void rechazaPerfilDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> PropertiesLoader.cargar(
                recursos, Map.of("SPRING_PROFILES_ACTIVE", "otro"), new Properties()));
    }
    @Test void propiedadJavaSeleccionaPerfil() {
        Properties sistema = new Properties();
        sistema.setProperty("pos.profile", "dev");
        var config = PropertiesLoader.cargar(recursos, produccion(), sistema);
        assertEquals("dev", config.getProperty("app.profile"));
    }
    @Test void usuarioH2RestringidoOperaDatosSinAdministrarEsquema() throws Exception {
        String url = "jdbc:h2:mem:privilegios_piero";
        try (var admin = DriverManager.getConnection(url, "sa", ""); var sql = admin.createStatement()) {
            sql.execute("CREATE TABLE productos_prueba(id INT PRIMARY KEY, nombre VARCHAR(60))");
            try (var crear = admin.prepareStatement("CREATE USER pos_app PASSWORD ?")) {
                crear.setString(1, "clave-solo-prueba");
                crear.execute();
            }
            sql.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON productos_prueba TO pos_app");
            try (var app = DriverManager.getConnection(url, "pos_app", "clave-solo-prueba");
                 var consulta = app.createStatement()) {
                consulta.executeUpdate("INSERT INTO productos_prueba VALUES (1, 'Corte')");
                consulta.executeUpdate("UPDATE productos_prueba SET nombre='Res' WHERE id=1");
                try (var filas = consulta.executeQuery("SELECT nombre FROM productos_prueba WHERE id=1")) {
                    assertTrue(filas.next());
                    assertEquals("Res", filas.getString(1));
                }
                assertThrows(SQLException.class, () -> consulta.execute("DROP TABLE productos_prueba"));
                assertThrows(SQLException.class, () -> consulta.execute("CREATE USER otro PASSWORD 'prueba'"));
                assertEquals(1, consulta.executeUpdate("DELETE FROM productos_prueba WHERE id=1"));
            }
        }
    }
}
