<script setup>
import { ref } from 'vue'
import { crearPartida } from '../api/matches'

const emit = defineEmits(['partidaIniciada'])

const modalidad = ref('BOLA_8')
const jugadorA1 = ref('')
const jugadorB1 = ref('')
const jugadorA2 = ref('')
const jugadorB2 = ref('')
const cargando = ref(false)
const error = ref('')

const crear = async () => {
  try {
    cargando.value = true
    error.value = ''

    const equipoA = modalidad.value === 'BOLA_8' ? [jugadorA1.value] : [jugadorA1.value, jugadorA2.value]
    const equipoB = modalidad.value === 'BOLA_8' ? [jugadorB1.value] : [jugadorB1.value, jugadorB2.value]

    if (equipoA.some(n => !n.trim()) || equipoB.some(n => !n.trim())) {
      throw new Error('Todos los nombres son obligatorios')
    }

    const partida = await crearPartida({
      modalidad: modalidad.value,
      equipoA,
      equipoB
    })

    emit('partidaIniciada', partida)
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="form-container">
    <div class="header-form">
      <h2>Nueva Partida</h2>
      <div class="selector-modalidad">
        <button
          :class="{ active: modalidad === 'BOLA_8' }"
          @click="modalidad = 'BOLA_8'">
          Bola 8
        </button>
        <button
          :class="{ active: modalidad === 'POR_PUNTOS' }"
          @click="modalidad = 'POR_PUNTOS'">
          Por Puntos
        </button>
      </div>
    </div>

    <div class="equipos-container">
      <!-- EQUIPO A (Bola 1 - Amarilla) -->
      <div class="bola-equipo equipo-a">
        <div class="centro-blanco">
          <label>Equipo A</label>
          <input v-model="jugadorA1" placeholder="Jugador 1" maxlength="12" />
          <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorA2" placeholder="Jugador 2" maxlength="12" />
        </div>
      </div>

      <!-- EQUIPO B (Bola 2 - Azul) -->
      <div class="bola-equipo equipo-b">
        <div class="centro-blanco">
          <label>Equipo B</label>
          <input v-model="jugadorB1" placeholder="Jugador 1" maxlength="12" />
          <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorB2" placeholder="Jugador 2" maxlength="12" />
        </div>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <button class="btn-iniciar" @click="crear" :disabled="cargando">
      {{ cargando ? 'Creando...' : 'Iniciar Partida' }}
    </button>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Bebas+Neue&family=Inter:wght@400;500;600;700;800&display=swap');

/* ============================================================
   CONTENEDOR PRINCIPAL — Mesa Premium
   ============================================================ */
.form-container {
  position: relative;
  max-width: 900px;
  margin: 0 auto;
  padding: 3rem 2rem 2.5rem;
  border-radius: 22px;
  color: #f8fafc;
  font-family: 'Inter', 'Segoe UI', sans-serif;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(34, 197, 94, 0.12) 0%, transparent 55%),
    radial-gradient(ellipse at 50% 100%, rgba(15, 23, 42, 0.9) 0%, transparent 60%),
    linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  border: 3px solid #0f172a;
  box-shadow:
    0 25px 60px rgba(0, 0, 0, 0.65),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

/* Textura sutil de paño */
.form-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(34, 197, 94, 0.05) 1px, transparent 1px);
  background-size: 4px 4px;
  pointer-events: none;
  z-index: 0;
}

.form-container > * {
  position: relative;
  z-index: 1;
}

/* ============================================================
   HEADER
   ============================================================ */
.header-form {
  text-align: center;
  margin-bottom: 3rem;
}

h2 {
  margin: 0;
  font-family: 'Bebas Neue', 'Inter', sans-serif;
  font-size: clamp(2.2rem, 5vw, 3rem);
  letter-spacing: 4px;
  color: #f8fafc;
  text-transform: uppercase;
  text-shadow:
    0 2px 0 #000,
    0 4px 12px rgba(34, 197, 94, 0.25);
}

