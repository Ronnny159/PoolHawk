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
      <h2>NUEVA PARTIDA</h2>
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
      <!-- EQUIPO A (Bola Amarilla) -->
      <div class="bola-equipo equipo-a">
        <div class="centro-blanco">
          <label>Equipo A</label>
          <input v-model="jugadorA1" placeholder="Jugador 1" maxlength="12" />
          <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorA2" placeholder="Jugador 2" maxlength="12" />
        </div>
      </div>
      
      <!-- EQUIPO B (Bola Azul) -->
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
      {{ cargando ? 'CREANDO...' : 'INICIAR PARTIDA' }}
    </button>
  </div>
</template>

<style scoped>
.form-container { 
  background: #1e293b; 
  padding: 3rem; 
  border-radius: 16px; 
  color: white; 
  max-width: 850px; 
  margin: 0 auto; 
  border: 4px solid #0f172a;
  box-shadow: 0 20px 40px rgba(0,0,0,0.6);
}

.header-form { text-align: center; margin-bottom: 3rem; }
h2 { font-size: 2.5rem; margin-top: 0; color: #e2e8f0; text-shadow: 2px 2px 0 #000; }

/* Selector de Modalidad estilo Botones de Casino/Mesa */
.selector-modalidad { 
  display: inline-flex; 
  background: #0f172a; 
  padding: 5px; 
  border-radius: 30px; 
  margin-top: 1rem;
  border: 1px solid #334155;
}
.selector-modalidad button {
  background: transparent;
  color: #94a3b8;
  border: none;
  padding: 10px 25px;
  border-radius: 25px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}
.selector-modalidad button.active {
  background: #334155;
  color: white;
  box-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.equipos-container { 
  display: flex; 
  gap: 2rem; 
  justify-content: center; 
  margin-bottom: 3rem;
  flex-wrap: wrap;
}

/* EL DISEÑO DE LA BOLA DE BILLAR GIGANTE */
.bola-equipo {
  width: 280px;
  height: 280px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  box-shadow: 
    inset -20px -20px 40px rgba(0,0,0,0.6),
    inset 15px 15px 30px rgba(255,255,255,0.4),
    0 15px 30px rgba(0,0,0,0.5);
  transition: transform 0.3s;
}

.bola-equipo:hover { transform: translateY(-5px); }

/* El círculo blanco central donde van los inputs */
.centro-blanco {
  width: 160px;
  height: 160px;
  background: #f8fafc;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 1rem;
  box-shadow: inset 2px 2px 10px rgba(0,0,0,0.2);
  z-index: 10;
}

.centro-blanco label {
  color: #0f172a;
  font-weight: 800;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.centro-blanco input {
  width: 120px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  border: 2px solid #cbd5e1;
  border-radius: 8px;
  text-align: center;
  font-weight: bold;
  font-size: 1rem;
  color: #0f172a;
  background: white;
  transition: border-color 0.2s;
}

.centro-blanco input:focus {
  outline: none;
  border-color: var(--felt-green);
}

/* Colores de las bolas */
.equipo-a { background: radial-gradient(circle at 30% 30%, #fef08a, var(--ball-yellow) 60%, #a16207 100%); }
.equipo-b { background: radial-gradient(circle at 30% 30%, #bfdbfe, var(--ball-blue) 60%, #1e3a8a 100%); }

/* Botón de Iniciar */
.btn-iniciar {
  display: block;
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  padding: 1.2rem;
  background: var(--felt-green);
  color: white;
  font-size: 1.5rem;
  font-family: 'Bebas Neue', sans-serif;
  letter-spacing: 2px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(21, 128, 61, 0.4);
  transition: all 0.3s;
}

.btn-iniciar:hover:not(:disabled) {
  background: var(--felt-light);
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(21, 128, 61, 0.6);
}

.btn-iniciar:disabled { opacity: 0.5; cursor: not-allowed; }

.error { color: var(--ball-red); text-align: center; font-weight: bold; margin-bottom: 1.5rem; }
</style>