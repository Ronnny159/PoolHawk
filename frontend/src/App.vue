<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ref, onMounted } from 'vue'
import { obtenerPartida } from './api/matches'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'

let socket = null

onMounted(() => {
  // Conectar al WebSocket del backend
  socket = new WebSocket('ws://localhost:8080/ws/matches')
  
  socket.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (partidaActual.value && partidaActual.value.id === data.matchId) {
      console.log('¡WebSocket recibió un tiro!')
      recargarPartida() // Vue se actualiza en tiempo real
    }
  }
})

onUnmounted(() => {
  if (socket) socket.close()
})

const partidaActual = ref(null)

const iniciarPartida = (nuevaPartida) => {
  partidaActual.value = nuevaPartida
}

const recargarPartida = async () => {
  if (!partidaActual.value) return
  const datos = await obtenerPartida(partidaActual.value.id)
  partidaActual.value = datos
}
</script>

<template>
  <div class="app-container">
    <!-- CABECERA PREMIUM -->
    <header class="app-header">
      <h1>
        <span class="logo-icon">🎱</span> POOLHAWK
      </h1>
      <p class="subtitle">SISTEMA DE RASTREO Y MARCADO AUTOMÁTICO</p>
    </header>

    <main class="main-content">
      <!-- BOTÓN NUEVA PARTIDA (Solo visible si hay partida activa) -->
      <div v-if="partidaActual" class="action-bar">
        <button @click="partidaActual = null" class="btn-nueva-partida">
          <span>←</span> NUEVA PARTIDA
        </button>
      </div>

      <!-- FORMULARIO O MARCADOR -->
      <div v-if="!partidaActual" class="view-container">
        <CreateMatchForm @partidaIniciada="iniciarPartida" />
      </div>

      <div v-else class="view-container">
        <ScoreBoard :partida="partidaActual" />
        <ShotSimulator :partida="partidaActual" @tiroRegistrado="recargarPartida" />
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ============================================================
   1. CONTENEDOR GLOBAL (Aprovecha el fondo de style.css)
   ============================================================ */
.app-container {
  min-height: 100vh;
  padding: 2rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ============================================================
   2. CABECERA PREMIUM
   ============================================================ */
.app-header {
  text-align: center;
  margin-bottom: 2.5rem;
  position: relative;
}

.app-header h1 {
  margin: 0;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 4rem;
  color: #ffffff;
  letter-spacing: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  /* Efecto de texto metálico/neón */
  text-shadow: 
    0 2px 0 #000,
    0 4px 10px rgba(34, 197, 94, 0.4), /* Halo verde */
    0 8px 20px rgba(0, 0, 0, 0.8);
}

.logo-icon {
  font-size: 3.5rem;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.8));
  /* Pequeña animación de flotación para la bola 8 */
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-5px); }
  100% { transform: translateY(0px); }
}

.subtitle {
  margin: 0.5rem 0 0;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--felt-light);
  letter-spacing: 3px;
  text-transform: uppercase;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
}

/* ============================================================
   3. BARRA DE ACCIONES Y BOTÓN "NUEVA PARTIDA"
   ============================================================ */
.action-bar {
  width: 100%;
  max-width: 950px; /* Mismo ancho que la mesa */
  margin: 0 auto 1rem;
  display: flex;
  justify-content: flex-start;
}

.btn-nueva-partida {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(180deg, #334155 0%, #1e293b 100%);
  color: #e2e8f0;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.2rem;
  letter-spacing: 1.5px;
  padding: 8px 20px;
  border: 1px solid #0f172a;
  border-radius: 6px;
  cursor: pointer;
  /* Efecto de relieve físico */
  box-shadow: 
    0 4px 0 #0f172a,
    0 6px 12px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: all 0.1s ease;
}

.btn-nueva-partida span {
  font-size: 1.4rem;
  line-height: 1;
}

.btn-nueva-partida:hover {
  background: linear-gradient(180deg, #475569 0%, #334155 100%);
  transform: translateY(-1px);
  box-shadow: 
    0 5px 0 #0f172a,
    0 8px 15px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.btn-nueva-partida:active {
  transform: translateY(3px);
  box-shadow: 
    0 1px 0 #0f172a,
    0 3px 6px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

/* ============================================================
   4. CONTENEDOR DE VISTAS
   ============================================================ */
.view-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============================================================
   5. RESPONSIVE
   ============================================================ */
@media (max-width: 768px) {
  .app-header h1 {
    font-size: 3rem;
    gap: 10px;
  }
  .logo-icon {
    font-size: 2.5rem;
  }
  .subtitle {
    font-size: 0.75rem;
    letter-spacing: 2px;
  }
}
</style>