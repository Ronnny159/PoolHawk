<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

const todosLosJugadores = computed(() => {
  return [...(props.partida.equipoA || []), ...(props.partida.equipoB || [])]
})

const nombreJugadorActual = computed(() => {
  if (!props.partida.jugadorActual) return ''
  return typeof props.partida.jugadorActual === 'object' ? props.partida.jugadorActual.nombre : props.partida.jugadorActual
})

const coloresBolas = [
  'radial-gradient(circle at 30% 30%, #fef08a, #eab308 60%, #a16207 100%)', // Amarilla
  'radial-gradient(circle at 30% 30%, #bfdbfe, #3b82f6 60%, #1e3a8a 100%)', // Azul
  'radial-gradient(circle at 30% 30%, #fecaca, #ef4444 60%, #7f1d1d 100%)', // Roja
  'radial-gradient(circle at 30% 30%, #bbf7d0, #22c55e 60%, #14532d 100%)'  // Verde
]
</script>

<template>
  <div class="grilla-4-jugadores">
    <div 
      v-for="(jugador, index) in todosLosJugadores" 
      :key="jugador.nombre"
      class="jugador-individual"
      :class="{ activo: jugador.nombre === nombreJugadorActual }"
    >
      <span class="nombre">{{ jugador.nombre }}</span>
      <div class="bola-marcador" :style="{ background: coloresBolas[index % coloresBolas.length] }">
        <span class="puntos">{{ props.partida.puntajes?.[jugador.id] || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grilla-4-jugadores { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
.jugador-individual { display: flex; flex-direction: column; align-items: center; padding: 1rem; border-radius: 12px; border: 2px solid transparent; transition: all 0.3s ease; }
.jugador-individual.activo { background: rgba(21, 128, 61, 0.2); border-color: var(--felt-light); box-shadow: 0 0 25px rgba(34, 197, 94, 0.4); transform: scale(1.02); }
.nombre { font-size: 1.3rem; font-weight: 800; color: #cbd5e1; margin-bottom: 1rem; text-transform: uppercase; text-align: center; }
.jugador-individual.activo .nombre { color: var(--felt-light); }
.bola-marcador { width: 100px; height: 100px; border-radius: 50%; display: flex; justify-content: center; align-items: center; position: relative; box-shadow: inset -15px -15px 25px rgba(0,0,0,0.5), inset 10px 10px 15px rgba(255,255,255,0.6), 5px 5px 15px rgba(0,0,0,0.5); }
.bola-marcador::before { content: ''; position: absolute; width: 45px; height: 45px; background: white; border-radius: 50%; box-shadow: inset 2px 2px 5px rgba(0,0,0,0.3); }
.puntos { position: relative; z-index: 10; font-family: 'Bebas Neue', sans-serif; font-size: 2.2rem; color: black; font-weight: bold; }
</style>