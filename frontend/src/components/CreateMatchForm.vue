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
  <div class="table-frame">
    <!-- TRONERAS (Esquinas y laterales) -->
    <div class="pocket top-left"></div>
    <div class="pocket top-mid"></div>
    <div class="pocket top-right"></div>
    <div class="pocket bottom-left"></div>
    <div class="pocket bottom-mid"></div>
    <div class="pocket bottom-right"></div>

    <!-- PAÑO VERDE DE LA MESA -->
    <div class="felt-container">
      
      <!-- CABECERA -->
      <div class="header-form">
        <h2>NUEVA PARTIDA</h2>
        
        <!-- SELECTOR DE MODALIDAD (Tipo pastilla) -->
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

      <!-- EQUIPOS / BOLAS -->
      <div class="equipos-container">
        
        <!-- EQUIPO A (Bola Amarilla) -->
        <div class="bola-equipo equipo-a">
          <div class="brillo" aria-hidden="true"></div>
          <div class="circulo-blanco">
            <span class="team-label">Equipo A</span>
            <input v-model="jugadorA1" placeholder="Jugador 1" maxlength="12" />
            <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorA2" placeholder="Jugador 2" maxlength="12" />
          </div>
        </div>

        <!-- VS -->
        <div class="vs-badge" aria-hidden="true">VS</div>

        <!-- EQUIPO B (Bola Azul) -->
        <div class="bola-equipo equipo-b">
          <div class="brillo" aria-hidden="true"></div>
          <div class="circulo-blanco">
            <span class="team-label">Equipo B</span>
            <input v-model="jugadorB1" placeholder="Jugador 1" maxlength="12" />
            <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorB2" placeholder="Jugador 2" maxlength="12" />
          </div>
        </div>
      </div>

      <!-- ERROR -->
      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <!-- BOTÓN INICIAR -->
      <button class="btn-iniciar" @click="crear" :disabled="cargando">
        {{ cargando ? 'CREANDO...' : 'INICIAR PARTIDA' }}
      </button>
      
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   1. MARCO DE MADERA Y PAÑO (MESA DE BILLAR)
   ============================================================ */
.table-frame {
  max-width: 950px;
  margin: 0 auto;
  position: relative;
  padding: 25px; /* Grosor de la madera */
  border-radius: 12px;
  /* Madera oscura con vetas y sombras */
  background: linear-gradient(135deg, #2a1106 0%, #4a220d 50%, #2a1106 100%);
  box-shadow: 
    0 30px 60px rgba(0, 0, 0, 0.9),
    inset 0 0 20px rgba(0, 0, 0, 0.8);
  border: 2px solid #1a0a02;
}

.felt-container {
  position: relative;
  background: 
    radial-gradient(circle at center, #106932 0%, #06421a 80%, #022d0d 100%);
  border-radius: 8px;
  padding: 3rem 2rem 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
  box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.7);
  border: 1px solid #022d0d;
}

/* Polvo de tiza sutil (Textura) */
.felt-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 20px 20px, 30px 30px;
  background-position: 0 0, 10px 15px;
  pointer-events: none;
}

/* ============================================================
   2. TRONERAS (Agujeros negros)
   ============================================================ */
