<script setup>
import { ref, onMounted } from 'vue'
import { listarPartidas, obtenerPartida } from '../api/matches'

const emit = defineEmits(['partidaSeleccionada'])
const partidas = ref([])
const cargando = ref(false)

const cargarHistorial = async () => {
  try {
    cargando.value = true
    partidas.value = await listarPartidas()
  } catch (error) {
    console.error('Error al cargar historial', error)
  } finally {
    cargando.value = false
  }
}

const seleccionar = async (id) => {
  try {
    const partida = await obtenerPartida(id)
    emit('partidaSeleccionada', partida)
  } catch (error) {
    alert('No se pudo cargar la partida')
  }
}

onMounted(cargarHistorial)
</script>

<template>
  <div class="historial-container">
    <h2>Historial de Partidas</h2>
    <button @click="cargarHistorial" :disabled="cargando">
      {{ cargando ? 'Cargando...' : '🔄 Actualizar' }}
    </button>

    <ul class="lista-partidas">
      <li v-for="p in partidas" :key="p.id" @click="seleccionar(p.id)">
        <div class="info">
          <strong>{{ p.modalidad }}</strong> - {{ p.equipoA[0]?.nombre || 'Equipo A' }} vs {{ p.equipoB[0]?.nombre || 'Equipo B' }}
          <br>
          <small>{{ p.fase }} | {{ new Date(p.fechaCreacion).toLocaleString() }}</small>
        </div>
        <button>Cargar ➔</button>
      </li>
    </ul>
    <p v-if="partidas.length === 0 && !cargando">No hay partidas registradas aún.</p>
  </div>
</template>

<style scoped>
.historial-container { max-width: 800px; margin: 2rem auto; color: white; background: #1e293b; padding: 2rem; border-radius: 12px; }
.lista-partidas { list-style: none; padding: 0; }
.lista-partidas li { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border-bottom: 1px solid #334155; cursor: pointer; }
.lista-partidas li:hover { background: #0f172a; }
button { background: #22c55e; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-weight: bold;}
</style>