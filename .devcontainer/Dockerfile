# Imagen base de Java 17 para el Codespace.
# La imagen trae registrado el repositorio APT de Yarn, cuya firma ya no es valida;
# eso hace fallar "apt-get update" al instalar el escritorio remoto (desktop-lite).
# El proyecto no usa Yarn, asi que se quita ese repositorio antes de instalar las features.
FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm

RUN rm -f /etc/apt/sources.list.d/yarn.list