.pocket {
  position: absolute;
  width: 42px;
  height: 42px;
  background: radial-gradient(circle, #000 50%, #0a0a0a 100%);
  border-radius: 50%;
  box-shadow: inset 0 8px 10px rgba(0,0,0,1), 0 2px 5px rgba(0,0,0,0.8);
  z-index: 10;
  border: 2px solid rgba(0,0,0,0.8);
}
/* Esquinas */
.pocket.top-left { top: 8px; left: 8px; }
.pocket.top-right { top: 8px; right: 8px; }
.pocket.bottom-left { bottom: 8px; left: 8px; }
.pocket.bottom-right { bottom: 8px; right: 8px; }
/* Laterales (Ajustados a la madera) */
.pocket.top-mid { top: -18px; left: 50%; transform: translateX(-50%); width: 48px; height: 36px; border-radius: 50%; }
.pocket.bottom-mid { bottom: -18px; left: 50%; transform: translateX(-50%); width: 48px; height: 36px; border-radius: 50%; }

/* ============================================================
   3. CABECERA Y SELECTOR
   ============================================================ */
.header-form {
  text-align: center;
  z-index: 2;
  margin-bottom: 2rem;
}

.header-form h2 {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 3.5rem;
  margin: 0 0 0.5rem 0;
  color: #ffffff;
  letter-spacing: 3px;
  text-shadow: 0 4px 0 rgba(0,0,0,0.5);
}

.selector-modalidad {
  display: inline-flex;
  background: #032310;
  padding: 4px;
  border-radius: 30px;
  border: 1px solid #011709;
  box-shadow: inset 0 3px 6px rgba(0,0,0,0.8);
}

.selector-modalidad button {
  background: transparent;
  color: #a0b8a8;
  border: none;
  padding: 8px 24px;
  border-radius: 20px;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selector-modalidad button.active {
  background: #ffffff;
  color: #000000;
  box-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

/* ============================================================
   4. BOLAS Y EQUIPOS
   ============================================================ */
.equipos-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 2rem;
  z-index: 2;
  flex-wrap: wrap;
}

/* La bola base */
.bola-equipo {
  width: 260px;
  height: 260px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  box-shadow: 
    inset -15px -15px 35px rgba(0, 0, 0, 0.7),
    inset 15px 15px 25px rgba(255, 255, 255, 0.4),
    0 25px 35px rgba(0, 0, 0, 0.6);
  transition: transform 0.3s ease;
}

.bola-equipo:hover {
  transform: translateY(-5px) scale(1.02);
}

/* Bola Amarilla */
.equipo-a {
  background: radial-gradient(circle at 30% 25%, #fef08a 0%, #facc15 40%, #ca8a04 80%, #713f12 100%);
}

/* Bola Azul */
.equipo-b {
  background: radial-gradient(circle at 30% 25%, #dbeafe 0%, #3b82f6 40%, #1d4ed8 80%, #1e3a8a 100%);
}

/* Brillo superior */
.brillo {
  position: absolute;
  top: 10%;
  left: 20%;
  width: 80px;
  height: 50px;
  background: radial-gradient(ellipse, rgba(255,255,255,0.8) 0%, transparent 60%);
  border-radius: 50%;
  pointer-events: none;
  transform: rotate(-15deg);
}

/* Círculo blanco interior */
.circulo-blanco {
  width: 170px;
  height: 170px;
  background: #ffffff;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 10px;
  box-shadow: 
    inset 0 5px 15px rgba(0,0,0,0.15),
    0 5px 10px rgba(0,0,0,0.2);
  gap: 6px;
}

.team-label {
  font-family: 'Inter', sans-serif;
  font-weight: 800;
  font-size: 1.1rem;
  color: #000;
  margin-bottom: 5px;
}

.circulo-blanco input {
  width: 125px;
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  background: #f8fafc;
  text-align: center;
  font-family: 'Inter', sans-serif;
  font-size: 0.85rem;
  color: #64748b;
  outline: none;
  transition: all 0.2s;
}

.circulo-blanco input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.2);
}

.circulo-blanco input::placeholder {
  color: #94a3b8;
}

/* VS Badge */
.vs-badge {
  background: #ffffff;
  color: #000;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.5rem;
  padding: 4px 10px;
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.5);
  transform: translateY(-20px);
}

/* ============================================================
   5. BOTÓN DE INICIO
   ============================================================ */
.btn-iniciar {
  margin-top: 1rem;
  padding: 1rem 3rem;
  background: linear-gradient(180deg, #22c55e 0%, #15803d 100%);
  color: #ffffff;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.8rem;
  letter-spacing: 2px;
  border: 4px solid #053d1b; /* Borde grueso tipo madera */
  border-radius: 8px;
  cursor: pointer;
  z-index: 2;
  box-shadow: 
    0 6px 0 #02240f, /* Sombra dura que simula relieve */
    0 10px 20px rgba(0,0,0,0.6),
    inset 0 2px 0 rgba(255,255,255,0.3);
  transition: all 0.1s;
}

.btn-iniciar:hover:not(:disabled) {
  background: linear-gradient(180deg, #4ade80 0%, #22c55e 100%);
  transform: translateY(-1px);
  box-shadow: 
    0 7px 0 #02240f,
    0 12px 22px rgba(0,0,0,0.6),
    inset 0 2px 0 rgba(255,255,255,0.4);
}

.btn-iniciar:active:not(:disabled) {
  transform: translateY(5px);
  box-shadow: 
    0 1px 0 #02240f,
    0 5px 10px rgba(0,0,0,0.6),
    inset 0 2px 0 rgba(255,255,255,0.2);
}

.btn-iniciar:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

/* ============================================================
   6. ERROR (Borde rojo)
   ============================================================ */
.error {
  color: #fecaca;
  background: rgba(239, 68, 68, 0.15);
  border-left: 4px solid #ef4444;
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 1rem;
  z-index: 2;
}

/* ============================================================
   7. RESPONSIVE
   ============================================================ */
@media (max-width: 768px) {
  .table-frame {
    padding: 15px;
  }
  .felt-container {
    padding: 2rem 1rem;
  }
  .header-form h2 {
    font-size: 2.8rem;
  }
  .bola-equipo {
    width: 220px;
    height: 220px;
  }
  .circulo-blanco {
    width: 140px;
    height: 140px;
  }
  .circulo-blanco input {
    width: 100px;
  }
  .equipos-container {
    gap: 1rem;
  }
}

@media (max-width: 520px) {
  .bola-equipo {
    width: 200px;
    height: 200px;
  }
  .circulo-blanco {
    width: 125px;
    height: 125px;
  }
  .vs-badge {
    transform: translateY(0);
  }
}
</style>