<script setup>
import { registrarTiro } from '../api/matches'
import { ref } from 'vue'
import { toast } from '../utils/toast'

const props = defineProps({
  partida: { type: Object, required: true }
})

const emit = defineEmits(['tiroRegistrado'])
const cargando = ref(false)

const avisarResultado = (resultado) => {
  const mensaje = resultado?.descripcion ?? 'Tiro registrado'
  if (mensaje.startsWith('Falta')) {
    toast.warning(mensaje)
  } else {
    toast.success(mensaje)
  }
}

const simularTiro = async (tipo) => {
  cargando.value = true

  try {
    const todosLosJugadores = [...props.partida.equipoA, ...props.partida.equipoB]
    const jugadorActual = todosLosJugadores.find(j => j.nombre === props.partida.jugadorActual)
    
    if (!jugadorActual) throw new Error('Jugador actual no encontrado')

    const esEquipoA = props.partida.equipoA.some(j => j.id === jugadorActual.id)
    const bolaObjetivo = esEquipoA ? 1 : 10
    const bolaAEmbolsar = esEquipoA ? 2 : 11

    // Generamos el ID único UNA SOLA VEZ por tiro
    const eventId = crypto.randomUUID()

    let payload = { 
      eventId: eventId, 
      jugadorId: jugadorActual.id, 
      duracionSegundos: 5 
    }

    if (tipo === 'FALLO') {
      payload.primeraBolaTocada = null
      payload.bolasEmbolsadas = []
    } else if (tipo === 'LEGAL') {
      payload.primeraBolaTocada = bolaObjetivo
      payload.bolasEmbolsadas = []
    } else if (tipo === 'EMBOLSÓ') {
      payload.primeraBolaTocada = bolaObjetivo
      payload.bolasEmbolsadas = [bolaAEmbolsar]
      payload.troneras = ['TOP_LEFT']
    }

    console.log(`Simulando tiro para ${jugadorActual.nombre}`, payload)

    // BUCLE DE REINTENTO
    let exito = false
    let intentos = 0
    const maxIntentos = 3

    while (!exito && intentos < maxIntentos) {
      try {
        const resultado = await registrarTiro(props.partida.id, payload)
        emit('tiroRegistrado')
        avisarResultado(resultado)
        exito = true
      } catch (error) {
        // Si es un error de red (sin respuesta) o un error de servidor (5xx)
        const esErrorRed = !error.response || error.response.status >= 500
        
        if (esErrorRed && intentos < maxIntentos - 1) {
          intentos++
          toast.warning(`Error de red. Reintentando tiro (Intento ${intentos}/${maxIntentos - 1})...`)
          // Esperamos 2 segundos antes de reintentar
          await new Promise(resolve => setTimeout(resolve, 2000))
        } else {
          // Errores 4xx (como 400 o 409) o si se agotaron los intentos, lanzamos el error
          throw error
        }
      }
    }

    if (!exito) {
      throw new Error('No se pudo registrar el tiro tras múltiples intentos.')
    }

  } catch (error) {
    console.error('Error al registrar tiro:', error)
    const mensaje = error.response?.data?.message || error.message
    toast.error('Error: ' + mensaje)
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
      <button @click="simularTiro('FALLO')" :disabled="cargando" class="btn-rojo">Fallar (No tocar bola)</button>
      <button @click="simularTiro('LEGAL')" :disabled="cargando" class="btn-gris">Tiro Legal (Toca bola)</button>
      <button @click="simularTiro('EMBOLSÓ')" :disabled="cargando" class="btn-verde">Embolsar Bola</button>
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