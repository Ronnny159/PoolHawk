<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true },
  bolasEquipoA: { type: Array, default: () => [] },
  bolasEquipoB: { type: Array, default: () => [] },
  bolasEmbolsadasA: { type: Array, default: () => [] },
  bolasEmbolsadasB: { type: Array, default: () => [] },
  esBola8: { type: Boolean, default: false },
  faseFinalizada: { type: Boolean, default: false },
  ganador: { type: String, default: '' }
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

// Tipo de bolas (Lisas/Rayadas) para mostrar junto al nombre
const tipoBolasA = computed(() => {
  if (!props.esBola8) return ''
  const j = props.partida.equipoA?.[0]
  if (!j) return ''
  const grupo = props.partida.gruposAsignados?.[j.nombre]
  return grupo === 'LISAS' ? ' (Lisas)' : grupo === 'RAYADAS' ? ' (Rayadas)' : ''
})
const tipoBolasB = computed(() => {
  if (!props.esBola8) return ''
  const j = props.partida.equipoB?.[0]
  if (!j) return ''
  const grupo = props.partida.gruposAsignados?.[j.nombre]
  return grupo === 'LISAS' ? ' (Lisas)' : grupo === 'RAYADAS' ? ' (Rayadas)' : ''
})

const esGanadorA = computed(() => props.faseFinalizada && props.ganador && props.partida.equipoA?.some(j => j.nombre === props.ganador))
const esGanadorB = computed(() => props.faseFinalizada && props.ganador && props.partida.equipoB?.some(j => j.nombre === props.ganador))
</script>

<template>
  <div class="jugadores">
    <div class="equipo equipo-a" :class="{ activo: turnoEsDeA, ganador: esGanadorA }">
      <span class="nombre">{{ nombreEquipoA }}{{ tipoBolasA }}</span>
      <div class="bola-marcador bola-amarilla">
        <span class="puntos">{{ puntosEquipoA }}</span>
        <span v-if="esGanadorA" class="corona-pequena">👑</span>
      </div>
      
      <!-- Bolas restantes y embolsadas (solo BOLA_8) -->
      <div v-if="props.esBola8" class="bolas-info">
        <div class="bolas-restantes">
          <span class="etiqueta">Restantes:</span>
          <span class="bolas-lista" v-for="n in bolasEquipoA" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasEquipoA.length === 0" class="vacio">—</span>
        </div>
        <div class="bolas-embolsadas">
          <span class="etiqueta">Embolsadas:</span>
          <span class="bolas-lista embolsada" v-for="n in bolasEmbolsadasA" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasEmbolsadasA.length === 0" class="vacio">—</span>
        </div>
      </div>
    </div>
    
    <div class="vs">VS</div>
    
    <div class="equipo equipo-b" :class="{ activo: turnoEsDeB, ganador: esGanadorB }">
      <span class="nombre">{{ nombreEquipoB }}{{ tipoBolasB }}</span>
      <div class="bola-marcador bola-azul">
        <span class="puntos">{{ puntosEquipoB }}</span>
        <span v-if="esGanadorB" class="corona-pequena">👑</span>
      </div>
      
      <div v-if="props.esBola8" class="bolas-info">
        <div class="bolas-restantes">
          <span class="etiqueta">Restantes:</span>
          <span class="bolas-lista" v-for="n in bolasEquipoB" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasEquipoB.length === 0" class="vacio">—</span>
        </div>
        <div class="bolas-embolsadas">
          <span class="etiqueta">Embolsadas:</span>
          <span class="bolas-lista embolsada" v-for="n in bolasEmbolsadasB" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasEmbolsadasB.length === 0" class="vacio">—</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
function colorBola(n) {
  if (n >= 1 && n <= 7) return 'lisa'
  if (n >= 9 && n <= 15) return 'rayada'
  return ''
}
</script>

<style scoped>
.jugadores { display: flex; align-items: center; justify-content: space-around; gap: 1rem; }
.equipo { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 1.5rem; border-radius: 12px; border: 2px solid transparent; transition: all 0.3s ease; }
.equipo.activo { background: rgba(21, 128, 61, 0.2); border-color: var(--felt-light); box-shadow: 0 0 25px rgba(34, 197, 94, 0.4); transform: scale(1.02); }
.equipo.ganador { background: rgba(251, 191, 36, 0.15); border-color: #fbbf24; box-shadow: 0 0 30px rgba(251, 191, 36, 0.4); }
.nombre { font-size: 1.3rem; font-weight: 800; color: #cbd5e1; margin-bottom: 0.8rem; text-transform: uppercase; text-align: center; }
.equipo.activo .nombre { color: var(--felt-light); }
.equipo.ganador .nombre { color: #fbbf24; }
.bola-marcador { width: 120px; height: 120px; border-radius: 50%; display: flex; flex-direction: column; justify-content: center; align-items: center; position: relative; box-shadow: inset -15px -15px 25px rgba(0,0,0,0.5), inset 10px 10px 15px rgba(255,255,255,0.6), 5px 5px 15px rgba(0,0,0,0.5); }
.bola-marcador::before { content: ''; position: absolute; width: 50px; height: 50px; background: white; border-radius: 50%; box-shadow: inset 2px 2px 5px rgba(0,0,0,0.3); }
.puntos { position: relative; z-index: 10; font-family: 'Bebas Neue', sans-serif; font-size: 2.5rem; color: black; font-weight: bold; }
.corona-pequena { position: relative; z-index: 10; font-size: 1.2rem; margin-top: -0.5rem; animation: corona-brillo 2s ease-in-out infinite; }
.bola-amarilla { background: radial-gradient(circle at 30% 30%, #fef08a, var(--ball-yellow) 60%, #a16207 100%); }
.bola-azul { background: radial-gradient(circle at 30% 30%, #bfdbfe, var(--ball-blue) 60%, #1e3a8a 100%); }
.vs { font-family: 'Bebas Neue', sans-serif; font-size: 2rem; color: #475569; }

/* Bolas info */
.bolas-info { margin-top: 1rem; width: 100%; display: flex; flex-direction: column; gap: 0.5rem; align-items: center; font-size: 0.85rem; }
.bolas-restantes, .bolas-embolsadas { display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap; justify-content: center; }
.etiqueta { color: #94a3b8; font-size: 0.75rem; text-transform: uppercase; font-weight: 600; min-width: 65px; }
.bolas-lista { display: inline-flex; width: 1.3rem; height: 1.3rem; border-radius: 50%; justify-content: center; align-items: center; font-size: 0.7rem; font-weight: bold; color: black; box-shadow: inset -2px -2px 4px rgba(0,0,0,0.4), inset 1px 1px 2px rgba(255,255,255,0.5); }
.bolas-lista.embolsada { opacity: 0.5; filter: grayscale(50%); }
.bolas-lista.lisa { background: radial-gradient(circle at 30% 30%, #fff9c4, #eab308 60%, #a16207 100%); }
.bolas-lista.rayada { background: radial-gradient(circle at 30% 30%, #fff, #ccc 40%, #999 100%); background-image: repeating-linear-gradient(45deg, transparent, transparent 3px, rgba(0,0,0,0.15) 3px, rgba(0,0,0,0.15) 6px), radial-gradient(circle at 30% 30%, #fff, #ccc 40%, #999 100%); }
.vacio { color: #64748b; font-size: 0.75rem; }
</style>