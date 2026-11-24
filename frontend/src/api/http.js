import axios from 'axios'

/**
 * Instancia centralizada de Axios para comunicarse con el backend.
 * BaseURL: apunta al servidor de Spring Boot.
 * Timeout: 5 segundos (5000 ms) para evitar que la app se quede colgada.
 */
const http = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 5000,
  headers: { 'Content-Type': 'application/json' }
})

// Interceptor para añadir el token SOLO si existe
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    // H-04 CORRECCIÓN: Solo agregar la cabecera si el token existe y no es nulo
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

export default http