# Guia de trabajo con Git y GitHub

Esta guia define como colabora el equipo en el repositorio **POS-Carnicer-a**.

## 1. Ramas

| Rama | Uso |
|------|-----|
| `main` | Version estable. No se hace commit directo: todo entra por Pull Request. |
| `feature/<tema>` | Nueva funcionalidad. Ej.: `feature/exportacion-excel-poi` |
| `fix/<tema>` | Correccion de un error. Ej.: `fix/vuelto-negativo` |
| `docs/<tema>` | Solo documentacion. |
| `mejora/<tema>` | Configuracion, limpieza o mantenimiento. |

## 2. Flujo de trabajo (GitHub Flow)

```bash
git switch main
git pull origin main                      # 1. partir de main actualizado
git switch -c feature/mi-cambio           # 2. crear la rama
# ... editar codigo ...
mvn test                                  # 3. probar en local
git add .
git commit -m "feat: agrega exportacion de compras a Excel"   # 4. commit descriptivo
git push -u origin feature/mi-cambio      # 5. subir la rama
```

6. En GitHub: **Compare & pull request**, completar la plantilla y pedir revision.
7. Esperar el **check verde del CI** (GitHub Actions compila y corre las pruebas).
8. Un companero revisa y aprueba; luego **Merge pull request** y borrar la rama.

## 3. Mensajes de commit (Conventional Commits)

Formato: `tipo: descripcion en presente`

| Tipo | Cuando usarlo |
|------|---------------|
| `feat` | Nueva funcionalidad |
| `fix` | Correccion de error |
| `docs` | Documentacion |
| `test` | Pruebas |
| `refactor` | Cambio interno sin alterar comportamiento |
| `chore` | Configuracion, dependencias, limpieza |

Evitar mensajes como `avance`, `cambios`, `completo`: no explican que cambio.

## 4. Que NO se sube al repositorio

Lo controla `.gitignore`:

- `target/` y `dependency-reduced-pom.xml` (se generan con Maven)
- `src/main/resources/application.properties` y cualquier `*.bak` (pueden tener claves)
- `logs/` y `backups/`
- configuracion personal del IDE (`.idea/`, `.vscode/`)

## 5. Resolver conflictos

```bash
git switch feature/mi-cambio
git pull origin main          # trae lo nuevo de main a tu rama
# corregir los archivos marcados con <<<<<<< ======= >>>>>>>
mvn test
git add .
git commit -m "chore: resuelve conflicto con main"
git push
```

Revisar siempre `pom.xml` despues de un conflicto: si se pierde una dependencia, el CI lo detecta.

## 6. Versiones

Cada entrega estable se marca con una etiqueta semantica y se publica como *Release*:

```bash
git tag -a v1.0.0 -m "Primera version estable"
git push origin v1.0.0
```

## 7. GitHub Codespaces

Para programar sin instalar nada: **Code → Codespaces → Create codespace on main**.
Ver [.devcontainer/README.md](.devcontainer/README.md).
