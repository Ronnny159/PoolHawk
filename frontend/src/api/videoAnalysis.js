import http from './http'
import { ENDPOINTS } from './endpoints'

const MAX_FILE_SIZE_MB = 500

/**
 * Sube un archivo de video al backend para análisis.
 *
 * @param {File} file - Archivo de video seleccionado por el usuario.
 * @param {Function} onProgress - Callback opcional con el porcentaje de progreso (0-100).
 * @returns {Promise<{videoId: string, ruta: string}>} Respuesta del backend con el ID y ruta del video.
 */
export async function uploadVideo(file, onProgress = null) {
  // Validación de tipo
  const tiposPermitidos = ['video/mp4', 'video/webm', 'video/avi', 'video/quicktime']
  if (!tiposPermitidos.includes(file.type)) {
    throw new Error('Tipo de archivo no válido. Usa .mp4, .webm o .avi')
  }

  // Validación de tamaño (500 MB)
  const maxBytes = MAX_FILE_SIZE_MB * 1024 * 1024
  if (file.size > maxBytes) {
    throw new Error(`El archivo excede los ${MAX_FILE_SIZE_MB} MB permitidos`)
  }

  const formData = new FormData()
  formData.append('file', file)

  const config = {
    // Timeout de 120 segundos para videos grandes
    timeout: 120000,
    // No establecer Content-Type manualmente; Axios lo hace automáticamente con FormData
    // incluyendo el boundary necesario
  }

  if (onProgress) {
    config.onUploadProgress = (progreso) => {
      const porcentaje = Math.round((progreso.loaded * 100) / progreso.total)
      onProgress(porcentaje)
    }
  }

  const respuesta = await http.post(ENDPOINTS.uploadVideo, formData, config)
  return respuesta.data
}
