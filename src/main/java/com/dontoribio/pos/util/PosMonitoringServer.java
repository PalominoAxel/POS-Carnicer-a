package com.dontoribio.pos.util;
import com.dontoribio.pos.config.DatabaseConnection;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PosMonitoringServer {

    private static final Logger log = LoggerFactory.getLogger(PosMonitoringServer.class);
    public static final PrometheusMeterRegistry REGISTRY = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // Medidores base listos para que el Integrante 3 los conecte a VentaService y ReporteExcelService
    public static final Counter VENTAS_OK = Counter.builder("pos_ventas_total")
            .tag("estado", "ok")
            .description("Total de ventas registradas exitosamente")
            .register(REGISTRY);

    public static final Counter VENTAS_ERROR = Counter.builder("pos_ventas_errores_total")
            .description("Total de errores en registro de ventas o BD")
            .register(REGISTRY);

    public static final Timer REPORTE_TIMER = Timer.builder("pos_reporte_excel_seconds")
            .publishPercentileHistogram()
            .description("Tiempo de generacion de reportes Excel")
            .register(REGISTRY);

    private static HttpServer server;

    public static void iniciar(int puerto) {
        try {
            // Métricas nativas de la JVM y procesador
            new JvmMemoryMetrics().bindTo(REGISTRY);
            new JvmGcMetrics().bindTo(REGISTRY);
            new JvmThreadMetrics().bindTo(REGISTRY);
            new ProcessorMetrics().bindTo(REGISTRY);

            server = HttpServer.create(new InetSocketAddress(puerto), 0);

            // Endpoint 1: Health Check validando conexión activa a BD (HikariCP)
            server.createContext("/health", exchange -> {
                boolean dbActiva = verificarConexionBD();
                int statusCode = dbActiva ? 200 : 503;
                String json = dbActiva
                        ? "{\"status\":\"UP\",\"app\":\"pos-dontoribio\",\"database\":\"CONNECTED\"}"
                        : "{\"status\":\"DOWN\",\"app\":\"pos-dontoribio\",\"database\":\"DISCONNECTED\"}";

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            // Endpoint 2: Métricas en formato Prometheus
            server.createContext("/prometheus", exchange -> {
                String response = REGISTRY.scrape();
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            server.setExecutor(null);
            server.start();
            log.info("Agente de monitoreo POS iniciado en http://localhost:{}/health y /prometheus", puerto);
        } catch (IOException e) {
            log.error("No se pudo iniciar el servidor de monitoreo POS en el puerto {}", puerto, e);
        }
    }

    public static void detener() {
        if (server != null) {
            server.stop(0);
            log.info("Agente de monitoreo POS detenido");
        }
    }

    public static void registrarTiempoReporte(long milisegundos) {
        REPORTE_TIMER.record(milisegundos, TimeUnit.MILLISECONDS);
    }

    private static boolean verificarConexionBD() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            log.warn("Fallo en health check de base de datos: {}", e.getMessage());
            return false;
        }
    }
}
