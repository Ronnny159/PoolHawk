<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

// Verificamos si el jugador actual pertenece al Equipo A o B
const turnoEsDeA = computed(() => {
  if (!props.partida.jugadorActual || !props.partida.equipoA) return false
  return props.partida.equipoA.some(j => j.nombre === props.partida.jugadorActual)
})

const turnoEsDeB = computed(() => {
  if (!props.partida.jugadorActual || !props.partida.equipoB) return false
  return props.partida.equipoB.some(j => j.nombre === props.partida.jugadorActual)
})

// Calculamos los puntos sumando a los jugadores de cada equipo
const puntosEquipoA = computed(() => {
  if (!props.partida.equipoA || !props.partida.puntajes) return 0
  return props.partida.equipoA.reduce((total, jugador) => {
    return total + (props.partida.puntajes[jugador.nombre] || 0)
  }, 0)
})

const puntosEquipoB = computed(() => {
  if (!props.partida.equipoB || !props.partida.puntajes) return 0
  return props.partida.equipoB.reduce((total, jugador) => {
    return total + (props.partida.puntajes[jugador.nombre] || 0)
  }, 0)
})

// Nombres de los equipos
const nombreEquipoA = computed(() => {
  return props.partida.equipoA ? props.partida.equipoA.map(j => j.nombre).join(' & ') : ''
})

const nombreEquipoB = computed(() => {
  return props.partida.equipoB ? props.partida.equipoB.map(j => j.nombre).join(' & ') : ''
})

const ganadorNombre = computed(() => {
  if (props.partida.fase !== 'FINALIZADA') return ''
  return props.partida.ganador || 'Desconocido'
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
      🏆 Ganador: <strong>{{ ganadorNombre }}</strong>
    </div>
  </div>
</template>

<style scoped>
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
.fase.finalizada { background: var(--ball-red); color: white; }

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
  border-color: var(--felt-light);
  box-shadow: 0 0 25px rgba(34, 197, 94, 0.4);
  transform: scale(1.02);
}

.nombre { font-size: 1.4rem; font-weight: 800; color: #cbd5e1; margin-bottom: 1rem; text-transform: uppercase; text-align: center;}
.equipo.activo .nombre { color: var(--felt-light); }

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

.equipo-a .bola-marcador { background: radial-gradient(circle at 30% 30%, #fef08a, var(--ball-yellow) 60%, #a16207 100%); }
.equipo-b .bola-marcador { background: radial-gradient(circle at 30% 30%, #bfdbfe, var(--ball-blue) 60%, #1e3a8a 100%); }

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

.turno strong { color: var(--ball-yellow); font-size: 1.4rem; }
.turno.ganador { color: var(--ball-yellow); }
</style>