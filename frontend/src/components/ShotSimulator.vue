<script setup>
import { registrarTiro } from '../api/matches'
import { ref } from 'vue'
import { toast } from '../utils/toast'

const props = defineProps({
  partida: { type: Object, required: true }
})

const emit = defineEmits(['tiroRegistrado'])
const cargando = ref(false)

const avisarResultado = (resultado, fueReintento) => {
  const mensaje = resultado?.descripcion ?? 'Tiro registrado'
  const sufijo = fueReintento ? ' (idempotente)' : ''
  if (mensaje.startsWith('Falta')) {
    toast.warning(mensaje + sufijo)
  } else {
    toast.success(mensaje + sufijo)
  }
}

const simularTiro = async (tipo) => {
  cargando.value = true

  try {
    const todosLosJugadores = [...props.partida.equipoA, ...props.partida.equipoB]
    const jugadorActual = todosLosJugadores.find(j => j.nombre === props.partida.jugadorActual)

    if (!jugadorActual) throw new Error('Jugador actual no encontrado')

    // Lista de bolas disponibles (excluyendo la 8 para que no termine la partida)
    const bolasDisponibles = [1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15]
    // Elegimos una bola al azar para no repetir siempre la misma
    const bolaRandom = bolasDisponibles[Math.floor(Math.random() * bolasDisponibles.length)]

    // IMPORTANTE: eventId generado UNA SOLA VEZ por tiro. Todos los reintentos
    // del bucle comparten el mismo eventId (idempotencia transparente).
    const eventId = crypto.randomUUID()

    let payload = {
      eventId,
      jugadorId: jugadorActual.id,
      duracionSegundos: 5
    }

    if (tipo === 'FALLO') {
      payload.primeraBolaTocada = null
      payload.bolasEmbolsadas = []
    } else if (tipo === 'LEGAL') {
      payload.primeraBolaTocada = bolaRandom
      payload.bolasEmbolsadas = []
    } else if (tipo === 'EMBOLSÓ') {
      payload.primeraBolaTocada = bolaRandom
      payload.bolasEmbolsadas = [bolaRandom]
      payload.troneras = ['TOP_LEFT']
    }

    console.log(`Simulando tiro para ${jugadorActual.nombre} con bola ${bolaRandom}`, payload)

    let exito = false
    let intentos = 0
    const maxIntentos = 3

    while (!exito && intentos < maxIntentos) {
      const fueReintento = intentos > 0
      try {
        const resultado = await registrarTiro(props.partida.id, payload)
        emit('tiroRegistrado')
        avisarResultado(resultado, fueReintento)
        exito = true
      } catch (error) {
        const status = error.response?.status
        const esErrorRed = !error.response || status >= 500

        // Si el backend devuelve 400 (ej. "La bola ya fue embolsada"), no reintentamos
        // porque es un error de lógica, no de red.
        if (esErrorRed && intentos < maxIntentos - 1) {
          intentos++
          toast.warning(`Error de red. Reintentando tiro (Intento ${intentos}/${maxIntentos - 1})...`)
          await new Promise(resolve => setTimeout(resolve, 2000))
        } else {
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
      <button @click="simularTiro('LEGAL')" :disabled="cargando" class="btn-gris">Tiro Legal (Sin embolsar)</button>
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