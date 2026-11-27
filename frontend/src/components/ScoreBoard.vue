<script setup>
import { computed } from 'vue'
import ScoreBoardEquipos from './ScoreBoardEquipos.vue'
import ScoreBoardTodosVsTodos from './ScoreBoardTodosVsTodos.vue'

const props = defineProps({
  partida: { type: Object, required: true },
  formato: { type: String, default: 'INDIVIDUAL' }
})

const mostrarTodosVsTodos = computed(() => {
  if (props.formato === 'PAREJAS') return false
  const cantA = props.partida.equipoA?.length || 0
  const cantB = props.partida.equipoB?.length || 0
  return (cantA + cantB) === 4
})

const esBola8 = computed(() => props.partida.modalidad === 'BOLA_8')
const faseFinalizada = computed(() => props.partida.fase === 'FINALIZADA')
const ganador = computed(() => props.partida.ganador)
const bolasEnMesa = computed(() => props.partida.bolasEnMesa || [])

const bolasEquipoA = computed(() => {
  if (!esBola8.value) return []
  const grupoA = props.partida.equipoA?.[0] ? props.partida.gruposAsignados?.[props.partida.equipoA[0].nombre] : null
  if (!grupoA || grupoA === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupoA === 'LISAS' ? 'LISA' : 'RAYADA'
  return bolasEnMesa.value.filter(n => {
    if (n === 0 || n === 8) return false
    return tipoBuscado === 'LISA' ? n <= 7 : n >= 9
  })
})

const bolasEquipoB = computed(() => {
  if (!esBola8.value) return []
  const grupoB = props.partida.equipoB?.[0] ? props.partida.gruposAsignados?.[props.partida.equipoB[0].nombre] : null
  if (!grupoB || grupoB === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupoB === 'LISAS' ? 'LISA' : 'RAYADA'
  return bolasEnMesa.value.filter(n => {
    if (n === 0 || n === 8) return false
    return tipoBuscado === 'LISA' ? n <= 7 : n >= 9
  })
})

const bolasEmbolsadasEquipoA = computed(() => {
  if (!esBola8.value) return []
  const grupoA = props.partida.equipoA?.[0] ? props.partida.gruposAsignados?.[props.partida.equipoA[0].nombre] : null
  if (!grupoA || grupoA === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupoA === 'LISAS' ? 'LISA' : 'RAYADA'
  const todasDelGrupo = tipoBuscado === 'LISA' ? [1,2,3,4,5,6,7] : [9,10,11,12,13,14,15]
  return todasDelGrupo.filter(n => !bolasEnMesa.value.includes(n))
})

const bolasEmbolsadasEquipoB = computed(() => {
  if (!esBola8.value) return []
  const grupoB = props.partida.equipoB?.[0] ? props.partida.gruposAsignados?.[props.partida.equipoB[0].nombre] : null
  if (!grupoB || grupoB === 'SIN_ASIGNAR') return []
  const tipoBuscado = grupoB === 'LISAS' ? 'LISA' : 'RAYADA'
  const todasDelGrupo = tipoBuscado === 'LISA' ? [1,2,3,4,5,6,7] : [9,10,11,12,13,14,15]
  return todasDelGrupo.filter(n => !bolasEnMesa.value.includes(n))
})
</script>

<template>
  <div class="scoreboard">
    <div class="header">
      <h2>{{ partida.modalidad === 'BOLA_8' ? 'BOLA 8' : 'POR PUNTOS' }}</h2>
      <span class="fase" :class="{ finalizada: faseFinalizada }">
        {{ partida.fase }}
      </span>
    </div>

    <ScoreBoardTodosVsTodos v-if="mostrarTodosVsTodos" :partida="partida" />
    <ScoreBoardEquipos v-else :partida="partida" :bolas-equipo-a="bolasEquipoA" :bolas-equipo-b="bolasEquipoB"
                       :bolas-embolsadas-a="bolasEmbolsadasEquipoA" :bolas-embolsadas-b="bolasEmbolsadasEquipoB"
                       :es-bola8="esBola8" :fase-finalizada="faseFinalizada" :ganador="ganador" />

    <div v-if="partida.fase === 'EN_CURSO'" class="turno">
      Turno de: <strong>{{ partida.jugadorActual }}</strong>
    </div>
    <div v-else-if="faseFinalizada && ganador" class="turno ganador">
      <span class="corona">👑</span> Ganador: <strong>{{ ganador }}</strong>
    </div>
    <div v-else class="turno">
      {{ partida.fase }}
    </div>
  </div>
</template>

<style scoped>
.scoreboard { background: #1e293b; border: 4px solid #0f172a; border-radius: 12px; padding: 2rem; color: white; max-width: 900px; margin: 0 auto; box-shadow: inset 0 0 30px rgba(0,0,0,0.8), 0 20px 40px rgba(0,0,0,0.6); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 1px solid #334155; padding-bottom: 1rem; }
h2 { margin: 0; font-size: 2.5rem; color: #e2e8f0; text-shadow: 2px 2px 0 #000; }
.fase { background: #334155; color: #94a3b8; padding: 0.4rem 1rem; border-radius: 4px; font-family: monospace; font-weight: bold; }
.fase.finalizada { background: #ef4444; color: white; }
.turno { text-align: center; margin-top: 2rem; font-size: 1.2rem; color: #94a3b8; padding: 1rem; background: #0f172a; border-radius: 8px; border: 1px solid #334155; }
.turno strong { color: #eab308; font-size: 1.4rem; }
.turno.ganador { color: #eab308; }
.corona { display: inline-block; animation: corona-brillo 2s ease-in-out infinite; margin-right: 0.5rem; font-size: 1.5rem; }
@keyframes corona-brillo {
  0%, 100% { filter: drop-shadow(0 0 2px #fbbf24) drop-shadow(0 0 4px #f59e0b); transform: scale(1); }
  50% { filter: drop-shadow(0 0 8px #fbbf24) drop-shadow(0 0 12px #f59e0b); transform: scale(1.1); }
}
</style>