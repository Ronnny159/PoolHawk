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
  <div class="card">
    <h2>Nueva Partida</h2>
    
    <div class="form-group">
      <label>Modalidad</label>
      <select v-model="modalidad">
        <option value="BOLA_8">Bola 8 (Individual)</option>
        <option value="POR_PUNTOS">Por Puntos (Parejas)</option>
      </select>
    </div>

    <div class="equipos">
      <div class="equipo">
        <h3>Equipo A</h3>
        <input v-model="jugadorA1" placeholder="Jugador A1" />
        <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorA2" placeholder="Jugador A2" />
      </div>
      
      <div class="equipo">
        <h3>Equipo B</h3>
        <input v-model="jugadorB1" placeholder="Jugador B1" />
        <input v-if="modalidad === 'POR_PUNTOS'" v-model="jugadorB2" placeholder="Jugador B2" />
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    
    <button @click="crear" :disabled="cargando">
      {{ cargando ? 'Creando...' : 'Iniciar Partida' }}
    </button>
  </div>
</template>

<style scoped>
.card { background: #1e1e1e; padding: 2rem; border-radius: 12px; color: white; max-width: 600px; margin: 0 auto; }
h2 { text-align: center; margin-bottom: 2rem; }
.form-group { margin-bottom: 1.5rem; }
label { display: block; margin-bottom: 0.5rem; color: #aaa; }
select, input { width: 100%; padding: 0.8rem; border-radius: 8px; border: 1px solid #333; background: #2a2a2a; color: white; margin-bottom: 0.5rem; }
.equipos { display: flex; gap: 2rem; margin-bottom: 2rem; }
.equipo { flex: 1; }
h3 { font-size: 1rem; color: #4ade80; margin-bottom: 1rem; }
button { width: 100%; padding: 1rem; background: #4ade80; color: #000; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; }
button:disabled { background: #555; cursor: not-allowed; }
.error { color: #ff4444; text-align: center; margin-bottom: 1rem; }
</style>