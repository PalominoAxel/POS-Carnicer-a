import java.io.Console;
import java.sql.DriverManager;
import java.util.Arrays;

/** Ejecutar localmente con JDK 17+ y el driver H2 en el classpath. */
class CrearUsuarioH2 {
    public static void main(String[] args) throws Exception {
        Console consola = System.console();
        if (consola == null) throw new IllegalStateException("Ejecuta desde una terminal interactiva");
        String url = consola.readLine("URL JDBC de la base preparada (sin MODE ni AUTO_SERVER): ");
        String admin = consola.readLine("Usuario administrador H2: ");
        char[] claveAdmin = consola.readPassword("Clave del administrador: ");
        char[] claveApp = consola.readPassword("Nueva clave de POS_APP: ");
        if (claveApp.length == 0) throw new IllegalArgumentException("La clave de POS_APP es obligatoria");
        try (var conexion = DriverManager.getConnection(url, admin, new String(claveAdmin))) {
            try (var crear = conexion.prepareStatement("CREATE USER POS_APP PASSWORD ?")) {
                crear.setString(1, new String(claveApp));
                crear.execute();
            }
            // Solo los objetos existentes del esquema del POS reciben permisos.
            try (var permisos = conexion.createStatement()) {
                for (String tabla : new String[]{"usuarios", "clientes", "productos", "proveedores",
                        "ventas", "detalle_ventas", "pagos", "compras", "detalle_compras", "caja", "configuracion"}) {
                    permisos.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON " + tabla + " TO POS_APP");
                }
            }
            System.out.println("POS_APP creado con permisos de datos. Verifica INFORMATION_SCHEMA.RIGHTS.");
        } finally {
            Arrays.fill(claveAdmin, '\0');
            Arrays.fill(claveApp, '\0');
        }
    }
}
