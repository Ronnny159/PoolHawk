<template>
  <div class="config-avanzada">
    <h3>🎬 Análisis de Video</h3>

    <div class="video-upload-section">
      <h4>📹 Subir y Analizar Video</h4>
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

      <!-- Paso 3: Comando para el agente + selección de cámara -->
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

        <!-- Selección de cámara para live tracking -->
        <div class="camera-section">
          <h4 class="step-title">📷 Cámara en Vivo (Opcional)</h4>
          <p class="step-subtitle">Si quieres seguir la partida en vivo con la cámara:</p>
          
          <div class="camera-options">
            <button class="camera-btn" @click="seleccionarCamara('dispositivo')">
              <span class="camera-icon">📷</span>
              <span>Cámara del dispositivo</span>
            </button>
            <button class="camera-btn" @click="seleccionarCamara('droidcam')">
              <span class="camera-icon">📱</span>
              <span>DroidCam</span>
            </button>
            <button class="camera-btn" @click="seleccionarCamara('camostudio')">
              <span class="camera-icon">🎥</span>
              <span>Camo Studio</span>
            </button>
          </div>

          <div v-if="camaraSeleccionada" class="camera-config">
            <div class="camera-status" :class="{ conectada: camaraConectada }">
              <span class="status-dot"></span>
              <span>{{ camaraConectada ? 'Cámara conectada ✅' : 'Cámara desconectada ❌' }}</span>
            </div>
            
            <div v-if="camaraSeleccionada === 'droidcam'" class="droidcam-input">
              <label>URL de DroidCam (ej: http://192.168.1.50:4747/video):</label>
              <input
                v-model="droidcamUrl"
                type="text"
                placeholder="http://192.168.x.x:4747/video"
                @keyup.enter="conectarCamara"
              />
            </div>
            
            <button class="btn-camara" @click="conectarCamara" :disabled="camaraConectada || conectandoCamara">
              {{ conectandoCamara ? 'Conectando...' : camaraConectada ? 'Conectada' : 'Conectar Cámara' }}
            </button>
            
            <div v-if="camaraError" class="upload-error">
              ❌ {{ camaraError }}
            </div>
          </div>
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
import { crearPartidaRapida as apiCrearPartidaRapida } from '../api/matches'

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

// Paso 3: Cámara
const camaraSeleccionada = ref(null) // 'dispositivo', 'droidcam', 'camostudio'
const camaraConectada = ref(false)
const conectandoCamara = ref(false)
const camaraError = ref(null)
const droidcamUrl = ref('')

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
    // response.filePath ahora es URL relativa (/api/v1/analysis/video/xxx.mp4)
    // Construir URL absoluta para el agente de visión
    const baseUrl = 'http://localhost:8080'
    videoPath.value = baseUrl + response.filePath
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
    const response = await apiCrearPartidaRapida({
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

const seleccionarCamara = (tipo) => {
  camaraSeleccionada.value = tipo
  camaraConectada.value = false
  camaraError.value = null
  if (tipo === 'droidcam') {
    droidcamUrl.value = ''
  }
}

const conectarCamara = async () => {
  if (!camaraSeleccionada.value) return
  
  conectandoCamara.value = true
  camaraError.value = null
  
  try {
    // Aquí se haría la llamada al backend para configurar la cámara
    // Por ahora simulamos la conexión
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    camaraConectada.value = true
    conectandoCamara.value = false
    toast.success('Cámara conectada correctamente')
  } catch (error) {
    camaraError.value = 'No se pudo conectar la cámara: ' + error.message
    toast.error(camaraError.value)
    conectandoCamara.value = false
  }
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
  camaraSeleccionada.value = null
  camaraConectada.value = false
  droidcamUrl.value = ''
  uploadError.value = null
  crearError.value = null
  camaraError.value = null
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

/* Cámara section */
.camera-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #333;
}

.camera-options {
  display: flex;
  gap: 0.75rem;
  margin: 1rem 0;
  flex-wrap: wrap;
}

.camera-btn {
  flex: 1;
  min-width: 140px;
  padding: 1rem;
  background: #2a2a2a;
  border: 2px solid #444;
  border-radius: 10px;
  color: #ddd;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.camera-btn:hover {
  border-color: #4ade80;
  background: #333;
}

.camera-btn.selected {
  border-color: #4ade80;
  background: rgba(34, 197, 94, 0.15);
}

.camera-icon {
  font-size: 1.5rem;
}

.camera-config {
  background: #2a2a2a;
  border-radius: 8px;
  padding: 1rem;
  margin-top: 1rem;
}

.camera-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 6px;
  margin-bottom: 1rem;
}

.camera-status.conectada {
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
}

.camera-status:not(.conectada) {
  background: rgba(239, 68, 68, 0.2);
  color: #ff4444;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}

.droidcam-input {
  margin-bottom: 1rem;
}

.droidcam-input label {
  display: block;
  margin-bottom: 0.5rem;
  color: #aaa;
  font-size: 0.85rem;
}

.droidcam-input input {
  width: 100%;
  padding: 0.6rem 0.8rem;
  background: #333;
  border: 1px solid #444;
  border-radius: 6px;
  color: white;
  font-size: 0.9rem;
}

.droidcam-input input:focus {
  border-color: #4ade80;
  outline: none;
}

.btn-camara {
  width: 100%;
  padding: 0.75rem;
  background: #4ade80;
  color: #000;
  border: none;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-camara:hover:not(:disabled) {
  background: #22c55e;
}

.btn-camara:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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