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

/**
 * Determina la bola objetivo según el equipo del jugador actual.
 * Convención: Equipo A → bola 1 (lisas 1-7), Equipo B → bola 9 (rayadas 9-15)
 */
const obtenerBolaObjetivo = () => {
  const todosLosJugadores = [...props.partida.equipoA, ...props.partida.equipoB]
  const jugadorActual = todosLosJugadores.find(j => j.id === props.partida.jugadorActualId)
  
  if (!jugadorActual) {
    // Fallback: si no se encuentra, usar bola 1
    console.warn('Jugador actual no encontrado en equipos, usando bola 1 por defecto')
    return 1
  }
  
  const esEquipoA = props.partida.equipoA.some(j => j.id === jugadorActual.id)
  return esEquipoA ? 1 : 9
}

const simularTiro = async (tipo) => {
  cargando.value = true

  try {
    const jugadorId = props.partida.jugadorActualId
    if (!jugadorId) throw new Error('Jugador actual no encontrado (falta jugadorActualId)')

    const bolaObjetivo = obtenerBolaObjetivo()
    
    // IMPORTANTE: eventId generado UNA SOLA VEZ por tiro. Todos los reintentos
    // del bucle comparten el mismo eventId (idempotencia transparente).
    const eventId = crypto.randomUUID()

    let payload = {
      eventId,
      jugadorId: jugadorId,
      duracionSegundos: 5,
      versionEsperada: props.partida.cantidadTiros,
      primeraBolaTocada: null,
      bolasEmbolsadas: [],
      troneras: []
    }

    if (tipo === 'FALLO') {
      // payload ya tiene null primeraBola, [] bolas, [] troneras
    } else if (tipo === 'LEGAL') {
      payload.primeraBolaTocada = bolaObjetivo
    } else if (tipo === 'EMBOLSÓ') {
      payload.primeraBolaTocada = bolaObjetivo
      payload.bolasEmbolsadas = [bolaObjetivo]
      payload.troneras = ['TOP_LEFT']
    }

    console.log(`Simulando tiro para jugador ${jugadorId} con bola objetivo ${bolaObjetivo} (Equipo ${bolaObjetivo === 1 ? 'A (Lisas)' : 'B (Rayadas)'})`, payload)

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