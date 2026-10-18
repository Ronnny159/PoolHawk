<script setup>
import { ref, onMounted } from 'vue'
import { obtenerPartida } from './api/matches'
import CreateMatchForm from './components/CreateMatchForm.vue'
import ScoreBoard from './components/ScoreBoard.vue'
import ShotSimulator from './components/ShotSimulator.vue'

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
    <header>
      <h1> PoolHawk</h1>
      <p class="subtitle">Sistema de Rastreo y Marcado Automático</p>
    </header>

    <main>
      <!-- Si no hay partida, mostrar formulario -->
      <div v-if="!partidaActual">
        <CreateMatchForm @partidaIniciada="iniciarPartida" />
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