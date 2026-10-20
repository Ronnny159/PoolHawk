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
  if (partidaTerminada.value) return
  
  try {
    cargando.value = true
    
    const todosLosJugadores = [...props.partida.equipoA, ...props.partida.equipoB]
    const jugadorActual = todosLosJugadores.find(j => j.nombre === props.partida.jugadorActual)

    let payload = { jugadorId: jugadorActual.id, duracionSegundos: 5 }

    if (tipo === 'FALLO') {
      payload.primeraBolaTocada = null
      payload.bolasEmbolsadas = []
    } else {
      // Lógica inteligente de grupos
      const grupoActual = props.partida.grupos[jugadorActual.nombre] // "LISAS", "RAYADAS" o "SIN_ASIGNAR"
      
      let bolaASimular = 1 // Por defecto Lisa
      if (grupoActual === 'RAYADAS') {
        bolaASimular = 9 // Bola Rayada
      }

      payload.primeraBolaTocada = bolaASimular
      
      if (tipo === 'EMBOLSÓ') {
        payload.bolasEmbolsadas = [bolaASimular]
        payload.troneras = ['TOP_LEFT']
      } else {
        payload.bolasEmbolsadas = []
      }
    }

    const resultado = await registrarTiro(props.partida.id, payload)
    ultimoResultado.value = resultado
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