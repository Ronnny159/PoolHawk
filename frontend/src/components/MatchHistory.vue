<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { listarPartidas, obtenerPartida } from '../api/matches'

const emit = defineEmits(['partidaSeleccionada'])
const partidas = ref([])
const cargando = ref(false)
const busqueda = ref('')

// Variables para la paginación
const paginaActual = ref(1)
const partidasPorPagina = 10

const cargarHistorial = async () => {
  try {
    cargando.value = true
    partidas.value = await listarPartidas()
    paginaActual.value = 1 // Reseteamos a la primera página al recargar
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

// 1. Filtramos por el buscador
const partidasFiltradas = computed(() => {
  if (!busqueda.value) return partidas.value
  
  const termino = busqueda.value.toLowerCase()
  return partidas.value.filter(p => {
    const nombresA = p.equipoA && Array.isArray(p.equipoA) ? p.equipoA.map(j => j.nombre.toLowerCase()).join(' ') : ''
    const nombresB = p.equipoB && Array.isArray(p.equipoB) ? p.equipoB.map(j => j.nombre.toLowerCase()).join(' ') : ''
    return nombresA.includes(termino) || nombresB.includes(termino)
  })
})

// 2. Calculamos el total de páginas basado en los resultados filtrados
const totalPaginas = computed(() => {
  return Math.ceil(partidasFiltradas.value.length / partidasPorPagina)
})

// 3. Recortamos la lista para mostrar solo los 10 de la página actual
const partidasPaginadas = computed(() => {
  const inicio = (paginaActual.value - 1) * partidasPorPagina
  const fin = inicio + partidasPorPagina
  return partidasFiltradas.value.slice(inicio, fin)
})

// Si el usuario busca algo, regresamos a la página 1
watch(busqueda, () => {
  paginaActual.value = 1
})

const siguientePagina = () => {
  if (paginaActual.value < totalPaginas.value) paginaActual.value++
}

const anteriorPagina = () => {
  if (paginaActual.value > 1) paginaActual.value--
}

const codigoCorto = (id) => id ? id.substring(0, 6).toUpperCase() : 'ERR'

onMounted(cargarHistorial)
</script>

<template>
  <div class="historial-container">
    <div class="header-historial">
      <h2>Historial de Partidas</h2>
      <button @click="cargarHistorial" :disabled="cargando" class="btn-refrescar" title="Actualizar lista">
        {{ cargando ? 'Cargando...' : '🔄 Actualizar' }}
      </button>
    </div>
    
    <div class="acciones">
      <input 
        v-model="busqueda" 
        type="text" 
        placeholder="🔍 Buscar por nombre de jugador..." 
        class="buscador"
      />
    </div>

    <ul class="lista-partidas">
      <!-- Usamos partidasPaginadas en lugar de partidasFiltradas -->
      <li v-for="p in partidasPaginadas" :key="p.id" @click="seleccionar(p.id)">
        <div class="info">
          <strong>{{ p.modalidad }} ({{ p.formato || 'INDIVIDUAL' }})</strong> - 
          {{ p.equipoA?.[0]?.nombre || 'Equipo A' }} vs {{ p.equipoB?.[0]?.nombre || 'Equipo B' }}
          <br>
          <small>
            Código: <strong>#{{ codigoCorto(p.id) }}</strong> | 
            {{ p.fase }} | {{ p.fechaCreacion ? new Date(p.fechaCreacion).toLocaleString() : 'Sin fecha' }}
          </small>
        </div>
        <button class="btn-cargar">Cargar ➔</button>
      </li>
    </ul>
    
    <p v-if="partidasPaginadas.length === 0 && !cargando" class="vacio">
      No se encontraron partidas.
    </p>

    <!-- Controles de Paginación -->
    <div v-if="totalPaginas > 1" class="paginacion">
      <button @click="anteriorPagina" :disabled="paginaActual === 1">◀ Anterior</button>
      <span>Página {{ paginaActual }} de {{ totalPaginas }}</span>
      <button @click="siguientePagina" :disabled="paginaActual === totalPaginas">Siguiente ▶</button>
    </div>
  </div>
</template>

<style scoped>
.historial-container { max-width: 800px; margin: 2rem auto; color: white; background: #1e293b; padding: 2rem; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.header-historial { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.header-historial h2 { margin: 0; color: #22c55e; }
.btn-refrescar { background: #334155; color: white; border: 1px solid #475569; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
.btn-refrescar:hover { background: #475569; }
.acciones { display: flex; gap: 1rem; margin-bottom: 1.5rem; }
.buscador { flex: 1; padding: 0.8rem; border-radius: 6px; border: 1px solid #334155; background: #0f172a; color: white; font-size: 1rem; outline: none; }
.buscador:focus { border-color: #22c55e; }
.lista-partidas { list-style: none; padding: 0; margin: 0; }
.lista-partidas li { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border-bottom: 1px solid #334155; cursor: pointer; transition: background 0.2s; }
.lista-partidas li:hover { background: #0f172a; }
.info { flex: 1; }
.info strong { color: #22c55e; }
.info small { color: #94a3b8; }
.btn-cargar { background: #22c55e; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-cargar:hover { background: #16a34a; }
.vacio { text-align: center; color: #94a3b8; margin-top: 2rem; }

/* Estilos de paginación */
.paginacion { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 2rem; padding-top: 1rem; border-top: 1px solid #334155; }
.paginacion button { background: #334155; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-weight: bold; }
.paginacion button:disabled { opacity: 0.3; cursor: not-allowed; }
.paginacion span { color: #94a3b8; font-size: 0.9rem; }
</style>