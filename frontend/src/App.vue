<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'

// Imports que vienen de develop
import ToastContainer from './components/ToastContainer.vue'
import LoginView from './components/LoginView.vue'
import CorrectionModal from './components/CorrectionModal.vue'
import { AUTH_REQUERIDA } from './api/endpoints'
import { obtenerPartida } from './api/matches' // Necesario para recargarPartida

const partidaActual = ref(null)
const formatoPartida = ref('INDIVIDUAL') // Guarda el formato (de tu rama)
const token = ref(localStorage.getItem('token')) // De develop
const mostrarCorreccion = ref(false) // De develop

let socket = null
let reconnectTimeout = null

const iniciarSesion = (nuevoToken) => {
  token.value = nuevoToken
}

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
  // Lógica de tu rama para el formato
  formatoPartida.value = nuevaPartida.formato || (nuevaPartida.equipoA.length === 2 ? 'PAREJAS' : 'INDIVIDUAL')
}

const recargarPartida = async () => {
  if (!partidaActual.value) return
  const datos = await obtenerPartida(partidaActual.value.id)
  partidaActual.value = { ...datos }
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = { ...partida }
  formatoPartida.value = partida.formato || (partida.equipoA.length === 2 ? 'PAREJAS' : 'INDIVIDUAL')
}

const cerrarSesion = () => {
  localStorage.removeItem('token')
  token.value = null
  partidaActual.value = null
  mostrarCorreccion.value = false
  window.location.reload()
}

const conectarWebSocket = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

  // Lógica de conexión robusta (soporta local y producción)
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsHost = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
  socket = new WebSocket(`${wsProtocol}//${wsHost}/ws/matches`)

  socket.onopen = () => console.log('🟢 Conectado al WebSocket de PoolHawk')

  socket.onmessage = (event) => {
    const partidaActualizada = JSON.parse(event.data)
    if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
      console.log('⚡ Marcador actualizado en vivo:', partidaActualizada.puntajes)
      // Forzamos la reactividad de Vue con un nuevo objeto
      partidaActual.value = { ...partidaActualizada }
    }
  }

  socket.onclose = () => {
    console.log('🔴 WebSocket desconectado. Reconectando en 3 segundos...')
    socket = null
    if (!reconnectTimeout) {
      reconnectTimeout = setTimeout(() => {
        reconnectTimeout = null
        conectarWebSocket()
      }, 3000)
    }
  }

  socket.onerror = (error) => console.error('Error en WebSocket:', error)
}

onMounted(() => {
  conectarWebSocket()
})

onUnmounted(() => {
  if (reconnectTimeout) clearTimeout(reconnectTimeout)
  if (socket) socket.close()
})
</script>

<template>
  <div class="app-container">
    <ToastContainer />
    <header>
      <h1>🦅 PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
      <button v-if="token || !AUTH_REQUERIDA" @click="cerrarSesion">Cerrar sesión</button>
    </header>

    <main>
      <!-- Login: solo se muestra si AUTH_REQUERIDA esta activa -->
      <LoginView v-if="AUTH_REQUERIDA && !token" @ingreso="iniciarSesion" />

      <template v-else>
        <!-- Si no hay partida, mostrar formulario -->
        <div v-if="!partidaActual">
          <CreateMatchForm @partidaIniciada="iniciarPartida" />
          <MatchHistory @partidaSeleccionada="cargarPartidaDelHistorial" />
        </div>

        <!-- Si hay partida, mostrar marcador y simulador -->
        <div v-else>
          <button @click="partidaActual = null; mostrarCorreccion = false" class="btn-salir">← Nueva Partida</button>
          
          <!-- El ScoreBoard ahora recibe el formato y la key para forzar el re-render -->
          <ScoreBoard 
            :key="JSON.stringify(partidaActual.puntajes) + partidaActual.jugadorActual" 
            :partida="partidaActual" 
            :formato="formatoPartida" 
          />
          
          <!-- El ShotSimulator ya no emite tiroRegistrado porque el WebSocket se encarga -->
          <ShotSimulator :partida="partidaActual" />
          
          <button @click="mostrarCorreccion = true" class="btn-corregir">Corregir último tiro</button>
          
          <CorrectionModal
            v-if="mostrarCorreccion"
            :partida="partidaActual"
            @cerrar="mostrarCorreccion = false"
            @corregido="recargarPartida"
          />
        </div>
      </template>
    </main>
  </div>
</template>

<style>
body {
  margin: 0;
  background-color: #121212;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.app-container {
  min-height: 100vh;
  padding: 2rem;
}

header {
  text-align: center;
  margin-bottom: 3rem;
  color: white;
}

header h1 {
  margin: 0;
  font-size: 3rem;
}

.subtitle {
  color: #888;
  margin-top: 0.5rem;
}

.btn-salir {
  display: block;
  margin-bottom: 1rem;
  background: transparent;
  color: #aaa;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

.btn-salir:hover {
  color: white;
}

.btn-corregir {
  display: block;
  width: 100%;
  max-width: 800px;
  margin: 1rem auto 0;
  padding: 0.8rem;
  background: transparent;
  color: #f59e0b;
  border: 1px solid #f59e0b;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
}

.btn-corregir:hover {
  background: rgba(245, 158, 11, 0.12);
}
</style>