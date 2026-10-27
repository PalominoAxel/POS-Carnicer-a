# Trabajar en GitHub Codespaces

1. En GitHub: **Code → Codespaces → Create codespace on main**.
2. Espera a que termine `postCreateCommand` (descarga dependencias y compila).
3. En la terminal del Codespace:

   ```bash
   mvn test          # corre las pruebas unitarias
   mvn javafx:run    # abre la aplicacion
   ```

4. Para **ver la ventana** de la app: pestaña **Ports** → puerto **6080 (Escritorio noVNC)** →
   abrir en el navegador. Contraseña por defecto del escritorio: `vscode`.

La app usa una base de datos H2 embebida, asi que no hay que instalar ni configurar
PostgreSQL dentro del Codespace. La balanza funciona en modo `SIMULADA`.

> Recuerda detener el Codespace cuando termines (Codespaces → ... → Stop) para no
> consumir horas de la cuota gratuita.
