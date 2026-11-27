<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'
import MatchHistory from './components/MatchHistory.vue'
import ToastContainer from './components/ToastContainer.vue'
import LoginView from './components/LoginView.vue'
import CorrectionModal from './components/CorrectionModal.vue'
import VideoAnalysisConfig from './components/VideoAnalysisConfig.vue'
import { AUTH_REQUERIDA } from './api/endpoints'
import { obtenerPartida } from './api/matches'
import { toast } from './utils/toast'

const tokenGuardado = localStorage.getItem('token')
const token = ref(tokenGuardado && tokenGuardado.length > 20 ? tokenGuardado : null)
if (!token.value) localStorage.removeItem('token')

const partidaActual = ref(null)
const formatoPartida = ref('INDIVIDUAL')
const mostrarCorreccion = ref(false)
const vistaLobby = ref('CREAR')
const cargandoPartida = ref(false)

let socket = null
let reconnectTimeout = null

const iniciarSesion = (nuevoToken) => {
  token.value = nuevoToken
  localStorage.setItem('token', nuevoToken)
  // Reconectar WebSocket con el nuevo token
  conectarWebSocket()
}

const detectarFormato = (partida) => {
  if (partida.formato) return partida.formato
  if (partida.equipoA?.length === 2 && partida.equipoB?.length === 2) {
    return 'PAREJAS'
  }
  return 'INDIVIDUAL'
}

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
  formatoPartida.value = detectarFormato(nuevaPartida)
  // Reconectar WebSocket al cambiar de partida
  conectarWebSocket()
}

const recargarPartida = async () => {
  if (!partidaActual.value || cargandoPartida.value) return
  cargandoPartida.value = true
  try {
    const datos = await obtenerPartida(partidaActual.value.id)
    partidaActual.value = { ...datos }
  } catch (error) {
    console.error('Error recargando partida:', error)
    toast.error('No se pudo recargar la partida')
  } finally {
    cargandoPartida.value = false
  }
}

const cargarPartidaDelHistorial = (partida) => {
  partidaActual.value = { ...partida }
  formatoPartida.value = detectarFormato(partida)
  conectarWebSocket()
}

const cerrarSesion = () => {
  localStorage.removeItem('token')
  token.value = null
  partidaActual.value = null
  mostrarCorreccion.value = false
  if (socket) {
    socket.close()
    socket = null
  }
  window.location.reload()
}

const volverAlLobby = () => {
  partidaActual.value = null
  mostrarCorreccion.value = false
  vistaLobby.value = 'CREAR'
  if (socket) {
    socket.close()
    socket = null
  }
}

const conectarWebSocket = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return

  // Construir la URL del WebSocket.
  // - Si VITE_API_BASE_URL apunta a un backend externo (host distinto al de la página),
  //   se usa ese host directamente.
  // - En cualquier otro caso (frontend servido por nginx en :80 o Vite dev), se conecta
  //   al MISMO host/puerto que sirve la página, para que nginx (location /ws/) o el proxy
  //   de Vite reenvíen el handshake al backend. Esto evita problemas de CORS/origin.
  const apiBase = import.meta.env.VITE_API_BASE_URL
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'

  let wsHost
  if (apiBase) {
    const apiHost = new URL(apiBase, window.location.origin).host
    // Solo usar el host del backend si es diferente al de la página (backend externo)
    wsHost = (apiHost !== window.location.host) ? apiHost : window.location.host
  } else {
    wsHost = window.location.host
  }

  // Incluir token en la URL del WebSocket para autenticación
  let wsUrl = `${wsProtocol}//${wsHost}/ws/matches`
  if (token.value) {
    wsUrl += `?token=${encodeURIComponent(token.value)}`
  }

  console.log('🔌 Conectando WebSocket a:', wsUrl)
  socket = new WebSocket(wsUrl)
  
  socket.onmessage = (event) => {
    try {
      const partidaActualizada = JSON.parse(event.data)
      if (partidaActual.value && partidaActual.value.id === partidaActualizada.id) {
        console.log('📊 Marcador actualizado en vivo')
        partidaActual.value = { ...partidaActualizada }
        formatoPartida.value = detectarFormato(partidaActualizada)
      }
    } catch (e) {
      console.error('Error parseando mensaje WebSocket', e)
    }
  }
  
  socket.onclose = () => {
    console.log('🔌 WebSocket desconectado. Reconectando en 3s...')
    socket = null
    if (!reconnectTimeout) {
      reconnectTimeout = setTimeout(() => {
        reconnectTimeout = null
        conectarWebSocket()
      }, 3000)
    }
  }
  
  socket.onerror = (error) => {
    console.error('Error en WebSocket:', error)
  }
  
  socket.onopen = () => {
    console.log('✅ WebSocket conectado')
  }
}

// Observar cambios en el token para reconectar WebSocket
watch(token, (nuevoToken) => {
  if (nuevoToken) {
    conectarWebSocket()
  }
})

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
      <h1> PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
      <button v-if="token || !AUTH_REQUERIDA" @click="cerrarSesion" class="btn-logout">Cerrar sesión</button>
    </header>
    <main>
      <LoginView v-if="AUTH_REQUERIDA && !token" @ingreso="iniciarSesion" />
      <template v-else-if="!partidaActual">
        <div class="lobby-nav">
          <button :class="{ active: vistaLobby === 'CREAR' }" @click="vistaLobby = 'CREAR'">🎱 Nueva Partida</button>
          <button :class="{ active: vistaLobby === 'HISTORIAL' }" @click="vistaLobby = 'HISTORIAL'">📜 Historial</button>
          <button :class="{ active: vistaLobby === 'CONFIG' }" @click="vistaLobby = 'CONFIG'">🎬 Análisis de Video</button>
        </div>
        <CreateMatchForm v-if="vistaLobby === 'CREAR'" @partidaIniciada="iniciarPartida" />
        <MatchHistory v-if="vistaLobby === 'HISTORIAL'" @partidaSeleccionada="cargarPartidaDelHistorial" />
        <VideoAnalysisConfig v-if="vistaLobby === 'CONFIG'" />
      </template>
      <template v-else>
        <button @click="volverAlLobby" class="btn-salir">← Volver al Lobby</button>
        
        <!-- Skeleton loader mientras carga la partida -->
        <div v-if="cargandoPartida" class="skeleton-loader">
          <div class="skeleton-header"></div>
          <div class="skeleton-teams"></div>
        </div>
        
        <ScoreBoard 
          v-else
          :key="partidaActual.id + '|' + partidaActual.jugadorActual + '|' + JSON.stringify(partidaActual.puntajes)"
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

/* Skeleton loader */
.skeleton-loader {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  background: #1e293b;
  border: 4px solid #0f172a;
  border-radius: 12px;
  box-shadow: inset 0 0 30px rgba(0,0,0,0.8), 0 20px 40px rgba(0,0,0,0.6);
}

.skeleton-header {
  height: 3rem;
  background: linear-gradient(90deg, #1e293b 25%, #334155 50%, #1e293b 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8px;
  margin-bottom: 2rem;
  border-bottom: 1px solid #334155;
  padding-bottom: 1rem;
}

.skeleton-teams {
  display: flex;
  justify-content: space-around;
  gap: 2rem;
}

.skeleton-team {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.skeleton-ball {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(90deg, #1e293b 25%, #334155 50%, #1e293b 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin: 1rem 0;
}

.skeleton-name {
  height: 1.5rem;
  width: 60%;
  background: linear-gradient(90deg, #1e293b 25%, #334155 50%, #1e293b 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>