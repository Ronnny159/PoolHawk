<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

// LOG DE DIAGNÓSTICO
console.log('📊 ScoreBoard se está redibujando. Puntajes actuales:', props.partida.puntajes)

// 1. Calculamos de quién es el turno
const turnoEsDeA = computed(() => {
  return props.partida.equipoA.some(j => j.nombre === props.partida.jugadorActual)
})

const turnoEsDeB = computed(() => {
  return props.partida.equipoB.some(j => j.nombre === props.partida.jugadorActual)
})

// 2. Calculamos los puntos
const puntosEquipoA = computed(() => {
  let total = 0
  const puntajes = props.partida.puntajes || {}
  for (const jugador of props.partida.equipoA) {
    total += puntajes[jugador.nombre] || 0
  }
  return total
})

const puntosEquipoB = computed(() => {
  let total = 0
  const puntajes = props.partida.puntajes || {}
  for (const jugador of props.partida.equipoB) {
    total += puntajes[jugador.nombre] || 0
  }
  return total
})

// 3. Calculamos el nombre del equipo
const nombreEquipoA = computed(() => {
  return props.partida.equipoA.map(j => j.nombre).join(' & ')
})

const nombreEquipoB = computed(() => {
  return props.partida.equipoB.map(j => j.nombre).join(' & ')
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

    <div class="jugadores">
      <!-- EQUIPO A -->
      <div class="equipo equipo-a" :class="{ activo: turnoEsDeA }">
        <span class="nombre">{{ nombreEquipoA }}</span>
        <div class="bola-marcador">
          <span class="puntos">{{ puntosEquipoA }}</span>
        </div>
      </div>
      
      <div class="vs">VS</div>
      
      <!-- EQUIPO B -->
      <div class="equipo equipo-b" :class="{ activo: turnoEsDeB }">
        <span class="nombre">{{ nombreEquipoB }}</span>
        <div class="bola-marcador">
          <span class="puntos">{{ puntosEquipoB }}</span>
        </div>
      </div>
    </div>

    <div v-if="partida.fase === 'EN_CURSO'" class="turno">
      Turno de: <strong>{{ partida.jugadorActual }}</strong>
    </div>
    <div v-else class="turno ganador">
      🏆 Ganador: <strong>{{ partida.equipoA.some(j => j.nombre === partida.ganador) ? nombreEquipoA : nombreEquipoB }}</strong>
    </div>
  </div>
</template>

<style scoped>
/* Tus estilos del marcador (los de las bolas de billar y el paño) se mantienen igual */
.scoreboard { 
  background: #1e293b;
  border: 4px solid #0f172a;
  border-radius: 12px; 
  padding: 2rem; 
  color: white; 
  max-width: 850px; 
  margin: 0 auto; 
  box-shadow: inset 0 0 30px rgba(0,0,0,0.8), 0 20px 40px rgba(0,0,0,0.6);
}

.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 1px solid #334155; padding-bottom: 1rem; }
h2 { margin: 0; font-size: 2.5rem; color: #e2e8f0; text-shadow: 2px 2px 0 #000; }

.fase { background: #334155; color: #94a3b8; padding: 0.4rem 1rem; border-radius: 4px; font-family: monospace; font-weight: bold; }
.fase.finalizada { background: #ef4444; color: white; }

.jugadores { display: flex; align-items: center; justify-content: space-around; gap: 1rem; }

.equipo { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  align-items: center;
  padding: 1.5rem; 
  border-radius: 12px; 
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.equipo.activo { 
  background: rgba(21, 128, 61, 0.2);
  border-color: #22c55e;
  box-shadow: 0 0 25px rgba(34, 197, 94, 0.4);
  transform: scale(1.02);
}

.nombre { font-size: 1.4rem; font-weight: 800; color: #cbd5e1; margin-bottom: 1rem; text-transform: uppercase;}
.equipo.activo .nombre { color: #4ade80; }

.bola-marcador {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  box-shadow: 
    inset -15px -15px 25px rgba(0,0,0,0.5),
    inset 10px 10px 15px rgba(255,255,255,0.6),
    5px 5px 15px rgba(0,0,0,0.5);
}

.bola-marcador::before {
  content: '';
  position: absolute;
  width: 50px;
  height: 50px;
  background: white;
  border-radius: 50%;
  box-shadow: inset 2px 2px 5px rgba(0,0,0,0.3);
}

.puntos {
  position: relative;
  z-index: 10;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 2.5rem;
  color: black;
  font-weight: bold;
}

.equipo-a .bola-marcador { background: radial-gradient(circle at 30% 30%, #fef08a, #eab308 60%, #a16207 100%); }
.equipo-b .bola-marcador { background: radial-gradient(circle at 30% 30%, #bfdbfe, #3b82f6 60%, #1e3a8a 100%); }

.vs { font-family: 'Bebas Neue', sans-serif; font-size: 2rem; color: #475569; }

.turno { 
  text-align: center; 
  margin-top: 2rem; 
  font-size: 1.2rem; 
  color: #94a3b8; 
  padding: 1rem;
  background: #0f172a;
  border-radius: 8px;
  border: 1px solid #334155;
}

.turno strong { color: #eab308; font-size: 1.4rem; }
.turno.ganador { color: #eab308; }
</style>