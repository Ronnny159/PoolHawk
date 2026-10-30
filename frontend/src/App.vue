<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { obtenerPartida } from './api/matches'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'
import ToastContainer from './components/ToastContainer.vue'
import LoginView from './components/LoginView.vue'
import { AUTH_REQUERIDA } from './api/endpoints'

const partidaActual = ref(null)
const token = ref(localStorage.getItem('token'))

// Guarda el token que entrega LoginView (el propio LoginView ya lo puso en localStorage)
const iniciarSesion = (nuevoToken) => {
  token.value = nuevoToken
}
let socket = null

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
}

const recargarPartida = async () => {
  if (!partidaActual.value) return
  const datos = await obtenerPartida(partidaActual.value.id)
  partidaActual.value = { ...datos }
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = { ...partida }
}

const cerrarSesion = () => {
  localStorage.removeItem('token')
  token.value = null
  partidaActual.value = null
}

// ───────────────────────────────────────────────────────────
// Lógica del WebSocket
// ───────────────────────────────────────────────────────────
const conectarWebSocket = () => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsHost = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
  socket = new WebSocket(`${wsProtocol}//${wsHost}/ws/matches`)

  socket.onopen = () => {
    console.log('🟢 Conectado al WebSocket de PoolHawk')
  }

  socket.onmessage = (event) => {
    const partidaActualizada = JSON.parse(event.data)
    
    // Solo actualizamos si es la partida que estamos viendo
    if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
      console.log('⚡ Marcador actualizado en vivo:', partidaActualizada.puntajes)
      
      // LA MAGIA: Asignamos un nuevo objeto para forzar el re-render en Vue 3
      partidaActual.value = { ...partidaActualizada }
    }
  }

  socket.onclose = () => {
    console.log('🔴 WebSocket desconectado. Reconectando en 3 segundos...')
    setTimeout(conectarWebSocket, 3000)
  }

  socket.onerror = (error) => {
    console.error('Error en WebSocket:', error)
  }
}

onMounted(() => {
  conectarWebSocket()
})

onUnmounted(() => {
  if (socket) socket.close()
})
</script>

<template>
  <div class="app-container">
    <ToastContainer />
    <header>
      <h1> PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
      <button v-if="token || !AUTH_REQUERIDA" @click="cerrarSesion">Cerrar sesi&oacute;n</button>
    </header>

    <main>
      <!-- Login: solo se muestra si AUTH_REQUERIDA esta activa (ver api/endpoints.js) -->
      <LoginView v-if="AUTH_REQUERIDA && !token" @ingreso="iniciarSesion" />

      <template v-else>
      <!-- Si no hay partida, mostrar formulario -->
      <div v-if="!partidaActual">
        <CreateMatchForm @partidaIniciada="iniciarPartida" />
        <MatchHistory @partidaSeleccionada="cargarPartidaDelHistorial" />
      </div>

      <!-- Si hay partida, mostrar marcador y simulador -->
      <div v-else>
        <button @click="partidaActual = null" class="btn-salir">← Nueva Partida</button>
        
        <!-- EL TRUCO: El :key obliga a ScoreBoard a redibujarse si los puntajes o el turno cambian -->
        <ScoreBoard 
          :key="JSON.stringify(partidaActual.puntajes) + partidaActual.jugadorActual" 
          :partida="partidaActual" 
        />
        
        <ShotSimulator :partida="partidaActual" @tiroRegistrado="recargarPartida" />
      </div>
    </template>
      <!-- fin del contenido protegido por login -->
    </main>
  </div>
</template>

<style>
/* Tus estilos se mantienen igual */
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
</style>