<script setup>
import BilliardButton from './BilliardButton.vue'
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
  <div class="simulador">
    <h3>Panel de Control (Simulador)</h3>
    <p>Usa estos botones para simular lo que la cámara enviaría.</p>
    
    <div class="botones">
      <BilliardButton 
        tipo="peligro" 
        :deshabilitado="cargando || partida.fase === 'FINALIZADA'" 
        @click="simularTiro('FALLO')">
        Fallar (No tocar bola)
      </BilliardButton>

      <BilliardButton 
        tipo="neutro" 
        :deshabilitado="cargando || partida.fase === 'FINALIZADA'" 
        @click="simularTiro('LEGAL')">
        Tiro Legal (Sin embolsar)
      </BilliardButton>

      <BilliardButton 
        tipo="exito" 
        :deshabilitado="cargando || partida.fase === 'FINALIZADA'" 
        @click="simularTiro('EMBOLSÓ')">
        Embolsar Bola
      </BilliardButton>
    </div>
  </div>
</template>

<style scoped>
.simulador { 
  background: #1e1e1e; 
  padding: 2rem; 
  border-radius: 12px; 
  margin-top: 2rem; 
  max-width: 800px; 
  margin-left: auto; 
  margin-right: auto; 
  color: white; 
}

h3 { 
  margin-top: 0; 
  color: #aaa; 
}

.botones { 
  display: flex; 
  gap: 1rem; 
  margin-top: 1rem; 
}
</style>