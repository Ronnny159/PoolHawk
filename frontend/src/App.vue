<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'
import ToastContainer from './components/ToastContainer.vue'
import LoginView from './components/LoginView.vue'
import CorrectionModal from './components/CorrectionModal.vue'
import { AUTH_REQUERIDA } from './api/endpoints'
import { obtenerPartida } from './api/matches'

const tokenGuardado = localStorage.getItem('token')
const token = ref(tokenGuardado && tokenGuardado.length > 20 ? tokenGuardado : null)
if (!token.value) localStorage.removeItem('token')

const partidaActual = ref(null)
const formatoPartida = ref('INDIVIDUAL')
const mostrarCorreccion = ref(false)
const vistaLobby = ref('CREAR')

let socket = null
let reconnectTimeout = null

const iniciarSesion = (nuevoToken) => {
  token.value = nuevoToken
  localStorage.setItem('token', nuevoToken)
}

// FUNCION AUXILIAR PARA DETECTAR EL FORMATO
const detectarFormato = (partida) => {
  // Si el backend lo envia, perfecto
  if (partida.formato) return partida.formato
  // Si no, lo adivinamos por la cantidad de jugadores en los equipos
  if (partida.equipoA?.length === 2 && partida.equipoB?.length === 2) {
    // Si ambos equipos tienen 2, es PAREJAS (2v2)
    return 'PAREJAS'
  }
  // Si no, es INDIVIDUAL (1v1 o Todos vs Todos)
  return 'INDIVIDUAL'
}

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
  formatoPartida.value = detectarFormato(nuevaPartida)
}

const recargarPartida = async () => {
  if (!partidaActual.value) return
  const datos = await obtenerPartida(partidaActual.value.id)
  partidaActual.value = { ...datos }
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = { ...partida }
  formatoPartida.value = detectarFormato(partida)
}

const cerrarSesion = () => {
  localStorage.removeItem('token')
  token.value = null
  partidaActual.value = null
  mostrarCorreccion.value = false
  window.location.reload()
}

const volverAlLobby = () => {
  partidaActual.value = null
  mostrarCorreccion.value = false
  vistaLobby.value = 'CREAR'
}

const conectarWebSocket = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsHost = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
  socket = new WebSocket(`${wsProtocol}//${wsHost}/ws/matches`)

  socket.onopen = () => console.log('🟢 Conectado al WebSocket de PoolHawk')

  socket.onmessage = (event) => {
    try {
      const partidaActualizada = JSON.parse(event.data)
      if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
        console.log('⚡ Marcador actualizado en vivo')
        partidaActual.value = { ...partidaActualizada }
        // Actualizamos también el formato por si acaso
        formatoPartida.value = detectarFormato(partidaActualizada)
      }
    } catch (e) {
      console.error('Error parseando mensaje WebSocket', e)
    }
  }

  socket.onclose = () => {
    console.log('🔴 WebSocket desconectado. Reconectando...')
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
      <button v-if="token || !AUTH_REQUERIDA" @click="cerrarSesion" class="btn-logout">Cerrar sesión</button>
    </header>

    <main>
      <LoginView v-if="AUTH_REQUERIDA && !token" @ingreso="iniciarSesion" />

      <template v-else-if="!partidaActual">
        <div class="lobby-nav">
          <button :class="{ active: vistaLobby === 'CREAR' }" @click="vistaLobby = 'CREAR'">🎱 Nueva Partida</button>
          <button :class="{ active: vistaLobby === 'HISTORIAL' }" @click="vistaLobby = 'HISTORIAL'">📜 Historial</button>
        </div>

        <CreateMatchForm v-if="vistaLobby === 'CREAR'" @partidaIniciada="iniciarPartida" />
        <MatchHistory v-if="vistaLobby === 'HISTORIAL'" @partidaSeleccionada="cargarPartidaDelHistorial" />
      </template>

      <template v-else>
        <button @click="volverAlLobby" class="btn-salir">← Volver al Lobby</button>
        
        <ScoreBoard 
          :key="JSON.stringify(partidaActual.puntajes) + partidaActual.jugadorActual" 
          :partida="partidaActual" 
          :formato="formatoPartida" 
        />
        
        <ShotSimulator :partida="partidaActual" />
        
        <button @click="mostrarCorreccion = true" class="btn-corregir">✏️ Corregir último tiro</button>
        
        <CorrectionModal
          v-if="mostrarCorreccion"
          :partida="partidaActual"
          @cerrar="mostrarCorreccion = false"
          @corregido="recargarPartida"
        />
      </template>
    </main>
  </div>
</template>

<style>
/* ... Los mismos estilos que ya tienes ... */
body { margin: 0; background-color: #121212; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
.app-container { min-height: 100vh; padding: 2rem; }
header { text-align: center; margin-bottom: 3rem; color: white; position: relative; }
header h1 { margin: 0; font-size: 3rem; }
.subtitle { color: #888; margin-top: 0.5rem; }
.btn-logout { position: absolute; top: 0; right: 0; background: transparent; color: #aaa; border: 1px solid #444; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; }
.btn-logout:hover { color: white; border-color: #666; }
.btn-salir { display: block; margin-bottom: 1rem; background: transparent; color: #aaa; border: none; cursor: pointer; font-size: 1rem; }
.btn-salir:hover { color: white; }
.btn-corregir { display: block; width: 100%; max-width: 800px; margin: 1rem auto 0; padding: 0.8rem; background: transparent; color: #f59e0b; border: 1px solid #f59e0b; border-radius: 8px; font-weight: bold; cursor: pointer; transition: all 0.2s; }
.btn-corregir:hover { background: rgba(245, 158, 11, 0.12); }
.lobby-nav { display: flex; justify-content: center; gap: 1rem; margin-bottom: 2rem; }
.lobby-nav button { padding: 0.8rem 2rem; background: #1e293b; color: #94a3b8; border: 1px solid #334155; border-radius: 8px; font-size: 1.1rem; font-weight: bold; cursor: pointer; transition: all 0.2s; }
.lobby-nav button:hover { background: #334155; color: white; }
.lobby-nav button.active { background: #22c55e; color: white; border-color: #22c55e; }
</style>