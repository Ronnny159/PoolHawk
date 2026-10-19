# PoolHawk - Documento de Contexto de Situación

## 1. ¿Qué es el proyecto?
PoolHawk es un sistema web Full-Stack para el rastreo y marcado automático de partidas de billar en dos modalidades: Bola 8 (individual) y Por Puntos (parejas).

## 2. Stack Tecnológico Actual
- **Backend:** Java 21, Spring Boot 3.3.5, Maven (Multi-módulo).
- **Frontend:** Vue 3 (Composition API), Vite, Axios.
- **Arquitectura Backend:** 3 capas (Dominio puro, Reglas de negocio, API REST).
- **Base de Datos:** Pendiente de implementar (Actualmente en memoria con ConcurrentHashMap).
- **Testing:** JUnit 5 (541 tests verdes en el backend).

## 3. Estado del Proyecto (MVP)
- El motor de reglas (Bola 8 y Por Puntos) está 100% funcional.
- La API REST expone endpoints para crear partidas, consultar estado y registrar tiros.
- El frontend permite crear partidas, simular tiros (Faltas, Tiros legales, Embolsar) y muestra un marcador en vivo y un botón de revancha.

## 4. Objetivo Actual
- Rediseñar la capa visual (UI/UX) del frontend para darle una estética premium inspirada en mesas de billar profesionales (paño verde, bolas 3D, iluminación).