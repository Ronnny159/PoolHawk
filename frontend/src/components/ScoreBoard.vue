<script setup>
import { computed } from 'vue'
import ScoreBoardEquipos from './ScoreBoardEquipos.vue'
import ScoreBoardTodosVsTodos from './ScoreBoardTodosVsTodos.vue'

const props = defineProps({
  partida: { type: Object, required: true },
  formato: { type: String, default: 'INDIVIDUAL' }
})

const mostrarTodosVsTodos = computed(() => {
  // Si es PAREJAS, NUNCA será Todos vs Todos
  if (props.formato === 'PAREJAS') return false
  
  // Si es INDIVIDUAL y hay 4 jugadores, es Todos vs Todos
  const cantA = props.partida.equipoA?.length || 0
  const cantB = props.partida.equipoB?.length || 0
  return (cantA + cantB) === 4
})
</script>

<template>
  <div class="scoreboard">
    <div class="header">
      <h2>{{ partida.modalidad === 'BOLA_8' ? 'BOLA 8' : 'POR PUNTOS' }}</h2>
      <span class="fase" :class="{ finalizada: partida.fase === 'FINALIZADA' }">
        {{ partida.fase }}
      </span>
    </div>

    <ScoreBoardTodosVsTodos v-if="mostrarTodosVsTodos" :partida="partida" />
    <ScoreBoardEquipos v-else :partida="partida" />

    <div v-if="partida.fase === 'EN_CURSO'" class="turno">
      Turno de: <strong>{{ partida.jugadorActual }}</strong>
    </div>
    <div v-else class="turno ganador">
      🏆 Ganador: <strong>{{ partida.ganador }}</strong>
    </div>
  </div>
</template>

<style scoped>
/* ... (Mismos estilos) ... */
.scoreboard { background: #1e293b; border: 4px solid #0f172a; border-radius: 12px; padding: 2rem; color: white; max-width: 900px; margin: 0 auto; box-shadow: inset 0 0 30px rgba(0,0,0,0.8), 0 20px 40px rgba(0,0,0,0.6); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 1px solid #334155; padding-bottom: 1rem; }
h2 { margin: 0; font-size: 2.5rem; color: #e2e8f0; text-shadow: 2px 2px 0 #000; }
.fase { background: #334155; color: #94a3b8; padding: 0.4rem 1rem; border-radius: 4px; font-family: monospace; font-weight: bold; }
.fase.finalizada { background: #ef4444; color: white; }
.turno { text-align: center; margin-top: 2rem; font-size: 1.2rem; color: #94a3b8; padding: 1rem; background: #0f172a; border-radius: 8px; border: 1px solid #334155; }
.turno strong { color: #eab308; font-size: 1.4rem; }
.turno.ganador { color: #eab308; }
</style>