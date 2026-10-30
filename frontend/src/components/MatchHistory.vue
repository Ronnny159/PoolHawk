<script setup>
import { ref, onMounted, computed } from 'vue'
import { listarPartidas, obtenerPartida } from '../api/matches'
import { toast } from '../utils/toast'

const emit = defineEmits(['partidaSeleccionada'])
const partidas = ref([])
const cargando = ref(false)
const busqueda = ref('') // Variable para el buscador

const cargarHistorial = async () => {
  try {
    cargando.value = true
    partidas.value = await listarPartidas()
  } catch (error) {
    console.error('Error al cargar historial', error)
    toast.error('No se pudo cargar el historial de partidas')
  } finally {
    cargando.value = false
  }
}

const seleccionar = async (id) => {
  try {
    const partida = await obtenerPartida(id)
    emit('partidaSeleccionada', partida)
  } catch (error) {
    toast.error('No se pudo cargar la partida')
  }
}

// 🪄 LA MAGIA: Filtramos las partidas en tiempo real según lo que escriba el usuario
const partidasFiltradas = computed(() => {
  if (!busqueda.value) return partidas.value
  
  const termino = busqueda.value.toLowerCase()
  return partidas.value.filter(p => {
    // Buscamos si el término está en el nombre de algún jugador del Equipo A o B
    const nombresA = p.equipoA.map(j => j.nombre.toLowerCase()).join(' ')
    const nombresB = p.equipoB.map(j => j.nombre.toLowerCase()).join(' ')
    return nombresA.includes(termino) || nombresB.includes(termino)
  })
})

// Genera un código corto y bonito para mostrar en lugar del UUID feo
const codigoCorto = (id) => id.substring(0, 6).toUpperCase()

onMounted(cargarHistorial)
</script>

<template>
  <div class="historial-container">
    <h2>Historial de Partidas</h2>
    
    <div class="acciones">
      <input 
        v-model="busqueda" 
        type="text" 
        placeholder="🔍 Buscar por nombre de jugador..." 
        class="buscador"
      />
      <button @click="cargarHistorial" :disabled="cargando">
        {{ cargando ? '...' : '🔄' }}
      </button>
    </div>

    <ul class="lista-partidas">
      <li v-for="p in partidasFiltradas" :key="p.id" @click="seleccionar(p.id)">
        <div class="info">
          <strong>{{ p.modalidad }}</strong> - {{ p.equipoA[0]?.nombre || 'Equipo A' }} vs {{ p.equipoB[0]?.nombre || 'Equipo B' }}
          <br>
          <small>
            <!-- Mostramos un código amigable en vez del UUID completo -->
            Código: <strong>#{{ codigoCorto(p.id) }}</strong> | 
            {{ p.fase }} | {{ new Date(p.fechaCreacion).toLocaleString() }}
          </small>
        </div>
        <button class="btn-cargar">Cargar ➔</button>
      </li>
    </ul>
    
    <p v-if="partidasFiltradas.length === 0 && !cargando" class="vacio">
      No se encontraron partidas con ese jugador.
    </p>
  </div>
</template>

<style scoped>
.historial-container { max-width: 800px; margin: 2rem auto; color: white; background: #1e293b; padding: 2rem; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.acciones { display: flex; gap: 1rem; margin-bottom: 1.5rem; }
.buscador { flex: 1; padding: 0.8rem; border-radius: 6px; border: 1px solid #334155; background: #0f172a; color: white; font-size: 1rem; outline: none; }
.buscador:focus { border-color: #22c55e; }
.acciones button { background: #334155; color: white; border: none; padding: 0 1rem; border-radius: 6px; cursor: pointer; font-size: 1.2rem; }
.lista-partidas { list-style: none; padding: 0; margin: 0; }
.lista-partidas li { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border-bottom: 1px solid #334155; cursor: pointer; transition: background 0.2s; }
.lista-partidas li:hover { background: #0f172a; }
.info { flex: 1; }
.info strong { color: #22c55e; }
.info small { color: #94a3b8; }
.btn-cargar { background: #22c55e; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-cargar:hover { background: #16a34a; }
.vacio { text-align: center; color: #94a3b8; margin-top: 2rem; }
</style>