/* ============================================================
   SELECTOR DE MODALIDAD — Botones físicos tipo toggle
   ============================================================ */
.selector-modalidad {
  display: inline-flex;
  margin-top: 1.4rem;
  padding: 5px;
  gap: 4px;
  background: #0f172a;
  border-radius: 30px;
  border: 1px solid #334155;
  box-shadow:
    inset 0 2px 6px rgba(0, 0, 0, 0.6),
    0 2px 4px rgba(0, 0, 0, 0.3);
}

.selector-modalidad button {
  padding: 10px 26px;
  background: transparent;
  color: #94a3b8;
  border: none;
  border-radius: 25px;
  font-family: 'Inter', 'Segoe UI', sans-serif;
  font-weight: 700;
  font-size: 0.8rem;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.25s ease;
}

.selector-modalidad button:hover:not(.active) {
  color: #e2e8f0;
  background: rgba(51, 65, 85, 0.5);
}

.selector-modalidad button.active {
  background: linear-gradient(180deg, #22c55e 0%, #16a34a 50%, #15803d 100%);
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
  box-shadow:
    0 2px 0 #14532d,
    0 4px 12px rgba(34, 197, 94, 0.45);
}

/* ============================================================
   CONTENEDOR DE EQUIPOS
   ============================================================ */
.equipos-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: clamp(2rem, 6vw, 5rem);
  margin-bottom: 3rem;
  flex-wrap: wrap;
}

/* ============================================================
   BOLAS DE BILLAR 3D
   ============================================================ */
.bola-equipo {
  --ball-size: clamp(240px, 44vw, 320px);
  position: relative;
  width: var(--ball-size);
  height: var(--ball-size);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  will-change: transform;
}

.bola-equipo:hover {
  transform: translateY(-8px) scale(1.02);
}

/* Destello especular (reflejo de luz superior-izquierda) */
.bola-equipo::before {
  content: '';
  position: absolute;
  top: 10%;
  left: 16%;
  width: 24%;
  height: 15%;
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.45) 35%,
    transparent 75%
  );
  border-radius: 50%;
  filter: blur(2px);
  transform: rotate(-30deg);
  z-index: 2;
  pointer-events: none;
}

/* Luz de rebote inferior (reflejo del paño verde) */
.bola-equipo::after {
  content: '';
  position: absolute;
  bottom: 7%;
  right: 20%;
  width: 22%;
  height: 9%;
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.35) 0%,
    transparent 70%
  );
  border-radius: 50%;
  filter: blur(5px);
  z-index: 2;
  pointer-events: none;
}

/* Bola 1 — Amarilla (Equipo A) */
.equipo-a {
  background: radial-gradient(
    circle at 32% 28%,
    #fefce8 0%,
    #fde047 10%,
    #facc15 35%,
    #eab308 55%,
    #a16207 82%,
    #713f12 100%
  );
  box-shadow:
    inset -25px -30px 60px rgba(0, 0, 0, 0.55),
    inset 12px 14px 35px rgba(255, 255, 255, 0.18),
    0 25px 50px rgba(0, 0, 0, 0.65),
    0 10px 20px rgba(0, 0, 0, 0.45),
    0 0 70px rgba(250, 204, 21, 0.18);
}

/* Bola 2 — Azul (Equipo B) */
.equipo-b {
  background: radial-gradient(
    circle at 32% 28%,
    #f0f9ff 0%,
    #93c5fd 10%,
    #3b82f6 35%,
    #2563eb 55%,
    #1d4ed8 82%,
    #1e3a8a 100%
  );
  box-shadow:
    inset -25px -30px 60px rgba(0, 0, 0, 0.55),
    inset 12px 14px 35px rgba(255, 255, 255, 0.18),
    0 25px 50px rgba(0, 0, 0, 0.65),
    0 10px 20px rgba(0, 0, 0, 0.45),
    0 0 70px rgba(59, 130, 246, 0.18);
}

/* ============================================================
   CÍRCULO BLANCO CENTRAL — Como el número de la bola
   ============================================================ */
