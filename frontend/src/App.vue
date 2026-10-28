<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { obtenerPartida } from './api/matches'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'


const partidaActual = ref(null)
let socket = null

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
}

// Mantenemos esta función por si acaso el simulador la usa
const recargarPartida = async () => {
  if (!partidaActual.value) return
  const datos = await obtenerPartida(partidaActual.value.id)
  partidaActual.value = datos
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = partida
}

// ───────────────────────────────────────────────────────────
// Lógica del WebSocket
// ───────────────────────────────────────────────────────────
const conectarWebSocket = () => {
  // Nos conectamos al endpoint que creaste en Spring Boot
  socket = new WebSocket('ws://localhost:8080/ws/matches')

  socket.onopen = () => {
    console.log('🟢 Conectado al WebSocket de PoolHawk')
  }

  socket.onmessage = (event) => {
    // El backend envía la partida completa en formato JSON
    const partidaActualizada = JSON.parse(event.data)
    
    // Solo actualizamos la pantalla si es la partida que estamos viendo
    if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
      console.log('⚡ Marcador actualizado en vivo')
      partidaActual.value = partidaActualizada
    }
  }

  socket.onclose = () => {
    console.log('🔴 WebSocket desconectado. Reconectando en 3 segundos...')
    setTimeout(conectarWebSocket, 3000) // Reconexión automática si se cae el server
  }

  socket.onerror = (error) => {
    console.error('Error en WebSocket:', error)
  }
}

// Conectamos el WebSocket cuando la app arranca
onMounted(() => {
  conectarWebSocket()
})

// Cerramos el WebSocket cuando se cierra la pestaña
onUnmounted(() => {
  if (socket) socket.close()
})
</script>

<template>
  <div class="app-container">
    <header>
      <h1> PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
    </header>

    <main>
      <!-- Si no hay partida, mostrar formulario -->
      <div v-if="!partidaActual">
        <CreateMatchForm @partidaIniciada="iniciarPartida" />
        <MatchHistory @partidaSeleccionada="cargarPartidaDelHistorial" />
      </div>

      <!-- Si hay partida, mostrar marcador y simulador -->
      <div v-else>
        <button @click="partidaActual = null" class="btn-salir">← Nueva Partida</button>
        <ScoreBoard :partida="partidaActual" />
        <ShotSimulator :partida="partidaActual" @tiroRegistrado="recargarPartida" />
      </div>
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
</style>