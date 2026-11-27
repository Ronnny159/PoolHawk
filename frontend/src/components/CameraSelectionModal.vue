<script setup>
import { ref } from 'vue'
import { toast } from '../utils/toast'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  matchId: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'camaraSeleccionada'])

const camaraSeleccionada = ref(null) // 'dispositivo', 'droidcam', 'camostudio'
const droidcamUrl = ref('')
const conectando = ref(false)

const opcionesCamara = [
  { id: 'dispositivo', icon: '📷', titulo: 'Cámara del dispositivo', descripcion: 'Usa la cámara web integrada o USB (índice 0/1)' },
  { id: 'droidcam', icon: '📱', titulo: 'DroidCam', descripcion: 'Conecta tu móvil como cámara vía IP (ej: http://192.168.1.50:4747/video)' },
  { id: 'camostudio', icon: '🎥', titulo: 'Camo Studio', descripcion: 'Usa Camo Studio desde tu iPhone/iPad o Android' }
]

const seleccionar = (tipo) => {
  camaraSeleccionada.value = tipo
  if (tipo === 'droidcam') {
    droidcamUrl.value = ''
  }
}

const conectar = async () => {
  if (!camaraSeleccionada.value) return
  
  conectando.value = true
  
  try {
    // Simular conexión - en producción aquí se llamaría al backend
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // Emitir evento con la configuración seleccionada
    const config = {
      tipo: camaraSeleccionada.value,
      url: camaraSeleccionada.value === 'droidcam' ? droidcamUrl.value : null
    }
    
    emit('camaraSeleccionada', { matchId: props.matchId, config })
    emit('update:modelValue', false)
    toast.success('Cámara configurada correctamente')
  } catch (error) {
    toast.error('No se pudo conectar la cámara: ' + error.message)
  } finally {
    conectando.value = false
  }
}

const cerrar = () => {
  emit('update:modelValue', false)
}
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="modelValue" class="modal-overlay" @click.self="cerrar">
      <div class="modal-container" role="dialog" aria-modal="true" aria-labelledby="camera-modal-title">
        <header class="modal-header">
          <h3 id="camera-modal-title">📷 Seleccionar Fuente de Video</h3>
          <button class="btn-cerrar" @click="cerrar" aria-label="Cerrar">✕</button>
        </header>
        
        <div class="modal-body">
          <p class="modal-subtitle">La partida se ha creado. ¿Qué fuente de video deseas usar para el análisis en vivo?</p>
          
          <div class="camera-options">
            <button
              v-for="opcion in opcionesCamara"
              :key="opcion.id"
              class="camera-option"
              :class="{ selected: camaraSeleccionada === opcion.id }"
              @click="seleccionar(opcion.id)"
              :disabled="conectando"
            >
              <span class="camera-icon">{{ opcion.icon }}</span>
              <div class="camera-info">
                <strong>{{ opcion.titulo }}</strong>
                <span>{{ opcion.descripcion }}</span>
              </div>
              <span v-if="camaraSeleccionada === opcion.id" class="checkmark">✓</span>
            </button>
          </div>
          
          <div v-if="camaraSeleccionada === 'droidcam'" class="droidcam-input">
            <label for="droidcam-url">URL de DroidCam:</label>
            <input
              id="droidcam-url"
              v-model="droidcamUrl"
              type="text"
              placeholder="http://192.168.x.x:4747/video"
              @keyup.enter="conectar"
              :disabled="conectando"
            />
            <p class="input-hint">Ejemplo: http://192.168.1.50:4747/video</p>
          </div>
          
          <div v-if="camaraSeleccionada === 'camostudio'" class="camostudio-info">
            <p>📋 Asegúrate de tener Camo Studio ejecutándose en tu dispositivo móvil y el servidor Camo activo en tu PC.</p>
            <p>La URL típica es: <code>http://localhost:4747/video</code> o la IP que muestra la app.</p>
          </div>
          
          <div v-if="camaraSeleccionada === 'dispositivo'" class="dispositivo-info">
            <p>📷 Se usará la cámara por defecto del sistema (índice 0).</p>
            <p>Si tienes múltiples cámaras, el agente usará la primera disponible.</p>
          </div>
        </div>
        
        <footer class="modal-footer">
          <button class="btn-cancelar" @click="cerrar" :disabled="conectando">Cancelar</button>
          <button 
            class="btn-conectar" 
            @click="conectar" 
            :disabled="!camaraSeleccionada || conectando || (camaraSeleccionada === 'droidcam' && !droidcamUrl.trim())"
          >
            {{ conectando ? 'Conectando...' : 'Conectar Cámara' }}
          </button>
        </footer>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
}

.modal-container {
  width: 100%;
  max-width: 520px;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6);
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #334155;
}

.modal-header h3 {
  margin: 0;
  color: #e2e8f0;
  font-size: 1.25rem;
}

.btn-cerrar {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-cerrar:hover {
  background: #334155;
  color: white;
}

.modal-body {
  padding: 1.5rem;
}

.modal-subtitle {
  margin: 0 0 1.5rem;
  color: #94a3b8;
  font-size: 0.95rem;
  line-height: 1.5;
}

.camera-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.camera-option {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #0f172a;
  border: 2px solid #334155;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.camera-option:hover:not(:disabled) {
  border-color: #22c55e;
  background: #1e293b;
}

.camera-option.selected {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.15);
}

.camera-option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.camera-icon {
  font-size: 1.75rem;
  flex-shrink: 0;
}

.camera-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.camera-info strong {
  color: #e2e8f0;
  font-size: 1rem;
}

.camera-info span {
  color: #94a3b8;
  font-size: 0.8rem;
}

.checkmark {
  color: #22c55e;
  font-size: 1.25rem;
  font-weight: bold;
}

.droidcam-input,
.camostudio-info,
.dispositivo-info {
  padding: 1rem;
  background: #0f172a;
  border-radius: 10px;
  margin-bottom: 1rem;
  border: 1px solid #334155;
}

.droidcam-input label {
  display: block;
  margin-bottom: 0.5rem;
  color: #cbd5e1;
  font-size: 0.9rem;
}

.droidcam-input input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #1e293b;
  border: 1px solid #475569;
  border-radius: 8px;
  color: #e2e8f0;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s;
}

.droidcam-input input:focus {
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.2);
}

.input-hint {
  margin: 0.5rem 0 0;
  color: #64748b;
  font-size: 0.8rem;
}

.camostudio-info p,
.dispositivo-info p {
  margin: 0.5rem 0;
  color: #94a3b8;
  font-size: 0.85rem;
}

.camostudio-info code,
.dispositivo-info code {
  background: #1e293b;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  color: #22c55e;
  font-size: 0.85rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid #334155;
  background: #0f172a;
}

.btn-cancelar,
.btn-conectar {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancelar {
  background: transparent;
  color: #94a3b8;
  border: 1px solid #475569;
}

.btn-cancelar:hover:not(:disabled) {
  background: #1e293b;
  color: white;
}

.btn-conectar {
  background: linear-gradient(180deg, #22c55e 0%, #15803d 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 0 #0f5132, 0 8px 16px rgba(34, 197, 94, 0.3);
}

.btn-conectar:hover:not(:disabled) {
  background: linear-gradient(180deg, #16a34a 0%, #15803d 100%);
}

.btn-conectar:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #0f5132;
}

.btn-cancelar:disabled,
.btn-conectar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Transiciones */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-container,
.modal-fade-leave-to .modal-container {
  transform: translateY(20px) scale(0.95);
}
</style>