.centro-blanco {
  position: relative;
  width: 62%;
  height: 62%;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 8%;
  box-sizing: border-box;
  background: radial-gradient(
    circle at 45% 38%,
    #ffffff 0%,
    #f8fafc 45%,
    #e2e8f0 80%,
    #cbd5e1 100%
  );
  box-shadow:
    inset 0 3px 8px rgba(0, 0, 0, 0.15),
    inset 0 -2px 4px rgba(255, 255, 255, 0.7),
    0 3px 10px rgba(0, 0, 0, 0.3),
    0 0 0 2px rgba(255, 255, 255, 0.35);
  z-index: 10;
}

.centro-blanco label {
  margin-bottom: 0.4rem;
  color: #0f172a;
  font-weight: 800;
  font-size: 0.72rem;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.6);
}

.centro-blanco input {
  width: 100%;
  max-width: 130px;
  margin-bottom: 0.4rem;
  padding: 0.42rem 0.5rem;
  border: 2px solid #cbd5e1;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #f1f5f9 100%);
  color: #0f172a;
  font-family: 'Inter', 'Segoe UI', sans-serif;
  font-weight: 700;
  font-size: 0.85rem;
  text-align: center;
  box-sizing: border-box;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

.centro-blanco input:last-child {
  margin-bottom: 0;
}

.centro-blanco input::placeholder {
  color: #94a3b8;
  font-weight: 500;
}

.centro-blanco input:focus {
  outline: none;
  border-color: #22c55e;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.05),
    0 0 0 3px rgba(34, 197, 94, 0.25);
}

/* ============================================================
   MENSAJE DE ERROR — Borde izquierdo rojo (Design System)
   ============================================================ */
.error {
  max-width: 520px;
  margin: 0 auto 1.5rem;
  padding: 0.85rem 1.2rem;
  background: rgba(239, 68, 68, 0.1);
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  color: #fca5a5;
  font-weight: 600;
  font-size: 0.92rem;
  text-align: left;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

/* ============================================================
   BOTÓN PRINCIPAL — Efecto físico 3D
   ============================================================ */
.btn-iniciar {
  display: block;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 1.1rem 2rem;
  border: none;
  border-radius: 14px;
  background: linear-gradient(180deg, #22c55e 0%, #16a34a 50%, #15803d 100%);
  color: #ffffff;
  font-family: 'Bebas Neue', 'Inter', sans-serif;
  font-size: 1.7rem;
  letter-spacing: 4px;
  text-transform: uppercase;
  text-shadow: 0 2px 3px rgba(0, 0, 0, 0.35);
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow:
    0 6px 0 #14532d,
    0 10px 22px rgba(0, 0, 0, 0.45),
    0 0 35px rgba(34, 197, 94, 0.3);
}

.btn-iniciar:hover:not(:disabled) {
  background: linear-gradient(180deg, #4ade80 0%, #22c55e 50%, #16a34a 100%);
  transform: translateY(-2px);
  box-shadow:
    0 8px 0 #14532d,
    0 14px 28px rgba(0, 0, 0, 0.5),
    0 0 45px rgba(34, 197, 94, 0.55);
}

.btn-iniciar:active:not(:disabled) {
  transform: translateY(4px);
  box-shadow:
    0 2px 0 #14532d,
    0 4px 10px rgba(0, 0, 0, 0.45),
    0 0 20px rgba(34, 197, 94, 0.3);
}

.btn-iniciar:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 560px) {
  .form-container {
    padding: 2rem 1rem 1.5rem;
  }

  .bola-equipo {
    --ball-size: 220px;
  }

  .equipos-container {
    gap: 1.5rem;
  }

  .selector-modalidad button {
    padding: 9px 18px;
    font-size: 0.72rem;
  }

  .centro-blanco label {
    font-size: 0.65rem;
    margin-bottom: 0.3rem;
  }

  .centro-blanco input {
    font-size: 0.78rem;
    padding: 0.35rem 0.4rem;
    margin-bottom: 0.3rem;
  }
}
</style>