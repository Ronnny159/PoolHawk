# PoolHawk

[![Maven CI](https://github.com/Ronnny159/PoolHawk/actions/workflows/maven.yml/badge.svg?branch=develop)](https://github.com/Ronnny159/PoolHawk/actions/workflows/maven.yml)

PoolHawk es un marcador automático de partidas de billar mediante visión por computadora. Elimina el conteo manual y garantiza la aplicación objetiva de las reglas (Bola 8 y Por Puntos). Los tiros se registran por una API REST, pasan por el motor de reglas y el marcador se actualiza en vivo en el navegador mediante WebSockets.

**Stack:** Java 21 + Spring Boot (Maven multi-módulo), Vue 3 (Vite), PostgreSQL y Docker.

## Estructura del repositorio

```
PoolHawk/
├── backend/          # Maven multi-módulo
│   ├── billiard-domain/   # Modelo de dominio
│   ├── billiard-rules/    # Motor de reglas (Bola 8, Por Puntos)
│   └── billiard-api/      # API REST, persistencia JPA y WebSocket
├── frontend/         # Aplicación Vue 3
├── tracking-agent/   # Simulador de cámara en Python
├── docs/             # Documentación y diagramas
├── .env.example      # Plantilla de variables de entorno
└── docker-compose.yml
```

## Requisitos previos

- Java 21
- Node.js 22 y npm
- Docker Desktop (con Docker Compose)
- Python 3.10 o superior (solo para el simulador de tracking)

## Configuración inicial: archivo .env

Las credenciales de la base de datos se leen de un archivo `.env` en la raíz. Antes de la primera ejecución, créalo a partir de la plantilla:

```bash
cp .env.example .env
```

En Windows (PowerShell): `Copy-Item .env.example .env`. Luego edita `.env` y ajusta los valores. Si falta alguna variable, `docker compose` se detiene con un mensaje claro.

## Opción 1: levantar todo con Docker

Desde la raíz del repositorio:

```bash
docker compose up -d --build
```

La primera vez tarda unos minutos porque compila el backend y el frontend.

| Servicio   | URL / Puerto            |
|------------|-------------------------|
| Frontend   | http://localhost:5173   |
| Backend    | http://localhost:8080   |
| PostgreSQL | localhost:5433          |
| Adminer    | http://localhost:8081   |

Para detener todo: `docker compose down` (agrega `-v` para borrar también los datos).

## Opción 2: modo desarrollo (paso a paso)

En este modo solo la base de datos corre en Docker; el backend y el frontend corren en tu máquina. Si ya levantaste todo con la opción 1, detén antes esos contenedores para liberar los puertos 8080 y 5173:

```bash
docker compose stop backend frontend
```

### 1. Base de datos

```bash
docker compose up -d postgres
```

### 2. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En Linux o macOS usa `./mvnw spring-boot:run`. La API queda en http://localhost:8080. En este modo el backend usa los datos de conexión de `application.yml`, que deben coincidir con tu `.env`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

La aplicación queda en http://localhost:5173.

## Simulador de tracking (Python)

El script `tracking-agent/simulador.py` simula las lecturas de la cámara: crea una partida de Bola 8 y envía 10 tiros a la API, con una pausa de 2 segundos entre cada uno.

```bash
cd tracking-agent
python -m pip install -r requirements.txt
python simulador.py
```

Opciones:

```bash
# Usar otra URL del backend
python simulador.py --base-url http://localhost:8080

# Disparar tiros sobre una partida que ya existe (por ejemplo, la abierta en el navegador)
python simulador.py --match-id <id-de-la-partida>
```

Para ver el marcador en vivo, abre el frontend en una ventana y ejecuta el simulador en otra: el marcador se actualiza solo gracias al WebSocket.

## Pruebas

```powershell
cd backend
.\mvnw.cmd test
```

## Seguridad básica

Este proyecto es un prototipo de demostración. Varias protecciones se dejaron abiertas a propósito para que la API y el marcador en vivo funcionen sin bloqueos durante la presentación. Esta sección describe con exactitud qué está protegido y qué no.

### Lo que sí está protegido

- La contraseña de PostgreSQL (`POSTGRES_PASSWORD`) del servicio de base de datos se lee del archivo `.env`. Si falta, `docker compose` se detiene con un mensaje claro en lugar de usar valores en blanco.
- `.env` está ignorado por Git y no se sube al repositorio. `.env.example` es la plantilla que sí se versiona: cópiala a `.env` y ajusta los valores.
- Si cambias la contraseña después de haber creado la base, borra el volumen (`docker compose down -v`), porque PostgreSQL solo lee sus credenciales al inicializarse.
- No hay ninguna configuración que active el stacktrace en las respuestas de error. Spring Boot lo oculta por defecto.

### Lo que está abierto para la demostración

- **La API no exige autenticación.** `SecurityConfig` permite todas las peticiones (`anyRequest().permitAll()`) y el filtro JWT no forma parte de la cadena de seguridad. La pantalla de inicio de sesión del frontend es solo una pantalla de entrada: no protege los endpoints.
- **CORS abierto.** `SecurityConfig` acepta cualquier origen y el endpoint `/ws/matches` usa `setAllowedOrigins("*")`, de modo que el marcador en vivo se puede abrir desde cualquier dirección.
- **Credenciales de desarrollo en `docker-compose.yml`.** El servicio `backend` tiene escritos directamente `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `JWT_SECRET`. Son valores de desarrollo y deben coincidir con los del `.env`.

### Para un entorno real

- Volver a poner el filtro JWT en la cadena de seguridad y exigir token en los endpoints de escritura.
- Restringir CORS y `setAllowedOrigins` a la dirección real del frontend.
- Mover la contraseña de la base y `JWT_SECRET` al `.env` o a un gestor de secretos, y cambiar los valores de ejemplo (`poolhawk_password`).
- Declarar `server.error.include-stacktrace: never` de forma explícita en `application.yml`.

### Limitaciones conocidas

- Para el desarrollo local, `application.yml` conserva las credenciales de la base de datos de desarrollo.

## Acceso (demostración)

La pantalla de inicio de sesión usa el usuario de demostración definido en el backend:

- Usuario: `arbitro`
- Contraseña: `admin123`

Solo para la demo: el backend deja abiertos todos los endpoints.
