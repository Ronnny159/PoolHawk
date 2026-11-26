<template>
  <div class="config-avanzada">
    <h3>⚙️ Configuraciones Avanzadas</h3>

    <div class="video-upload-section">
      <h4>📹 Análisis de Video</h4>
      <p>
        Sube un video de una partida de billar para que el agente de visión
        analice automáticamente los tiros y las posiciones de las bolas.
      </p>

      <!-- Paso 1: Subida de video -->
      <div v-if="!videoSubido" class="step-card">
        <div class="file-input-wrapper">
          <input
            type="file"
            accept="video/mp4,video/webm,video/avi"
            @change="handleFileUpload"
            ref="fileInput"
            id="video-file-input"
            style="display: none"
          />
          <label class="file-label" for="video-file-input">
            <span v-if="selectedFile">{{ selectedFile.name }}</span>
            <span v-else>Seleccionar video...</span>
          </label>
        </div>

        <div v-if="uploadError" class="upload-error">
          ❌ {{ uploadError }}
        </div>

        <div v-if="uploading" class="upload-progress">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: uploadProgress + '%' }"
            ></div>
          </div>
          <p>{{ uploadProgress }}% - Subiendo video...</p>
        </div>

        <button
          v-if="!uploading && selectedFile"
          class="btn-upload"
          @click="iniciarSubida"
        >
          🚀 Subir Video
        </button>
      </div>

      <!-- Paso 2: Nombres de jugadores -->
      <div v-else-if="videoSubido && !partidaCreada" class="step-card">
        <h4 class="step-title">👥 Jugadores de la Partida</h4>
        <p class="step-subtitle">Ingresa los nombres para la partida (Por Puntos, 1 vs 1):</p>
        
        <div class="players-form">
          <div class="player-input">
            <label>Jugador 1</label>
            <input
              v-model="jugador1"
              type="text"
              placeholder="Nombre del jugador 1"
              maxlength="20"
              @keyup.enter="crearPartidaRapida"
            />
          </div>
          <div class="player-input">
            <label>Jugador 2</label>
            <input
              v-model="jugador2"
              type="text"
              placeholder="Nombre del jugador 2"
              maxlength="20"
              @keyup.enter="crearPartidaRapida"
            />
          </div>
        </div>

        <div v-if="crearError" class="upload-error">
          ❌ {{ crearError }}
        </div>

        <button
          v-if="!creandoPartida"
          class="btn-upload"
          @click="crearPartidaRapida"
        >
          ✨ Crear Partida y Analizar
        </button>
        <div v-else class="upload-progress">
          <div class="progress-bar indeterminate"></div>
          <p>Creando partida...</p>
        </div>
      </div>

      <!-- Paso 3: Comando para el agente -->
      <div v-else-if="partidaCreada" class="step-card success">
        <h4 class="step-title">✅ Partida Creada</h4>
        <p class="step-subtitle">Ejecuta este comando en tu terminal para analizar el video:</p>
        
        <div class="command-box">
          <code>python vision_agent.py --source "{{ videoPath }}" --match-id "{{ matchId }}"</code>
          <button class="btn-copy" @click="copiarComando" title="Copiar al portapapeles">
            📋 Copiar
          </button>
        </div>

        <div class="video-info">
          <p><strong>Video:</strong> {{ videoFilename }}</p>
          <p><strong>Jugadores:</strong> {{ jugador1 }} vs {{ jugador2 }}</p>
          <p><strong>Modalidad:</strong> Por Puntos (1 vs 1)</p>
        </div>

        <button class="btn-reset" @click="resetear">
          🔄 Subir otro video
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { toast } from '../utils/toast'
import { uploadVideo } from '../api/videoAnalysis'
import { crearPartidaRapida } from '../api/matches'

// Estado del flujo
const step = ref(1) // 1: upload, 2: players, 3: done

// Paso 1: Video
const fileInput = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadError = ref(null)
const videoSubido = ref(false)
const videoPath = ref('')
const videoFilename = ref('')

// Paso 2: Jugadores
const jugador1 = ref('')
const jugador2 = ref('')
const creandoPartida = ref(false)
const crearError = ref(null)
const partidaCreada = ref(false)
const matchId = ref('')

const handleFileUpload = (event) => {
  const file = event.target.files[0]
  if (!file) return

  const tiposPermitidos = ['video/mp4', 'video/webm', 'video/avi', 'video/quicktime']
  if (!tiposPermitidos.includes(file.type)) {
    uploadError.value = 'Tipo de archivo no válido. Usa .mp4, .webm o .avi'
    toast.error(uploadError.value)
    selectedFile.value = null
    return
  }

  const maxBytes = 500 * 1024 * 1024 // 500 MB
  if (file.size > maxBytes) {
    uploadError.value = 'El archivo excede los 500 MB permitidos'
    toast.error(uploadError.value)
    selectedFile.value = null
    return
  }

  selectedFile.value = file
  uploadError.value = null
}

const iniciarSubida = async () => {
  if (!selectedFile.value) return

  uploading.value = true
  uploadProgress.value = 0
  uploadError.value = null

  try {
    const response = await uploadVideo(selectedFile.value, (progreso) => {
      uploadProgress.value = progreso
    })

    videoSubido.value = true
    videoPath.value = response.filePath
    videoFilename.value = response.filename
    
    toast.success('Video subido. Ahora ingresa los nombres de los jugadores.')
    
    // Resetear input
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    selectedFile.value = null
    uploading.value = false
    uploadProgress.value = 0
  } catch (error) {
    uploadError.value = error.message || 'Error al subir el video'
    toast.error(uploadError.value)
    uploading.value = false
    uploadProgress.value = 0
  }
}

