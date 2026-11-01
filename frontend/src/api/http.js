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
  headers: { 'Content-Type': 'application/json' }
})

// Interceptor para imprimir errores en consola (útil en desarrollo)
http.interceptors.request.use(
  (config) => {
    let token = localStorage.getItem('token')
    
    // TRUCO TEMPORAL PARA DESARROLLO:
    // Si no hay token (porque no hay Login aún), usamos uno de prueba
    if (!token) {
      token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcmJpdHJvIiwiaWF0IjoxNzkzMzIwOTg3LCJleHAiOjE3OTM0MDczODd9.xG7MAlz5IScZRuKKzMowTZysKHsXOCpR9fNN5Mgnse4'
    }
    
    config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error)
)

export default http