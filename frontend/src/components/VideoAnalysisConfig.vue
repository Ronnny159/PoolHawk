<template>
  <div class="config-avanzada">
    <h3>⚙️ Configuraciones Avanzadas</h3>

    <div class="video-upload-section">
      <h4>📹 Análisis de Video</h4>
      <p>
        Sube un video de una partida de billar para que el agente de visión
        analice automáticamente los tiros y las posiciones de las bolas.
      </p>

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
        🚀 Subir y Analizar
      </button>

      <div v-if="uploadSuccess" class="upload-success">
        ✅ {{ mensajeExito }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { toast } from '../utils/toast'
import { uploadVideo } from '../api/videoAnalysis'

const fileInput = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadError = ref(null)
const uploadSuccess = ref(false)
const mensajeExito = ref('')

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
  uploadSuccess.value = false
  mensajeExito.value = ''
}

const iniciarSubida = async () => {
  if (!selectedFile.value) return

  uploading.value = true
  uploadProgress.value = 0
  uploadError.value = null
  uploadSuccess.value = false

  try {
    await uploadVideo(selectedFile.value, (progreso) => {
      uploadProgress.value = progreso
    })

    uploadSuccess.value = true
    mensajeExito.value = 'Video cargado. El agente de visión comenzará el análisis.'
    toast.success(mensajeExito.value)

    // Resetear input de archivo
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    selectedFile.value = null
  } catch (error) {
    uploadError.value = error.message || 'Error al subir el video'
    toast.error(uploadError.value)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped>
.config-avanzada {
  max-width: 600px;
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
}

.progress-fill {
  height: 100%;
  background: #4ade80;
  width: 0;
  transition: width 0.3s;
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

.btn-upload:hover {
  background: #22c55e;
}

.upload-success {
  color: #4ade80;
  font-size: 0.9rem;
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(74, 222, 128, 0.1);
  border-radius: 6px;
  border: 1px solid #4ade80;
}
</style>