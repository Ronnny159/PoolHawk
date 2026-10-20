<script setup>
import { computed } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

// 1. Nombres de los equipos (unidos por si es pareja)
const nombreEquipoA = computed(() => props.partida.equipoA.map(j => j.nombre.trim()).join(' / '))
const nombreEquipoB = computed(() => props.partida.equipoB.map(j => j.nombre.trim()).join(' / '))

// 2. Función de suma total (Blindada contra espacios y nulos)
const calcularPuntosEquipo = (equipo) => {
  if (!props.partida.puntajes) return 0;
  
  return equipo.reduce((total, jugador) => {
    // Limpiamos el nombre para asegurar que haga match exacto con la llave del JSON
    const nombreLimpio = jugador.nombre.trim();
    // Buscamos en el mapa de puntajes. Si no existe, sumamos 0.
    const puntosJugador = props.partida.puntajes[nombreLimpio] || 0;
    return total + puntosJugador;
  }, 0);
}

// 3. Puntos computados para el template
const puntosEquipoA = computed(() => calcularPuntosEquipo(props.partida.equipoA))
const puntosEquipoB = computed(() => calcularPuntosEquipo(props.partida.equipoB))

// 4. Resaltado de turno (Blindado contra espacios)
const turnoEsDeA = computed(() => {
  if (!props.partida.jugadorActual) return false;
  const actualLimpio = props.partida.jugadorActual.trim();
  return props.partida.equipoA.some(j => j.nombre.trim() === actualLimpio);
})

const turnoEsDeB = computed(() => {
  if (!props.partida.jugadorActual) return false;
  const actualLimpio = props.partida.jugadorActual.trim();
  return props.partida.equipoB.some(j => j.nombre.trim() === actualLimpio);
})
</script>

<template>
  <div class="table-frame">
    <!-- TRONERAS (Esquinas) -->
    <div class="pocket top-left"></div>
    <div class="pocket top-mid"></div>
    <div class="pocket top-right"></div>
    <div class="pocket bottom-left"></div>
    <div class="pocket bottom-mid"></div>
    <div class="pocket bottom-right"></div>

    <!-- PAÑO VERDE -->
    <div class="felt-container">
      
      <!-- BARRA SUPERIOR -->
      <div class="top-bar">
        <h2 class="title">{{ partida.modalidad === 'BOLA_8' ? '8-BALL' : 'BY POINTS' }}</h2>
        
        <div class="live-badge" :class="{ 'finished': partida.fase === 'FINALIZADA' }">
          <span class="dot"></span> 
          {{ partida.fase === 'EN_CURSO' ? 'LIVE' : 'FINISHED' }}
        </div>
      </div>

      <!-- TABLERO PRINCIPAL -->
      <div class="board-content">
        
        <!-- EQUIPO A (Izquierda) -->
        <div class="team-side">
          <div class="ball-container" :class="{ 'glow-active': turnoEsDeA && partida.fase === 'EN_CURSO' }">
            <div class="ball ball-yellow">
              <div class="ball-inner">
                <span class="score">{{ puntosEquipoA }}</span>
              </div>
            </div>
          </div>
          <span class="team-name">{{ nombreEquipoA }}</span>
        </div>

        <!-- CENTRO: MINI MESA -->
        <div class="center-area">
          <div class="mini-table">
            <!-- Troneras de la mini mesa -->
            <div class="m-pocket tl"></div>
            <div class="m-pocket tm"></div>
            <div class="m-pocket tr"></div>
            <div class="m-pocket bl"></div>
            <div class="m-pocket bm"></div>
            <div class="m-pocket br"></div>

            <!-- Contenido según modalidad -->
            <div class="table-content">
              <!-- MODO BOLA 8: Racimo de bolas -->
              <div v-if="partida.modalidad === 'BOLA_8'" class="rack-view">
                <div class="cue-ball"></div>
                <div class="rack">
                  <div class="row"><span class="dot b1"></span></div>
                  <div class="row"><span class="dot b2"></span><span class="dot b8"></span></div>
                  <div class="row"><span class="dot b3"></span><span class="dot b4"></span><span class="dot b5"></span></div>
                  <div class="row"><span class="dot b6"></span><span class="dot b7"></span><span class="dot b9"></span><span class="dot b10"></span></div>
                  <div class="row"><span class="dot b11"></span><span class="dot b12"></span><span class="dot b13"></span><span class="dot b14"></span><span class="dot b15"></span></div>
                </div>
              </div>

              <!-- MODO POR PUNTOS: Barras de progreso -->
              <div v-else class="points-view">
                <div class="progress-group">
                  <span class="p-label">TEAM A — {{ puntosEquipoA }} / 50</span>
                  <div class="progress-track">
                    <div class="progress-fill fill-a" :style="{ width: Math.min((puntosEquipoA / 50) * 100, 100) + '%' }"></div>
                  </div>
                </div>
                <div class="progress-group">
                  <span class="p-label">TEAM B — {{ puntosEquipoB }} / 50</span>
                  <div class="progress-track">
                    <div class="progress-fill fill-b" :style="{ width: Math.min((puntosEquipoB / 50) * 100, 100) + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- INDICADOR DE TURNO / GANADOR -->
          <div class="turn-indicator">
            <div class="mini-cue-ball"></div>
            <span v-if="partida.fase === 'EN_CURSO'">Turn: <strong>{{ partida.jugadorActual }}</strong></span>
            <span v-else>🏆 Ganador: <strong>{{ partida.equipoA.some(j => j.nombre === partida.ganador) ? nombreEquipoA : nombreEquipoB }}</strong></span>
          </div>
        </div>

        <!-- EQUIPO B (Derecha) -->
        <div class="team-side">
          <div class="ball-container" :class="{ 'glow-active': turnoEsDeB && partida.fase === 'EN_CURSO' }">
            <div class="ball ball-blue">
              <div class="ball-inner">
                <span class="score">{{ puntosEquipoB }}</span>
              </div>
            </div>
          </div>
          <span class="team-name">{{ nombreEquipoB }}</span>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   1. MARCO Y PAÑO
   ============================================================ */
