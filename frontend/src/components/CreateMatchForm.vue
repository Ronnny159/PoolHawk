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
  <div class="mesa-wrapper">
    <div class="form-container">

      <!-- CABECERA -->
      <div class="header-form">
        <h2>NUEVA PARTIDA</h2>
        <p class="subtitulo-form">Configura tu mesa y prepara el taco</p>

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

      <!-- BOLAS / EQUIPOS -->
      <div class="equipos-container">
        <!-- EQUIPO A (Bola 1 - Amarilla) -->
        <div class="bola-wrapper">
          <div class="bola-equipo equipo-a">
            <div class="brillo" aria-hidden="true"></div>
            <div class="centro-blanco">
              <label>Equipo A</label>
              <input v-model="jugadorA1" placeholder="Jugador 1" maxlength="12" />
              <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorA2" placeholder="Jugador 2" maxlength="12" />
            </div>
          </div>
        </div>

        <!-- VS -->
        <div class="vs-badge" aria-hidden="true">VS</div>

        <!-- EQUIPO B (Bola 2 - Azul) -->
        <div class="bola-wrapper">
          <div class="bola-equipo equipo-b">
            <div class="brillo" aria-hidden="true"></div>
            <div class="centro-blanco">
              <label>Equipo B</label>
              <input v-model="jugadorB1" placeholder="Jugador 1" maxlength="12" />
              <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorB2" placeholder="Jugador 2" maxlength="12" />
            </div>
          </div>
        </div>
      </div>

      <!-- ERROR (feedback con borde izquierdo rojo) -->
      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <!-- CTA -->
      <button class="btn-iniciar" @click="crear" :disabled="cargando">
        {{ cargando ? 'CREANDO...' : 'INICIAR PARTIDA' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   MARCO EXTERIOR (madera oscura tipo borde de mesa)
   ============================================================ */
.mesa-wrapper {
  max-width: 920px;
  margin: 0 auto;
  padding: 10px;
  border-radius: 26px;
  background: linear-gradient(145deg, #0a0f1a 0%, #1a2332 50%, #0a0f1a 100%);
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.85),
    0 10px 20px rgba(0, 0, 0, 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* ============================================================
   PAÑO DE LA MESA (contenedor del formulario)
   ============================================================ */
.form-container {
  position: relative;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(34, 197, 94, 0.14) 0%, transparent 55%),
    linear-gradient(160deg, #1e293b 0%, #0f172a 100%);
  padding: 3rem 2.5rem 2.5rem;
  border-radius: 18px;
  color: var(--text-main);
  border: 1px solid #334155;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    inset 0 -40px 80px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}

/* Textura de paño muy sutil */
.form-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    repeating-linear-gradient(45deg, transparent 0, transparent 2px, rgba(34, 197, 94, 0.02) 2px, rgba(34, 197, 94, 0.02) 4px);
  pointer-events: none;
  border-radius: inherit;
}

/* ============================================================
   CABECERA
   ============================================================ */
.header-form {
  text-align: center;
  margin-bottom: 2.5rem;
  position: relative;
  z-index: 1;
}

.header-form h2 {
  font-size: 3rem;
  margin: 0;
  color: var(--text-main);
  text-shadow:
    0 2px 0 #000,
    0 4px 12px rgba(0, 0, 0, 0.7);
  letter-spacing: 3px;
}

.subtitulo-form {
  margin: 0.4rem 0 0;
  color: var(--text-muted);
  font-size: 0.95rem;
  font-family: 'Inter', sans-serif;
}

/* ============================================================
   SELECTOR DE MODALIDAD (slot con relieve hundido)
   ============================================================ */
.selector-modalidad {
  display: inline-flex;
  background: #0a0f1a;
  padding: 6px;
  border-radius: 30px;
  margin-top: 1.2rem;
  border: 1px solid #1e293b;
  box-shadow:
    inset 0 3px 6px rgba(0, 0, 0, 0.6),
    0 1px 0 rgba(255, 255, 255, 0.04);
}

.selector-modalidad button {
  background: transparent;
  color: var(--text-muted);
  border: none;
  padding: 10px 28px;
  border-radius: 25px;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.6px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.selector-modalidad button:hover:not(.active) {
  color: var(--text-main);
}

.selector-modalidad button.active {
  background: linear-gradient(180deg, #22c55e 0%, #15803d 100%);
  color: #ffffff;
  box-shadow:
    0 3px 10px rgba(21, 128, 61, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

/* ============================================================
   ZONA DE EQUIPOS
   ============================================================ */
.equipos-container {
  display: flex;
  gap: 1rem;
  justify-content: center;
  align-items: center;
  margin-bottom: 2.5rem;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.vs-badge {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 2rem;
  color: #475569;
  letter-spacing: 2px;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.7);
  user-select: none;
}

/* ============================================================
   LA BOLA DE BILLAR GIGANTE 3D
   ============================================================ */
.bola-wrapper {
  position: relative;
}

.bola-equipo {
  width: 280px;
  height: 280px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  transition:
    transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.45s ease;
  box-shadow:
    inset -22px -22px 55px rgba(0, 0, 0, 0.7),
    inset 18px 18px 35px rgba(255, 255, 255, 0.35),
    0 22px 40px rgba(0, 0, 0, 0.7),
    0 8px 15px rgba(0, 0, 0, 0.5);
}

.bola-equipo:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow:
    inset -22px -22px 55px rgba(0, 0, 0, 0.7),
    inset 18px 18px 35px rgba(255, 255, 255, 0.35),
    0 35px 55px rgba(0, 0, 0, 0.8),
    0 14px 22px rgba(0, 0, 0, 0.6);
}

/* Brillo especular (reflejo de luz cenital) */
.brillo {
  position: absolute;
  top: 12%;
  left: 18%;
  width: 92px;
  height: 68px;
  background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.75) 0%, rgba(255, 255, 255, 0) 70%);
  border-radius: 50%;
  filter: blur(5px);
  pointer-events: none;
  z-index: 6;
}

/* Gradiente esférico Equipo A (Bola 1 - Amarilla) */
.equipo-a {
  background: radial-gradient(
    circle at 30% 25%,
    #fef9c3 0%,
    var(--ball-yellow) 42%,
    #ca8a04 78%,
    #713f12 100%
  );
}

/* Gradiente esférico Equipo B (Bola 2 - Azul) */
.equipo-b {
  background: radial-gradient(
    circle at 30% 25%,
    #dbeafe 0%,
    var(--ball-blue) 42%,
    #1d4ed8 78%,
    #1e3a8a 100%
  );
}

/* ============================================================
   CÍRCULO BLANCO CENTRAL (parche numerado)
   ============================================================ */
.centro-blanco {
  width: 168px;
  height: 168px;
  background: radial-gradient(circle at 38% 30%, #ffffff 0%, #f1f5f9 65%, #dbe3ec 100%);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 0.75rem;
  position: relative;
  z-index: 10;
  box-shadow:
    inset 3px 3px 10px rgba(0, 0, 0, 0.18),
    inset -2px -2px 6px rgba(255, 255, 255, 0.9),
    0 3px 8px rgba(0, 0, 0, 0.35);
}

.centro-blanco label {
  color: #0f172a;
  font-family: 'Bebas Neue', sans-serif;
  font-weight: 400;
  font-size: 1.05rem;
  letter-spacing: 1.5px;
  margin-bottom: 0.45rem;
}

.centro-blanco input {
  width: 128px;
  padding: 0.42rem 0.5rem;
  margin-bottom: 0.35rem;
  border: 2px solid #cbd5e1;
  border-radius: 7px;
  text-align: center;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 0.9rem;
  color: #0f172a;
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.06);
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
  border-color: var(--felt-light);
  box-shadow:
    0 0 0 3px rgba(34, 197, 94, 0.22),
    inset 0 1px 2px rgba(0, 0, 0, 0.06);
}

/* ============================================================
   BOTÓN INICIAR (efecto físico 3D)
   ============================================================ */
.btn-iniciar {
  display: block;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 1.15rem 1rem 1.05rem;
  background: linear-gradient(180deg, #22c55e 0%, #15803d 100%);
  color: #ffffff;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.55rem;
  letter-spacing: 3px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.35);
  box-shadow:
    0 6px 0 #0f5132,
    0 10px 22px rgba(0, 0, 0, 0.55),
    0 4px 8px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);
  transition: transform 0.12s ease, box-shadow 0.12s ease, background 0.25s ease;
}

.btn-iniciar:hover:not(:disabled) {
  background: linear-gradient(180deg, #4ade80 0%, #22c55e 100%);
  transform: translateY(-1px);
  box-shadow:
    0 7px 0 #0f5132,
    0 14px 26px rgba(0, 0, 0, 0.55),
    0 5px 10px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

.btn-iniciar:active:not(:disabled) {
  transform: translateY(4px);
  box-shadow:
    0 2px 0 #0f5132,
    0 4px 10px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.btn-iniciar:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  filter: grayscale(0.25);
}

/* ============================================================
   FEEDBACK DE ERROR (borde izquierdo rojo)
   ============================================================ */
.error {
  color: #fecaca;
  background: rgba(239, 68, 68, 0.12);
  border-left: 4px solid var(--ball-red);
  padding: 0.85rem 1rem;
  border-radius: 6px;
  text-align: left;
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  font-size: 0.92rem;
  margin: 0 auto 1.5rem;
  max-width: 420px;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.15);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 720px) {
  .form-container {
    padding: 2rem 1.25rem 2rem;
  }

  .header-form h2 {
    font-size: 2.4rem;
  }

  .bola-equipo {
    width: 230px;
    height: 230px;
  }

  .centro-blanco {
    width: 138px;
    height: 138px;
  }

  .centro-blanco input {
    width: 108px;
    font-size: 0.85rem;
  }

  .vs-badge {
    font-size: 1.4rem;
  }
}

@media (max-width: 520px) {
  .equipos-container {
    gap: 1.5rem;
  }

  .bola-equipo {
    width: 210px;
    height: 210px;
  }

  .brillo {
    width: 70px;
    height: 50px;
  }
}
</style>