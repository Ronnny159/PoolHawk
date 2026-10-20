<script setup>
import { registrarTiro } from '../api/matches'
import { ref } from 'vue'

const props = defineProps({
  partida: { type: Object, required: true }
})

const emit = defineEmits(['tiroRegistrado'])
const cargando = ref(false)

const simularTiro = async (tipo) => {
  try {
    cargando.value = true

    // Buscar el ID del jugador actual
    const todosLosJugadores = [...props.partida.equipoA, ...props.partida.equipoB]
    const jugadorActual = todosLosJugadores.find(j => j.nombre === props.partida.jugadorActual)
    if (!jugadorActual) {
      throw new Error('Jugador actual no encontrado en la partida')
    }

    // Convención del simulador:
    //   Equipo A → bola 1 (lisas)
    //   Equipo B → bola 9 (rayadas)
    const esEquipoA = props.partida.equipoA.some(j => j.id === jugadorActual.id)
    const bolaObjetivo = esEquipoA ? 1 : 9

    let payload = { jugadorId: jugadorActual.id, duracionSegundos: 5 }

    if (tipo === 'FALLO') {
      payload.primeraBolaTocada = null
      payload.bolasEmbolsadas = []
    } else if (tipo === 'LEGAL') {
      payload.primeraBolaTocada = bolaObjetivo
      payload.bolasEmbolsadas = []
    } else if (tipo === 'EMBOLSÓ') {
      payload.primeraBolaTocada = bolaObjetivo
      payload.bolasEmbolsadas = [bolaObjetivo]
      payload.troneras = ['TOP_LEFT']
    }

    await registrarTiro(props.partida.id, payload)
    emit('tiroRegistrado')

  } catch (error) {
    console.error('Error al registrar tiro:', error)
    alert('Error al registrar el tiro')
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="simulator-panel">
    <div class="control-buttons">
      
      <!-- Botón Falta -->
      <button 
        @click="simularTiro('FALLO')" 
        :disabled="cargando" 
        class="btn-3d btn-rojo">
        {{ cargando ? '...' : 'Foul' }}
      </button>

      <!-- Botón Tiro Legal -->
      <button 
        @click="simularTiro('LEGAL')" 
        :disabled="cargando" 
        class="btn-3d btn-gris">
        {{ cargando ? '...' : 'Legal Shot' }}
      </button>

      <!-- Botón Embolsar -->
      <button 
        @click="simularTiro('EMBOLSÓ')" 
        :disabled="cargando" 
        class="btn-3d btn-verde">
        {{ cargando ? '...' : 'Pocket Ball' }}
      </button>
      
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   PANEL DE CONTROL (Extensión de la mesa)
   ============================================================ */
.simulator-panel {
  max-width: 950px;
  margin: 1.5rem auto 0;
  padding: 1.5rem 2rem;
  /* Fondo de madera para que parezca la parte baja de la mesa */
  background: linear-gradient(135deg, #2a1106 0%, #4a220d 50%, #2a1106 100%);
  border-radius: 12px;
  border: 2px solid #1a0a02;
  box-shadow: 
    0 20px 40px rgba(0, 0, 0, 0.8),
    inset 0 0 20px rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
}

/* Contenedor de los botones */
.control-buttons {
  display: flex;
  gap: 1.5rem;
  width: 100%;
  max-width: 700px;
  justify-content: space-between;
}

/* ============================================================
   BOTONES CON EFECTO FÍSICO 3D
   ============================================================ */
.btn-3d {
  flex: 1;
  padding: 12px 20px;
  font-family: 'Inter', sans-serif;
  font-weight: 800;
  font-size: 1.1rem;
  letter-spacing: 0.5px;
  border-radius: 30px;
  border: none;
  cursor: pointer;
  position: relative;
  transition: all 0.1s ease;
  text-transform: capitalize;
  outline: none;
}

/* Estado normal */
.btn-3d:not(:disabled):hover {
  transform: translateY(-2px);
}

/* Efecto al presionar (hundimiento) */
.btn-3d:not(:disabled):active {
  transform: translateY(4px);
}

/* Estado deshabilitado */
.btn-3d:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

/* ============================================================
   COLORES ESPECÍFICOS
   ============================================================ */

/* 🔴 Foul (Rojo) */
.btn-rojo {
  background: #ef4444;
  color: #ffffff;
  box-shadow: 
    0 5px 0 #991b1b, /* Borde inferior grueso */
    0 8px 15px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.3);
}
.btn-rojo:not(:disabled):hover {
  background: #f87171;
  box-shadow: 
    0 7px 0 #991b1b,
    0 10px 18px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.3);
}
.btn-rojo:not(:disabled):active {
  background: #dc2626;
  box-shadow: 
    0 1px 0 #991b1b,
    0 3px 8px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.1);
}

/* ⚪ Legal Shot (Gris) */
.btn-gris {
  background: #cbd5e1;
  color: #000000;
  box-shadow: 
    0 5px 0 #64748b,
    0 8px 15px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.8);
}
.btn-gris:not(:disabled):hover {
  background: #e2e8f0;
  box-shadow: 
    0 7px 0 #64748b,
    0 10px 18px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.8);
}
.btn-gris:not(:disabled):active {
  background: #94a3b8;
  box-shadow: 
    0 1px 0 #64748b,
    0 3px 8px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.4);
}

/* 🟢 Pocket Ball (Verde Neón) */
.btn-verde {
  background: #4ade80;
  color: #000000;
  box-shadow: 
    0 5px 0 #15803d,
    0 8px 15px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.6);
}
.btn-verde:not(:disabled):hover {
  background: #86efac;
  box-shadow: 
    0 7px 0 #15803d,
    0 10px 18px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.6);
}
.btn-verde:not(:disabled):active {
  background: #22c55e;
  box-shadow: 
    0 1px 0 #15803d,
    0 3px 8px rgba(0, 0, 0, 0.5),
    inset 0 2px 2px rgba(255, 255, 255, 0.4);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 650px) {
  .control-buttons {
    flex-direction: column;
    gap: 1rem;
  }
  .btn-3d {
    width: 100%;
    padding: 15px;
  }
}
</style>