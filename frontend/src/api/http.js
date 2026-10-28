import axios from 'axios'

/**
 * Instancia centralizada de Axios para comunicarse con el backend.
 * 
 * BaseURL: apunta al servidor de Spring Boot.
 * Timeout: 5 segundos (5000 ms) para evitar que la app se quede colgada.
 */
const http = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Interceptor para inyectar el Token JWT en cada petición
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Interceptor para imprimir errores en consola (útil en desarrollo)
http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('Error en la petición HTTP:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default http