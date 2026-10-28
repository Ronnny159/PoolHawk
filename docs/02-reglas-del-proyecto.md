# Reglas y Decisiones del Proyecto

## 1. Decisiones de Arquitectura (No romper)
- **Domain-Driven Design (DDD):** El módulo `billiard-domain` no tiene dependencias de frameworks (ni Spring, ni JPA). Es Java puro.
- **Patrón Strategy:** `ReglasBillar` es la interfaz. `ReglasBola8` y `ReglasPorPuntos` son las implementaciones. No se deben mezclar sus lógicas.
- **Comunicación Frontend-Backend:** Se usa REST (JSON). El backend corre en el puerto 8080 y el frontend en el 5173 (configurado con CORS).

## 2. Reglas de Negocio del Billar
- **Bola 8:** Gana quien embolse la bola 8 después de limpiar su grupo (lisas 1-7 o rayadas 9-15).
- **Por Puntos:** Se juega en parejas. Cada bola vale 1 punto. Gana el equipo que llegue a 50 puntos.
- **Faltas:** Si no se toca bola, si se embolsa la blanca, o si se toca el grupo equivocado.

## 3. Estilo de Código
- **Idioma:** Código en inglés, Javadoc y mensajes al usuario en español.
- **Frontend:** Usar `<script setup>` de Vue 3. No usar librerías de UI externas (como Vuetify o Tailwind), solo CSS puro.