<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

const nombreJugadorActual = computed(() => {
  if (!props.partida.jugadorActual) return ''
  return typeof props.partida.jugadorActual === 'object' ? props.partida.jugadorActual.nombre : props.partida.jugadorActual
})

const turnoEsDeA = computed(() => {
  if (!nombreJugadorActual.value || !props.partida.equipoA) return false
  return props.partida.equipoA.some(j => j.nombre === nombreJugadorActual.value)
})

const turnoEsDeB = computed(() => {
  if (!nombreJugadorActual.value || !props.partida.equipoB) return false
  return props.partida.equipoB.some(j => j.nombre === nombreJugadorActual.value)
})

const puntosEquipoA = computed(() => props.partida.equipoA?.reduce((total, jugador) => total + (props.partida.puntajes?.[jugador.nombre] || 0), 0) || 0)
const puntosEquipoB = computed(() => props.partida.equipoB?.reduce((total, jugador) => total + (props.partida.puntajes?.[jugador.nombre] || 0), 0) || 0)

const nombreEquipoA = computed(() => props.partida.equipoA ? props.partida.equipoA.map(j => j.nombre).join(' & ') : '')
const nombreEquipoB = computed(() => props.partida.equipoB ? props.partida.equipoB.map(j => j.nombre).join(' & ') : '')
</script>

<template>
  <div class="jugadores">
    <div class="equipo equipo-a" :class="{ activo: turnoEsDeA }">
      <span class="nombre">{{ nombreEquipoA }}</span>
      <div class="bola-marcador bola-amarilla">
        <span class="puntos">{{ puntosEquipoA }}</span>
      </div>
    </div>
    
    <div class="vs">VS</div>
    
    <div class="equipo equipo-b" :class="{ activo: turnoEsDeB }">
      <span class="nombre">{{ nombreEquipoB }}</span>
      <div class="bola-marcador bola-azul">
        <span class="puntos">{{ puntosEquipoB }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.jugadores { display: flex; align-items: center; justify-content: space-around; gap: 1rem; }
.equipo { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 1.5rem; border-radius: 12px; border: 2px solid transparent; transition: all 0.3s ease; }
.equipo.activo { background: rgba(21, 128, 61, 0.2); border-color: var(--felt-light); box-shadow: 0 0 25px rgba(34, 197, 94, 0.4); transform: scale(1.02); }
.nombre { font-size: 1.4rem; font-weight: 800; color: #cbd5e1; margin-bottom: 1rem; text-transform: uppercase; text-align: center; }
.equipo.activo .nombre { color: var(--felt-light); }
.bola-marcador { width: 120px; height: 120px; border-radius: 50%; display: flex; justify-content: center; align-items: center; position: relative; box-shadow: inset -15px -15px 25px rgba(0,0,0,0.5), inset 10px 10px 15px rgba(255,255,255,0.6), 5px 5px 15px rgba(0,0,0,0.5); }
.bola-marcador::before { content: ''; position: absolute; width: 50px; height: 50px; background: white; border-radius: 50%; box-shadow: inset 2px 2px 5px rgba(0,0,0,0.3); }
.puntos { position: relative; z-index: 10; font-family: 'Bebas Neue', sans-serif; font-size: 2.5rem; color: black; font-weight: bold; }
.bola-amarilla { background: radial-gradient(circle at 30% 30%, #fef08a, var(--ball-yellow) 60%, #a16207 100%); }
.bola-azul { background: radial-gradient(circle at 30% 30%, #bfdbfe, var(--ball-blue) 60%, #1e3a8a 100%); }
.vs { font-family: 'Bebas Neue', sans-serif; font-size: 2rem; color: #475569; }
</style>