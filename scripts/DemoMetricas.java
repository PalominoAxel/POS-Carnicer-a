import com.dontoribio.pos.util.PosMonitoringServer;
import java.util.concurrent.ThreadLocalRandom;

/** Genera trafico SIMULADO de ventas y reportes Excel para validar Prometheus sin abrir la interfaz del POS. */
public class DemoMetricas {
    public static void main(String[] args) throws Exception {
        PosMonitoringServer.iniciar(8080);
        System.out.println("Demo de metricas en http://localhost:8080/prometheus (Ctrl+C para detener)");
        while (true) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            PosMonitoringServer.VENTAS_OK.increment();
            if (r.nextInt(10) == 0) {
                PosMonitoringServer.VENTAS_ERROR.increment();
            }
            PosMonitoringServer.registrarTiempoReporte(200 + r.nextInt(1800));
            Thread.sleep(1000);
        }
    }
}
