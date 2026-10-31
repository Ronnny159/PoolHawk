<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'

const partidaActual = ref(null)
const formatoPartida = ref('INDIVIDUAL') // NUEVO: Guarda el formato
let socket = null
let reconnectTimeout = null

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
  // Si la partida trae formato lo usa, si no, asume PAREJAS si hay 4 jugadores
  formatoPartida.value = nuevaPartida.formato || (nuevaPartida.equipoA.length === 2 ? 'PAREJAS' : 'INDIVIDUAL')
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = partida
  formatoPartida.value = partida.formato || (partida.equipoA.length === 2 ? 'PAREJAS' : 'INDIVIDUAL')
}

const cerrarSesion = () => {
  localStorage.removeItem('token')
  window.location.reload()
}

const conectarWebSocket = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

  socket = new WebSocket('ws://localhost:8080/ws/matches')

  socket.onopen = () => console.log('🟢 Conectado al WebSocket de PoolHawk')

  socket.onmessage = (event) => {
    const partidaActualizada = JSON.parse(event.data)
    if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
      console.log('⚡ Marcador actualizado en vivo')
      partidaActual.value = partidaActualizada
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

onMounted(() => conectarWebSocket())

onUnmounted(() => {
  if (reconnectTimeout) clearTimeout(reconnectTimeout)
  if (socket) socket.close()
})
</script>

<template>
  <div class="app-container">
    <header>
      <h1>🦅 PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
      <button @click="cerrarSesion">Salir</button>
    </header>

    <main>
      <div v-if="!partidaActual">
        <CreateMatchForm @partidaIniciada="iniciarPartida" />
        <MatchHistory @partidaSeleccionada="cargarPartidaDelHistorial" />
      </div>

      <div v-else>
        <button @click="partidaActual = null" class="btn-salir">← Nueva Partida</button>
        
        <!-- Le pasamos el formato al ScoreBoard -->
        <ScoreBoard :partida="partidaActual" :formato="formatoPartida" />
        <ShotSimulator :partida="partidaActual" />
      </div>
    </main>
  </div>
</template>

<style>
body { margin: 0; background-color: #121212; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
.app-container { min-height: 100vh; padding: 2rem; }
header { text-align: center; margin-bottom: 3rem; color: white; }
header h1 { margin: 0; font-size: 3rem; }
.subtitle { color: #888; margin-top: 0.5rem; }
.btn-salir { display: block; margin-bottom: 1rem; background: transparent; color: #aaa; border: none; cursor: pointer; font-size: 1rem; }
.btn-salir:hover { color: white; }
</style>