.table-frame {
  max-width: 950px;
  margin: 0 auto;
  position: relative;
  padding: 25px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2a1106 0%, #4a220d 50%, #2a1106 100%);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.9), inset 0 0 20px rgba(0, 0, 0, 0.8);
  border: 2px solid #1a0a02;
}

.felt-container {
  position: relative;
  background: radial-gradient(circle at center, #106932 0%, #06421a 80%, #022d0d 100%);
  border-radius: 8px;
  padding: 1.5rem 2rem 2.5rem;
  box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.7);
  border: 1px solid #022d0d;
}

/* Troneras principales */
.pocket { position: absolute; width: 42px; height: 42px; background: radial-gradient(circle, #000 50%, #0a0a0a 100%); border-radius: 50%; box-shadow: inset 0 8px 10px rgba(0,0,0,1), 0 2px 5px rgba(0,0,0,0.8); z-index: 10; border: 2px solid rgba(0,0,0,0.8); }
.pocket.top-left { top: 8px; left: 8px; }
.pocket.top-right { top: 8px; right: 8px; }
.pocket.bottom-left { bottom: 8px; left: 8px; }
.pocket.bottom-right { bottom: 8px; right: 8px; }
.pocket.top-mid { top: -18px; left: 50%; transform: translateX(-50%); width: 48px; height: 36px; border-radius: 50%; }
.pocket.bottom-mid { bottom: -18px; left: 50%; transform: translateX(-50%); width: 48px; height: 36px; border-radius: 50%; }

/* ============================================================
   2. BARRA SUPERIOR
   ============================================================ */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 0 1rem;
}

.title {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 2.5rem;
  color: white;
  margin: 0;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0,0,0,0.4);
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid rgba(34, 197, 94, 0.4);
  color: #4ade80;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 0.85rem;
  letter-spacing: 1px;
}

.live-badge.finished {
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
}

.dot {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 50%;
  box-shadow: 0 0 8px currentColor;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}

/* ============================================================
   3. TABLERO PRINCIPAL Y BOLAS LATERALES
   ============================================================ */
.board-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
}

.team-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.ball-container {
  position: relative;
  padding: 10px;
  border-radius: 50%;
  transition: all 0.4s ease;
}

/* Efecto de iluminación para el turno activo */
.ball-container.glow-active {
  box-shadow: 0 0 35px 5px rgba(34, 197, 94, 0.6), inset 0 0 15px rgba(34,197,94,0.4);
  border: 2px solid rgba(34, 197, 94, 0.8);
}

/* Diseño de la bola 3D */
.ball {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: inset -15px -15px 30px rgba(0,0,0,0.6), inset 10px 10px 20px rgba(255,255,255,0.4), 0 15px 25px rgba(0,0,0,0.5);
}

