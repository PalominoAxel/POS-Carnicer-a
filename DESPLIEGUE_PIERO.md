# Perfiles y variables de entorno del POS

Implementacion para el proyecto JavaFX con H2, preparada por Piero. El lector propio del POS carga application.properties, despues application-dev.properties o application-prod.properties. En dev, application-local.properties permite conservar configuraciones privadas. Las variables DB_URL, DB_USERNAME, DB_PASSWORD y DB_DRIVER prevalecen sobre los archivos. El perfil se selecciona mediante -Dpos.profile o SPRING_PROFILES_ACTIVE. Esta ultima variable se interpreta en PropertiesLoader.

## Desarrollo

El perfil predeterminado es dev. Conserva la base embebida en la carpeta de datos de la instalacion y el asistente inicial. Quienes tenian credenciales locales en application.properties deben moverlas a application-local.properties antes de integrar estos cambios. Esa variante local queda ignorada por Git. Los tres archivos comunes y de perfiles se versionan sin claves reales.

## Preparacion de produccion

1. Hacer copia de seguridad de la base de la instalacion.
2. Preparar el esquema y configurar el negocio y sus usuarios de aplicacion mediante la cuenta administradora. Esto se realiza antes de usar prod. Los perfiles no migran la base existente a otro motor.
3. Con H2 2.3.232 disponible, crear la cuenta JDBC con permisos minimos desde una terminal interactiva. El programa pide las claves sin mostrarlas.

```powershell
java -cp "RUTA/h2-2.3.232.jar" scripts/CrearUsuarioH2.java
```

La URL debe apuntar al archivo H2 preparado, por ejemplo jdbc:h2:file:C:/despliegue/proyecto/datos/dontoribio;IFEXISTS=TRUE. Sustituir la ruta por la instalacion real. Si la base ya tiene configurado el modo PostgreSQL, ese ajuste queda guardado en ella. Evitar MODE o AUTO_SERVER en la URL de la cuenta restringida porque pueden requerir permisos administrativos. Cerrar el POS durante esta preparacion. El usuario POS_APP debe ser nuevo. Si una concesion de permisos falla, el administrador debe revisar los permisos de la cuenta antes de usarla.

POS_APP recibe SELECT, INSERT, UPDATE y DELETE sobre las once tablas del esquema. El arranque prod omite DDL y siembra de datos. El test automatizado comprueba operaciones CRUD y rechazo de DROP TABLE y CREATE USER en una base H2 temporal.

## Arranque prod

En una terminal con el JAR ya construido:

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DB_URL='jdbc:h2:file:C:/despliegue/proyecto/datos/dontoribio;IFEXISTS=TRUE'
$env:DB_USERNAME='POS_APP'
$clave = Read-Host 'Clave de POS_APP' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $clave).Password
java -jar .\pos-dontoribio.jar
```

La URL es un ejemplo. Usar la ubicacion real de la base. Una variable ausente o vacia detiene la configuracion de produccion. La contrasena vive en el entorno del proceso. El pool registra el perfil activo y omite la URL del log.

## Comprobaciones y evidencia

```powershell
mvn "-Dtest=ProfilesConfigurationTest" test
```

La prueba cubre los perfiles, las credenciales externas, valores ausentes o vacios, precedencia y privilegios H2. Para la evidencia de la instalacion real, consultar desde una sesion administradora H2:

```sql
SELECT USER_NAME, IS_ADMIN FROM INFORMATION_SCHEMA.USERS WHERE USER_NAME = 'POS_APP';
SELECT GRANTEE, TABLE_SCHEMA, TABLE_NAME, RIGHTS FROM INFORMATION_SCHEMA.RIGHTS WHERE GRANTEE = 'POS_APP';
```

Capturar IS_ADMIN=false y los permisos por tabla, ocultando contrasenas. La creacion del usuario y la captura de la base de produccion requieren ejecutar los pasos en esa instalacion. Las pruebas automatizadas solo crean una base temporal.

El documento de Juana usa Spring Boot/MySQL. Esta rama adapta los objetivos de perfiles, variables y privilegios al POS JavaFX/H2 publicado. Los endpoints HTTP y el frontend Angular requieren la arquitectura correspondiente del equipo.
