<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { corregirTiro } from '../api/correcciones'
import { toast } from '../utils/toast'

const props = defineProps({
  partida: { type: Object, required: true }
})

const emit = defineEmits(['cerrar', 'corregido'])

const TRONERAS = [
  { valor: 'TOP_LEFT', texto: 'Superior izquierda' },
  { valor: 'TOP_RIGHT', texto: 'Superior derecha' },
  { valor: 'MIDDLE_LEFT', texto: 'Lateral izquierda' },
  { valor: 'MIDDLE_RIGHT', texto: 'Lateral derecha' },
  { valor: 'BOTTOM_LEFT', texto: 'Inferior izquierda' },
  { valor: 'BOTTOM_RIGHT', texto: 'Inferior derecha' }
]

// 0 = bola blanca, 1 a 15 = bolas numeradas
const BOLAS = Array.from({ length: 16 }, (_, n) => n)

const jugadores = computed(() => [...props.partida.equipoA, ...props.partida.equipoB])

const jugadorId = ref(
  jugadores.value.find((j) => j.nombre === props.partida.jugadorActual)?.id ??
    jugadores.value[0]?.id ??
    ''
)
const primeraBola = ref('')
const embolsadas = ref({})
const motivo = ref('')
const enviando = ref(false)
const primerCampo = ref(null)

const nombreBola = (n) => (n === 0 ? 'Blanca' : String(n))

const numerosEmbolsados = computed(() =>
  Object.keys(embolsadas.value)
    .map(Number)
    .sort((a, b) => a - b)
)

const alternarBola = (n) => {
  const copia = { ...embolsadas.value }
  if (n in copia) {
    delete copia[n]
  } else {
    copia[n] = TRONERAS[0].valor
  }
  embolsadas.value = copia
}

const guardar = async () => {
  try {
    enviando.value = true
    await corregirTiro(props.partida.id, {
      jugadorId: jugadorId.value,
      primeraBola: primeraBola.value === '' ? null : Number(primeraBola.value),
      embolsadas: embolsadas.value,
      motivo: motivo.value.trim()
    })
    toast.success('Tiro corregido')
    emit('corregido')
    emit('cerrar')
  } catch (error) {
    const estado = error.response?.status
    if (estado === 404 || estado === 405) {
      toast.error('El servidor aún no tiene habilitada la corrección de tiros')
    } else if (estado === 401 || estado === 403) {
      toast.error('No tienes permiso para corregir tiros')
    } else {
      toast.error('No se pudo guardar la corrección')
    }
  } finally {
    enviando.value = false
  }
}

const alPresionarTecla = (evento) => {
  if (evento.key === 'Escape') emit('cerrar')
}

onMounted(() => {
  document.addEventListener('keydown', alPresionarTecla)
  primerCampo.value?.focus()
})

onUnmounted(() => {
  document.removeEventListener('keydown', alPresionarTecla)
})
</script>

<template>
  <div class="overlay" @click.self="$emit('cerrar')">
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="correccion-titulo">
      <h3 id="correccion-titulo">Corregir tiro</h3>
      <p class="ayuda">Indica lo que ocurrió realmente en el tiro que quieres corregir.</p>

      <form @submit.prevent="guardar">
        <label for="corr-jugador">Jugador</label>
        <select id="corr-jugador" ref="primerCampo" v-model="jugadorId" :disabled="enviando">
          <option v-for="j in jugadores" :key="j.id" :value="j.id">{{ j.nombre }}</option>
        </select>

        <label for="corr-primera">Primera bola tocada</label>
        <select id="corr-primera" v-model="primeraBola" :disabled="enviando">
          <option value="">Ninguna (no tocó bola)</option>
          <option v-for="n in BOLAS.slice(1)" :key="n" :value="n">{{ n }}</option>
        </select>

        <span class="etiqueta" id="corr-embolsadas">Bolas embolsadas</span>
        <div class="bolas" role="group" aria-labelledby="corr-embolsadas">
          <button
            v-for="n in BOLAS"
            :key="n"
            type="button"
            class="bola"
            :class="{ activa: n in embolsadas }"
            :aria-pressed="n in embolsadas"
            :disabled="enviando"
            @click="alternarBola(n)"
          >
            {{ nombreBola(n) }}
          </button>
        </div>

        <ul v-if="numerosEmbolsados.length" class="troneras">
          <li v-for="n in numerosEmbolsados" :key="n">
            <label :for="'corr-tronera-' + n">{{ nombreBola(n) }} cayó en</label>
            <select :id="'corr-tronera-' + n" v-model="embolsadas[n]" :disabled="enviando">
              <option v-for="t in TRONERAS" :key="t.valor" :value="t.valor">{{ t.texto }}</option>
            </select>
          </li>
        </ul>
        <p v-else class="ayuda">Ninguna bola embolsada.</p>

        <label for="corr-motivo">Motivo de la corrección</label>
        <textarea
          id="corr-motivo"
          v-model="motivo"
          rows="2"
          maxlength="200"
          placeholder="Ej.: la cámara no detectó el primer contacto"
          :disabled="enviando"
        ></textarea>

        <div class="acciones">
          <button type="button" class="btn-cancelar" :disabled="enviando" @click="$emit('cerrar')">
            Cancelar
          </button>
          <button type="submit" class="btn-guardar" :disabled="enviando">
            {{ enviando ? 'Guardando...' : 'Guardar corrección' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.65);
}
.modal {
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 1.8rem;
  background: #1e293b;
  color: #f1f5f9;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
}
h3 { margin: 0 0 0.3rem; }
.ayuda { margin: 0 0 1rem; color: #94a3b8; font-size: 0.95rem; }
form { display: flex; flex-direction: column; gap: 0.45rem; }
label, .etiqueta { margin-top: 0.5rem; font-size: 0.9rem; color: #cbd5e1; }
select, textarea {
  padding: 0.7rem;
  border-radius: 6px;
  border: 1px solid #334155;
  background: #0f172a;
  color: white;
  font-size: 1rem;
  font-family: inherit;
}
select:focus, textarea:focus { outline: none; border-color: #f59e0b; }
.bolas {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(3.6rem, 1fr));
  gap: 0.4rem;
}
.bola {
  padding: 0.55rem 0.3rem;
  border: 1px solid #334155;
  border-radius: 6px;
  background: #0f172a;
  color: #cbd5e1;
  font-weight: bold;
  cursor: pointer;
}
.bola.activa { background: #f59e0b; border-color: #f59e0b; color: #1c1917; }
.troneras { list-style: none; margin: 0.4rem 0 0; padding: 0; display: flex; flex-direction: column; gap: 0.4rem; }
.troneras li { display: flex; align-items: center; gap: 0.8rem; }
.troneras label { margin: 0; flex: 0 0 8rem; }
.troneras select { flex: 1; }
.acciones { display: flex; gap: 0.8rem; margin-top: 1.2rem; }
.acciones button {
  flex: 1;
  padding: 0.9rem;
  border: none;
  border-radius: 8px;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
}
.btn-cancelar { background: #334155; color: white; }
.btn-guardar { background: #f59e0b; color: #1c1917; }
button:disabled, select:disabled, textarea:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
