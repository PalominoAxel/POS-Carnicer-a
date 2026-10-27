# Registro de mejoras aplicadas (temas del curso)

Fecha: 2026-10-07. Orden de aplicación acordado: logging → backups → tests → Git → seguridad → documentación de despliegue.

## 1. Logging (monitoreo)

**Problema:** no existía ningún registro persistente de errores. Todo se mostraba una sola vez en un `Alert` y se perdía; esto fue justo lo que dificultó diagnosticar el incidente de la "caja huérfana" documentado antes.

**Qué se hizo:**
- Se agregó `logback-classic` como dependencia (`pom.xml`).
- `src/main/resources/logback.xml`: escribe a consola y a `logs/pos-dontoribio.log`, rotando por día y borrando logs de más de 30 días.
- Se agregaron logs en los puntos donde antes un error solo generaba un `Alert`:
  - `DatabaseConnection` (fallo al inicializar el pool de conexiones).
  - `VentaDAOImpl`, `CompraDAOImpl`, `CajaDAOImpl` (errores de transacción, incluido el caso de "no existe la tabla caja").
  - `LoginController` (fallo de conexión al iniciar sesión).
  - `MainApp`: handler global de excepciones no capturadas + log de inicio/cierre de la app.

**Cómo verificarlo:** correr el `.jar` y revisar `logs/pos-dontoribio.log` (se genera junto al jar, no se versiona en git).

## 2. Backups locales automatizados (cron jobs / backups / scripts)

**Problema:** no había ningún respaldo de la base de datos. Si se corrompe el disco de la máquina, se pierde todo el historial de ventas/compras.

**Qué se hizo** (en `scripts/`):
- `backup_bd.ps1`: lee la conexión real desde `src/main/resources/application.properties`, corre `pg_dump` y guarda el respaldo en `backups/` (carpeta no versionada). Borra respaldos de más de 30 días.
- `instalar_tarea_backup.ps1`: registra una tarea programada de Windows (el equivalente a un cron job) para correr el backup todos los días a las 23:30.

**Importante:** `instalar_tarea_backup.ps1` **no se ejecutó** — modifica el Programador de tareas de Windows, así que lo corrés vos cuando quieras (una sola vez, como administrador):
```powershell
.\scripts\instalar_tarea_backup.ps1
```
Requiere que `pg_dump` esté en el PATH (viene con la instalación de PostgreSQL, carpeta `bin`).

## 3. Pruebas unitarias (testing)

**Problema:** cero pruebas automatizadas en todo el proyecto.

**Qué se hizo:**
- Se agregó JUnit 5 + Mockito (`pom.xml`, scope `test`).
- `src/test/java/.../service/`: `VentaServiceTest`, `CajaServiceTest`, `UsuarioServiceTest` — 32 pruebas en total, usando los DAO simulados (sin tocar la base de datos real). Esto fue posible porque `VentaService`, `CajaService` y `UsuarioService` ya habían quedado con el DAO inyectable por constructor desde el refactor de capas anterior.

**Cómo correrlas:**
```powershell
mvn test
```

## 4. Git y GitHub (control de versiones)

**Qué se hizo:**
- Se completó `.gitignore` para que `logs/` y `backups/` (generados en runtime) nunca se versionen.
- Se corrigió la configuración local del repo: faltaba `origin/HEAD` apuntando a `main` (`git remote set-head origin main`), lo cual es necesario para que herramientas de revisión de diffs funcionen bien contra el remoto.

**Recomendación para adelante (hábito, no código):** mensajes de commit descriptivos en vez de "avance" / "Cambios totales" — por ejemplo "Agregar logging a archivo con Logback" en vez de un mensaje genérico. Así se puede auditar qué cambió y por qué mirando solo el historial.

## 5. Seguridad

Se corrió una revisión de seguridad enfocada en los cambios de esta sesión (RBAC, servicios, backups, logging). Encontró un problema real, que ya se corrigió:

**Hallazgo:** `AutorizacionSupervisor` (el diálogo de "autorización de supervisor" que agregamos para RBAC) decidía si saltarse el pedido de clave usando el objeto `Usuario` **en memoria** de la sesión abierta. Si un administrador era desactivado desde "Gestionar usuarios" mientras su sesión en el POS seguía abierta en otra máquina/ventana, esa sesión seguía pudiendo autorizar acciones de supervisor indefinidamente, porque nunca se volvía a comprobar su estado real en la base de datos.

**Corrección aplicada:**
- `UsuarioService.esAdministrador(...)` ahora también exige `activo = true` (antes solo miraba el rol).
- Se agregó `UsuarioService.esAdministradorVigente(...)`, que vuelve a consultar la base de datos por el `username` de la sesión en vez de confiar en el objeto en memoria. `AutorizacionSupervisor` ahora usa este método.
- Se agregaron pruebas que cubren exactamente este caso (`UsuarioServiceTest`), para que no vuelva a pasar sin que un test lo marque.

No se encontraron otros hallazgos de alta confianza (no hay inyección SQL — todo usa `PreparedStatement` — ni credenciales ni contraseñas en los logs nuevos).

## 6. Despliegue con Maven

Ya estaba implementado (ver `pom.xml`, plugin `maven-shade-plugin`). Se deja documentado el proceso para quien no lo haya hecho antes:

```powershell
# Compilar + correr los tests
mvn test

# Generar el jar ejecutable (incluye todas las dependencias)
mvn package -DskipTests

# Correrlo
java -jar target/pos-dontoribio.jar
```

El jar que hay que ejecutar es `target/pos-dontoribio.jar` (el shade plugin lo reemplaza por la versión "shaded", con todas las dependencias adentro). `application.properties` debe existir en `src/main/resources/` **antes** de empaquetar (se copia dentro del jar); no se versiona en git porque tiene las credenciales reales — cada instalación crea el suyo copiando `application.properties.example`.
- Documentación y pruebas de arquitectura por Rosa Alcalá.