# Don Toribio POS — Sistema de punto de venta para carnicería

![Java](https://img.shields.io/badge/Java-17-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-21-blue) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Base_de_datos-336791) ![Maven](https://img.shields.io/badge/Maven-Gesti%C3%B3n_de_dependencias-red)

## 1. Descripción general

**Don Toribio POS** es una aplicación de escritorio para la **Comercializadora de Carnes Don Toribio S.A.C.**, diseñada para apoyar las operaciones de venta y pesaje de productos cárnicos. Reúne en una interfaz gráfica el registro de operaciones comerciales, la administración de productos, las compras a proveedores y el seguimiento de caja.

A diferencia de un punto de venta genérico, el proyecto contempla el **pesaje de productos** mediante una balanza conectada por puerto serial, además de un modo simulado que permite trabajar sin disponer del equipo físico durante el desarrollo o las demostraciones.

El sistema está implementado en **Java 17** con **JavaFX y FXML** para las pantallas, **PostgreSQL** para la persistencia de información y **Maven** para compilar, ejecutar y administrar dependencias.

## 2. Problema que busca resolver

En una carnicería, cada venta puede depender del peso del producto, su precio y la disponibilidad del inventario. Cuando los registros se manejan de forma separada, resulta más difícil consultar ventas anteriores, controlar movimientos de caja o mantener ordenado el catálogo.

Don Toribio POS propone centralizar estas tareas en una aplicación de escritorio, con una estructura que separa la interfaz, las reglas de negocio y el acceso a los datos. Su objetivo es facilitar el trabajo diario del personal y mejorar la trazabilidad de las operaciones.

## 3. Objetivos

- Facilitar el registro y la consulta de ventas de productos cárnicos.
- Integrar el pesaje de productos al flujo de atención.
- Organizar la información de productos, clientes y proveedores.
- Registrar compras y apoyar el seguimiento del abastecimiento.
- Administrar usuarios y controles de acceso.
- Consultar el historial de operaciones y los movimientos de caja.
- Mantener una base técnica que permita ampliar y probar el sistema.

## 4. Módulos del sistema

| Módulo | Descripción |
|---|---|
| **Inicio de sesión** | Pantalla de autenticación y acceso a las funciones del sistema. |
| **Punto de venta** | Interfaz principal para la atención y registro de operaciones de venta. |
| **Catálogo** | Consulta y administración de la información de los productos. |
| **Compras** | Registro de compras y manejo de información vinculada con proveedores. |
| **Control de caja** | Funciones de seguimiento de la caja y sus movimientos. |
| **Historial de ventas** | Consulta de operaciones registradas anteriormente. |
| **Gestión de usuarios** | Creación y administración de cuentas de acceso. |
| **Balanza** | Servicios para obtener pesos desde un dispositivo real o desde un simulador. |
| **Comprobantes** | Servicio para generar tickets asociado al proceso de venta. |

Estas secciones se corresponden con controladores, modelos y servicios presentes en el código fuente. La disponibilidad de cada función en una instalación concreta depende de la configuración de la base de datos y del entorno.

## 5. Tecnologías y dependencias principales

| Tecnología | Uso en el proyecto |
|---|---|
| Java 17 | Lenguaje de programación y lógica de la aplicación. |
| JavaFX 21.0.4 | Interfaz gráfica de escritorio. |
| FXML | Definición de vistas separadas de sus controladores. |
| PostgreSQL | Base de datos relacional. |
| JDBC | Comunicación con PostgreSQL. |
| HikariCP | Administración del pool de conexiones. |
| Maven | Gestión de dependencias, compilación y ejecución. |
| jSerialComm | Comunicación serial con la balanza. |
| BCrypt | Tratamiento seguro de contraseñas. |
| ControlsFX y AtlantaFX | Componentes y presentación visual de la interfaz. |
| Logback | Registro de eventos de la aplicación. |
| JUnit 5 y Mockito | Pruebas unitarias y simulación de dependencias. |

## 6. Arquitectura del proyecto

El código sigue una organización por responsabilidades:

```text
src/
├── main/
│   ├── java/com/dontoribio/pos/
│   │   ├── config/         # Conexión y lectura de configuración
│   │   ├── controller/     # Controladores de las pantallas JavaFX
│   │   ├── dao/            # Interfaces de acceso a datos
│   │   │   └── impl/       # Implementaciones de los DAO
│   │   ├── model/          # Entidades del dominio comercial
│   │   ├── service/        # Lógica de negocio y servicios externos
│   │   ├── util/           # Utilidades y autorización
│   │   ├── Launcher.java
│   │   └── MainApp.java
│   └── resources/
│       ├── application.properties.example
│       ├── db/            # Scripts SQL
│       └── com/dontoribio/pos/view/  # Pantallas FXML
└── test/java/com/dontoribio/pos/service/
    ├── CajaServiceTest.java
    ├── UsuarioServiceTest.java
    └── VentaServiceTest.java
```

**Flujo conceptual:** el usuario interactúa con las pantallas FXML; sus controladores invocan los servicios de negocio; los servicios y DAO trabajan con los modelos y la base de datos PostgreSQL. El servicio de balanza encapsula la lectura del peso, para permitir una implementación real y otra simulada.

## 7. Requisitos para ejecutar el proyecto

- **JDK 17** instalado y configurado.
- **Apache Maven** disponible desde la terminal.
- **PostgreSQL** instalado y en funcionamiento.
- Un IDE como **Visual Studio Code** con extensiones para Java, o IntelliJ IDEA.
- Para usar la balanza real: dispositivo y puerto serial compatibles, con la configuración adecuada.

## 8. Instalación y configuración

### Paso 1. Obtener el código

```bash
git clone https://github.com/PalominoAxel/POS-Carnicer-a.git
cd POS-Carnicer-a
```

> Si descargaste el proyecto como ZIP, descomprímelo y abre la carpeta que contiene `pom.xml`.

### Paso 2. Preparar PostgreSQL

Crea una base de datos denominada `dontoribio_pos` y revisa los scripts incluidos en:

```text
src/main/resources/db/schema.sql
src/main/resources/db/productos.sql
src/main/resources/db/roles.sql
```

El esquema define la estructura de la base de datos; los otros scripts contienen datos o configuración complementaria. Revisa el contenido y el orden de ejecución antes de aplicarlos en tu entorno. Utiliza una cuenta de aplicación con permisos restringidos, no el superusuario de PostgreSQL.

### Paso 3. Configurar la conexión

Copia el archivo:

```text
src/main/resources/application.properties.example
```

con el nombre:

```text
src/main/resources/application.properties
```

Edita sus valores según tu instalación:

```properties
db.url=jdbc:postgresql://localhost:5432/dontoribio_pos
db.user=pos_app
db.password=TU_CONTRASENA

balanza.puerto=COM3
balanza.baudRate=9600
balanza.modo=SIMULADA

seguridad.clave_recuperacion=TU_CLAVE_SEGURA
```

**Importante:** no publiques contraseñas reales ni subas tu `application.properties` personal al repositorio. El proyecto incluye un archivo de ejemplo para compartir la estructura de configuración sin revelar credenciales.

### Paso 4. Compilar y ejecutar

Desde la carpeta raíz del proyecto:

```bash
mvn clean compile
mvn javafx:run
```

Para ejecutar las pruebas:

```bash
mvn test
```

Para generar el paquete mediante Maven:

```bash
mvn clean package
```

El archivo `pom.xml` incluye configuración para crear un JAR mediante Maven Shade. La ejecución final del paquete puede requerir ajustes según el sistema operativo y la distribución de Java utilizada.

## 9. Flujo de uso de referencia

1. El personal inicia sesión con una cuenta autorizada.
2. Desde la pantalla principal accede al punto de venta o a los módulos administrativos.
3. Consulta el catálogo y selecciona los productos que necesita registrar.
4. Para productos vendidos por peso, utiliza el servicio de balanza real o simulada, según la configuración.
5. Registra la operación comercial y puede consultar su historial.
6. Las áreas administrativas permiten revisar compras, usuarios y control de caja.

Este recorrido resume el propósito de los módulos identificados en el código; no sustituye las pruebas funcionales en un entorno instalado.

## 10. Calidad, seguridad y mantenimiento

El repositorio incorpora varios elementos orientados al mantenimiento del software:

- **Pruebas unitarias:** clases de prueba para los servicios de caja, usuarios y ventas.
- **Separación de capas:** controladores, servicios, modelos y objetos DAO independientes.
- **Contraseñas:** dependencia BCrypt para el tratamiento de credenciales.
- **Conexiones:** HikariCP para gestionar conexiones a PostgreSQL.
- **Configuración externa:** archivo de ejemplo para evitar compartir secretos reales.
- **Respaldo:** scripts PowerShell en `scripts/` para tareas relacionadas con copias de seguridad de la base de datos.
- **Registro de eventos:** integración de Logback para diagnóstico.

La presencia de estos componentes no implica por sí sola que el sistema esté certificado para producción; antes de utilizarlo comercialmente deben validarse permisos, respaldos, recuperación, conexión con hardware y flujos completos de venta.

## 11. Capturas de pantalla

Se recomienda incorporar imágenes reales del sistema en ejecución para mostrar:

- Inicio de sesión.
- Pantalla principal del punto de venta.
- Catálogo de productos.
- Registro de compras.
- Historial de ventas y control de caja.

Cuando dispongas de ellas, puedes guardarlas en `docs/capturas/` e insertarlas en esta sección con Markdown, por ejemplo:

```markdown
![Pantalla principal](docs/capturas/pantalla-principal.png)
```

## 12. Alcance y posibles mejoras

El proyecto proporciona una base para digitalizar las operaciones de una carnicería. Como líneas de evolución se pueden considerar reportes gerenciales, alertas de inventario, nuevas opciones de impresión, una instalación más sencilla y pruebas integrales con diferentes modelos de balanza.

Estas ideas se presentan como **posibles mejoras futuras**, no como funcionalidades ya implementadas.

## 13. Repositorio y documentación adicional

- [Repositorio del proyecto](https://github.com/PalominoAxel/POS-Carnicer-a)
- [Historial de commits](https://github.com/PalominoAxel/POS-Carnicer-a/commits/principal/)
- [Documento de mejoras del curso](MEJORAS-CURSO.md)

---

**Don Toribio POS** — Proyecto de software de escritorio para la gestión de ventas y operaciones de una carnicería.