const crearPartidaRapida = async () => {
  if (!jugador1.value.trim() || !jugador2.value.trim()) {
    crearError.value = 'Ambos nombres son obligatorios'
    toast.error(crearError.value)
    return
  }

  creandoPartida.value = true
  crearError.value = null

  try {
    const response = await crearPartidaRapida({
      jugador1: jugador1.value.trim(),
      jugador2: jugador2.value.trim()
    })

    partidaCreada.value = true
    matchId.value = response.id
    toast.success('Partida creada (' + response.id + '). Copia el comando y ejecútalo.')
    creandoPartida.value = false
  } catch (error) {
    crearError.value = error.response?.data?.message || error.message || 'Error al crear la partida'
    toast.error(crearError.value)
    creandoPartida.value = false
  }
}

const copiarComando = async () => {
  const comando = 'python vision_agent.py --source "' + videoPath.value + '" --match-id "' + matchId.value + '"'
  await navigator.clipboard.writeText(comando)
  toast.success('¡Comando copiado al portapapeles!')
}

const resetear = () => {
  step.value = 1
  videoSubido.value = false
  partidaCreada.value = false
  videoPath.value = ''
  videoFilename.value = ''
  matchId.value = ''
  jugador1.value = ''
  jugador2.value = ''
  uploadError.value = null
  crearError.value = null
}
</script>

<style scoped>
.config-avanzada {
  max-width: 700px;
  margin: 0 auto;
  padding: 1.5rem;
  background: #1e1e1e;
  border-radius: 12px;
  color: white;
}

.config-avanzada h3 {
  margin-top: 0;
  color: #fff;
  font-size: 1.25rem;
  border-bottom: 2px solid #333;
  padding-bottom: 0.75rem;
}

.config-avanzada h4 {
  margin: 1rem 0 0.5rem;
  color: #4ade80;
  font-size: 1rem;
}

.config-avanzada p {
  color: #aaa;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.step-card {
  background: #252525;
  border: 1px solid #333;
  border-radius: 10px;
  padding: 1.5rem;
  margin-top: 1rem;
}

.step-card.success {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.1);
}

.step-title {
  margin: 0 0 0.25rem;
  color: #4ade80;
}

.step-subtitle {
  margin: 0 0 1rem;
  color: #aaa;
  font-size: 0.85rem;
}

.file-input-wrapper {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  align-items: center;
}

.file-input-wrapper input[type="file"] {
  display: none;
}

.file-label {
  flex: 1;
  padding: 0.75rem 1rem;
  background: #333;
  border: 2px dashed #555;
  border-radius: 8px;
  cursor: pointer;
  color: #ccc;
  text-align: center;
  transition: all 0.2s;
}

.file-label:hover {
  border-color: #4ade80;
  background: #2a2a2a;
}

.upload-error {
  color: #ff4444;
  font-size: 0.85rem;
  margin-bottom: 0.75rem;
  padding: 0.5rem;
  background: rgba(255, 68, 68, 0.1);
  border-radius: 6px;
}

.upload-progress {
  margin-bottom: 1rem;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #333;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
  position: relative;
}

.progress-bar.indeterminate .progress-fill {
  animation: progress-indeterminate 1.5s infinite;
}

@keyframes progress-indeterminate {
  0% { left: -100%; width: 100%; }
  100% { left: 100%; width: 100%; }
}

.progress-fill {
  height: 100%;
  background: #4ade80;
  width: 0;
  transition: width 0.3s;
  position: absolute;
  left: 0;
}

.btn-upload {
  background: #4ade80;
  color: #000;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  width: 100%;
  transition: background 0.2s;
}

.btn-upload:hover:not(:disabled) {
  background: #22c55e;
}

.btn-upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.players-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.player-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.player-input label {
  color: #ccc;
  font-size: 0.85rem;
}

.player-input input {
  padding: 0.6rem 0.8rem;
  background: #333;
  border: 1px solid #444;
  border-radius: 6px;
  color: white;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.player-input input:focus {
  border-color: #4ade80;
}

.command-box {
  position: relative;
  background: #0a0f1a;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 1rem;
  margin: 1rem 0;
  overflow: hidden;
}

.command-box code {
  display: block;
  color: #4ade80;
  font-family: 'Monospace', monospace;
  font-size: 0.85rem;
  word-break: break-all;
  line-height: 1.5;
}

.btn-copy {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: #333;
  border: 1px solid #444;
  color: #ccc;
  padding: 0.35rem 0.7rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.2s;
}

.btn-copy:hover {
  background: #4ade80;
  color: #000;
  border-color: #4ade80;
}

.video-info {
  background: #2a2a2a;
  border-radius: 8px;
  padding: 1rem;
  margin: 1rem 0;
  font-size: 0.85rem;
}

.video-info p {
  margin: 0.35rem 0;
  color: #ddd;
}

.btn-reset {
  background: transparent;
  color: #94a3b8;
  border: 1px solid #444;
  padding: 0.6rem 1.2rem;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 1rem;
  transition: all 0.2s;
}

.btn-reset:hover {
  color: white;
  border-color: #666;
}
</style>