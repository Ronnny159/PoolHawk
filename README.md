# PoolHawk

PoolHawk — Marcador automático de partidas de billar mediante visión por computadora. Elimina el conteo manual y garantiza la aplicación objetiva de las reglas.





\# PoolHawk



PoolHawk es un marcador automático de partidas de billar mediante visión por

computadora. Elimina el conteo manual y garantiza la aplicación objetiva de las

reglas (Bola 8 y Por Puntos). Los tiros se registran por una API REST, pasan por

el motor de reglas y el marcador se actualiza en vivo en el navegador mediante

WebSockets.



\*\*Stack:\*\* Java 21 + Spring Boot (Maven multi-módulo), Vue 3 (Vite), PostgreSQL y Docker.



\## Estructura del repositorio



```

PoolHawk/

├── backend/          # Maven multi-módulo

│   ├── billiard-domain/   # Modelo de dominio

│   ├── billiard-rules/    # Motor de reglas (Bola 8, Por Puntos)

│   └── billiard-api/      # API REST, persistencia JPA y WebSocket

├── frontend/         # Aplicación Vue 3

├── tracking-agent/   # Simulador de cámara en Python

├── docs/             # Documentación y diagramas

└── docker-compose.yml

```



\## Requisitos previos



\- Java 21

\- Node.js 22 y npm

\- Docker Desktop (con Docker Compose)

\- Python 3.10 o superior (solo para el simulador de tracking)



\## Opción 1: levantar todo con Docker



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



\## Opción 2: modo desarrollo (paso a paso)



En este modo solo la base de datos corre en Docker; el backend y el frontend

corren en tu máquina. Si ya levantaste todo con la opción 1, detén antes esos

contenedores para liberar los puertos 8080 y 5173:



```bash

docker compose stop backend frontend

```



\### 1. Base de datos



```bash

docker compose up -d postgres

```



\### 2. Backend



```powershell

cd backend

.\\mvnw.cmd spring-boot:run

```



En Linux o macOS usa `./mvnw spring-boot:run`. La API queda en http://localhost:8080.



\### 3. Frontend



```bash

cd frontend

npm install

npm run dev

```



La aplicación queda en http://localhost:5173.



\## Simulador de tracking (Python)



El script `tracking-agent/simulador.py` simula las lecturas de la cámara: crea una

partida de Bola 8 y envía 10 tiros a la API, con una pausa de 2 segundos entre cada uno.



```bash

cd tracking-agent

python -m pip install -r requirements.txt

python simulador.py

```



Opciones:



```bash

\# Usar otra URL del backend

python simulador.py --base-url http://localhost:8080



\# Disparar tiros sobre una partida que ya existe (por ejemplo, la abierta en el navegador)

python simulador.py --match-id <id-de-la-partida>

```



Para ver el marcador en vivo, abre el frontend en una ventana y ejecuta el

simulador en otra: el marcador se actualiza solo gracias al WebSocket.



\## Pruebas



```powershell

cd backend

.\\mvnw.cmd test

```



