<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true },
  esBola8: { type: Boolean, default: false },
  faseFinalizada: { type: Boolean, default: false },
  ganador: { type: String, default: '' }
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

const esGanador = (jugador) => props.faseFinalizada && props.ganador && jugador.nombre === props.ganador
</script>

<template>
  <div class="grilla-4-jugadores">
    <div 
      v-for="(jugador, index) in todosLosJugadores" 
      :key="jugador.nombre"
      class="jugador-individual"
      :class="{ activo: jugador.nombre === nombreJugadorActual, ganador: esGanador(jugador) }"
    >
      <span class="nombre">{{ jugador.nombre }}{{ tipoBolas(jugador) }}</span>
      <div class="bola-marcador" :style="{ background: coloresBolas[index % coloresBolas.length] }">
        <span class="puntos">{{ props.partida.puntajes?.[jugador.nombre] || 0 }}</span>
        <span v-if="esGanador(jugador)" class="corona-pequena">👑</span>
      </div>
      
      <!-- Bolas restantes/embolsadas para BOLA_8 en modo 4 jugadores -->
      <div v-if="props.esBola8" class="bolas-info">
        <div class="bolas-restantes">
          <span class="etiqueta">Restantes:</span>
          <span class="bolas-lista" v-for="n in bolasRestantes(jugador)" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasRestantes(jugador).length === 0" class="vacio">—</span>
        </div>
        <div class="bolas-embolsadas">
          <span class="etiqueta">Embolsadas:</span>
          <span class="bolas-lista embolsada" v-for="n in bolasEmbolsadas(jugador)" :key="n" :class="colorBola(n)">{{ n }}</span>
          <span v-if="bolasEmbolsadas(jugador).length === 0" class="vacio">—</span>
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

function tipoBolas(jugador) {
  if (!props.esBola8) return ''
  const grupo = props.partida.gruposAsignados?.[jugador.nombre]
  return grupo === 'LISAS' ? ' (Lisas)' : grupo === 'RAYADAS' ? ' (Rayadas)' : ''
}

function bolasRestantes(jugador) {
  if (!props.esBola8) return []
  const grupo = props.partida.gruposAsignados?.[jugador.nombre]
  if (!grupo || grupo === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupo === 'LISAS' ? 'LISA' : 'RAYADA'
  const bolasEnMesa = props.partida.bolasEnMesa || []
  return (tipoBuscado === 'LISA' ? [1,2,3,4,5,6,7] : [9,10,11,12,13,14,15])
    .filter(n => bolasEnMesa.includes(n))
}

function bolasEmbolsadas(jugador) {
  if (!props.esBola8) return []
  const grupo = props.partida.gruposAsignados?.[jugador.nombre]
  if (!grupo || grupo === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupo === 'LISAS' ? 'LISA' : 'RAYADA'
  const bolasEnMesa = props.partida.bolasEnMesa || []
  return (tipoBuscado === 'LISA' ? [1,2,3,4,5,6,7] : [9,10,11,12,13,14,15])
    .filter(n => !bolasEnMesa.includes(n))
}
</script>

<style scoped>
.grilla-4-jugadores { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
.jugador-individual { display: flex; flex-direction: column; align-items: center; padding: 1rem; border-radius: 12px; border: 2px solid transparent; transition: all 0.3s ease; }
.jugador-individual.activo { background: rgba(21, 128, 61, 0.2); border-color: var(--felt-light); box-shadow: 0 0 25px rgba(34, 197, 94, 0.4); transform: scale(1.02); }
.jugador-individual.ganador { background: rgba(251, 191, 36, 0.15); border-color: #fbbf24; box-shadow: 0 0 30px rgba(251, 191, 36, 0.4); }
.nombre { font-size: 1.2rem; font-weight: 800; color: #cbd5e1; margin-bottom: 0.8rem; text-transform: uppercase; text-align: center; }
.jugador-individual.activo .nombre { color: var(--felt-light); }
.jugador-individual.ganador .nombre { color: #fbbf24; }
.bola-marcador { width: 100px; height: 100px; border-radius: 50%; display: flex; flex-direction: column; justify-content: center; align-items: center; position: relative; box-shadow: inset -15px -15px 25px rgba(0,0,0,0.5), inset 10px 10px 15px rgba(255,255,255,0.6), 5px 5px 15px rgba(0,0,0,0.5); }
.bola-marcador::before { content: ''; position: absolute; width: 45px; height: 45px; background: white; border-radius: 50%; box-shadow: inset 2px 2px 5px rgba(0,0,0,0.3); }
.puntos { position: relative; z-index: 10; font-family: 'Bebas Neue', sans-serif; font-size: 2.2rem; color: black; font-weight: bold; }
.corona-pequena { position: relative; z-index: 10; font-size: 1rem; margin-top: -0.5rem; animation: corona-brillo 2s ease-in-out infinite; }
@keyframes corona-brillo {
  0%, 100% { filter: drop-shadow(0 0 2px #fbbf24) drop-shadow(0 0 4px #f59e0b); transform: scale(1); }
  50% { filter: drop-shadow(0 0 8px #fbbf24) drop-shadow(0 0 12px #f59e0b); transform: scale(1.1); }
}

/* Bolas info */
.bolas-info { margin-top: 0.8rem; width: 100%; display: flex; flex-direction: column; gap: 0.4rem; align-items: center; font-size: 0.8rem; }
.bolas-restantes, .bolas-embolsadas { display: flex; align-items: center; gap: 0.3rem; flex-wrap: wrap; justify-content: center; }
.etiqueta { color: #94a3b8; font-size: 0.7rem; text-transform: uppercase; font-weight: 600; min-width: 55px; }
.bolas-lista { display: inline-flex; width: 1.1rem; height: 1.1rem; border-radius: 50%; justify-content: center; align-items: center; font-size: 0.65rem; font-weight: bold; color: black; box-shadow: inset -2px -2px 4px rgba(0,0,0,0.4), inset 1px 1px 2px rgba(255,255,255,0.5); }
.bolas-lista.embolsada { opacity: 0.5; filter: grayscale(50%); }
.bolas-lista.lisa { background: radial-gradient(circle at 30% 30%, #fff9c4, #eab308 60%, #a16207 100%); }
.bolas-lista.rayada { background: radial-gradient(circle at 30% 30%, #fff, #ccc 40%, #999 100%); background-image: repeating-linear-gradient(45deg, transparent, transparent 3px, rgba(0,0,0,0.15) 3px, rgba(0,0,0,0.15) 6px), radial-gradient(circle at 30% 30%, #fff, #ccc 40%, #999 100%); }
.vacio { color: #64748b; font-size: 0.7rem; }
</style>