.ball-yellow { background: radial-gradient(circle at 30% 30%, #fef08a, #facc15 50%, #ca8a04 100%); }
.ball-blue { background: radial-gradient(circle at 30% 30%, #bfdbfe, #3b82f6 50%, #1d4ed8 100%); }

.ball-inner {
  width: 70px;
  height: 70px;
  background: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: inset 2px 2px 5px rgba(0,0,0,0.2);
}

.score {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 2.8rem;
  color: #000;
  margin-top: 4px;
}

.team-name {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.8rem;
  color: white;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.6);
}

/* ============================================================
   4. CENTRO: MINI MESA Y TURNO
   ============================================================ */
.center-area {
  flex: 1.5;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.mini-table {
  width: 100%;
  max-width: 380px;
  height: 180px;
  background: #06421a;
  border: 8px solid #4a220d;
  border-radius: 8px;
  position: relative;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.8), 0 10px 20px rgba(0,0,0,0.6);
}

/* Troneras mini mesa */
.m-pocket {
  position: absolute;
  width: 16px;
  height: 16px;
  background: #000;
  border-radius: 50%;
  box-shadow: inset 0 2px 4px #000;
}
.m-pocket.tl { top: -8px; left: -8px; }
.m-pocket.tm { top: -8px; left: 50%; transform: translateX(-50%); }
.m-pocket.tr { top: -8px; right: -8px; }
.m-pocket.bl { bottom: -8px; left: -8px; }
.m-pocket.bm { bottom: -8px; left: 50%; transform: translateX(-50%); }
.m-pocket.br { bottom: -8px; right: -8px; }

.table-content {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
}

/* MODO BOLA 8: Racimo */
.rack-view {
  position: relative;
  width: 100%;
  height: 100%;
}

.cue-ball {
  position: absolute;
  right: 20%;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.rack {
  position: absolute;
  left: 15%;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.row {
  display: flex;
  gap: 1px;
  justify-content: center;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: inset -1px -1px 2px rgba(0,0,0,0.5), 1px 1px 2px rgba(0,0,0,0.6);
}
/* Colores de bolas simplificados */
.b1 { background: #facc15; } .b2 { background: #3b82f6; } .b3 { background: #ef4444; }
.b4 { background: #a855f7; } .b5 { background: #f97316; } .b6 { background: #22c55e; }
.b7 { background: #991b1b; } .b8 { background: #000; } .b9 { background: #fde047; }
.b10 { background: #1e3a8a; } .b11 { background: #b91c1c; } .b12 { background: #7e22ce; }
.b13 { background: #c2410c; } .b14 { background: #15803d; } .b15 { background: #450a0a; }


/* MODO POR PUNTOS: Barras de progreso */
.points-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 0 15px;
}

.progress-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.p-label {
  font-family: 'Bebas Neue', sans-serif;
  color: #f8fafc;
  font-size: 1.1rem;
  letter-spacing: 1px;
}

.progress-track {
  width: 100%;
  height: 18px;
  background: rgba(0,0,0,0.4);
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.2);
  overflow: hidden;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.5);
  position: relative;
}

.progress-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.4s ease;
  /* Patrón de bolitas usando gradientes */
  background-image: radial-gradient(circle at 6px 50%, rgba(255,255,255,0.4) 2px, transparent 3px);
  background-size: 12px 100%;
}

.fill-a { background-color: #facc15; }
.fill-b { background-color: #3b82f6; }


/* Turno */
.turn-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(0,0,0,0.4);
  padding: 6px 16px;
  border-radius: 20px;
  color: #cbd5e1;
  font-family: 'Inter', sans-serif;
  font-size: 0.9rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.3);
}

.turn-indicator strong {
  color: #fff;
}

.mini-cue-ball {
  width: 12px;
  height: 12px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 5px rgba(255,255,255,0.8);
}

/* ============================================================
   5. RESPONSIVE
   ============================================================ */
@media (max-width: 800px) {
  .board-content {
    flex-direction: column;
    gap: 1rem;
  }
  .team-side {
    flex-direction: row;
    width: 100%;
    justify-content: space-around;
  }
  .ball { width: 100px; height: 100px; }
  .ball-inner { width: 50px; height: 50px; }
  .score { font-size: 1.8rem; }
  .team-name { font-size: 1.4rem; }
  .center-area { width: 100%; }
}
</style>