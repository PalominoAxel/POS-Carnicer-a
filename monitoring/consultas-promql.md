# Consultas PromQL del POS Don Toribio

Prometheus: http://localhost:9090 (job `pos-dontoribio`).

| N.° | Consulta | Qué mide |
|---|---|---|
| Q1 | `up{job="pos-dontoribio"}` | Disponibilidad (1 = UP, 0 = caído) |
| Q2 | `rate(pos_ventas_total[1m]) * 60` | Ventas por minuto |
| Q3 | `histogram_quantile(0.95, sum by (le) (rate(pos_reporte_excel_seconds_bucket[1m])))` | Latencia p95 de reportes Excel (s) |
| Q4 | `jvm_memory_used_bytes{area="heap"} / 1024 / 1024` | Heap por región (MB) |
| Q5 | `sum(jvm_memory_used_bytes{area="heap"}) / 1024 / 1024` | Heap total (MB) |

Q2 y Q3 muestran datos cuando los servicios llaman a `PosMonitoringServer.VENTAS_OK` y `registrarTiempoReporte()`.
Mientras tanto, `scripts/DemoMetricas.java` genera tráfico simulado para validar la recolección.
