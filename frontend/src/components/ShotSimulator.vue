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

    let payload = { jugadorId: jugadorActual.id, duracionSegundos: 5 }

    if (tipo === 'FALLO') {
      // No toca ninguna bola -> generará falta NO_TOCAR_BOLA
      payload.primeraBolaTocada = null
      payload.bolasEmbolsadas = []
    } else if (tipo === 'LEGAL') {
      // Toca la bola 1 (lisa) pero no embolsa
      payload.primeraBolaTocada = 1
      payload.bolasEmbolsadas = []
    } else if (tipo === 'EMBOLSÓ') {
      // Toca la bola 1 y la embolsa
      payload.primeraBolaTocada = 1
      payload.bolasEmbolsadas = [1]
      payload.troneras = ['TOP_LEFT']
    }

    await registrarTiro(props.partida.id, payload)
    
    // Recargar estado de la partida después del tiro
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
      <button @click="simularTiro('FALLO')" :disabled="cargando" class="btn-rojo">
        Fallar (No tocar bola)
      </button>
      <button @click="simularTiro('LEGAL')" :disabled="cargando" class="btn-gris">
        Tiro Legal (Sin embolsar)
      </button>
      <button @click="simularTiro('EMBOLSÓ')" :disabled="cargando" class="btn-verde">
        Embolsar Bola
      </button>
    </div>
  </div>
</template>

<style scoped>
.simulador { background: #1e1e1e; padding: 2rem; border-radius: 12px; margin-top: 2rem; max-width: 800px; margin-left: auto; margin-right: auto; color: white; }
h3 { margin-top: 0; color: #aaa; }
.botones { display: flex; gap: 1rem; margin-top: 1rem; }
button { flex: 1; padding: 1rem; border: none; border-radius: 8px; font-weight: bold; cursor: pointer; }
.btn-rojo { background: #ff4444; color: white; }
.btn-gris { background: #555; color: white; }
.btn-verde { background: #4ade80; color: black; }
button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>