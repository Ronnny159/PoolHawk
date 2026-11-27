// ---------------------------------------------------------------------------
// CONTRATO con el backend (login y correccion de tiros).
//
// - LOGIN: confirmado con AuthController (develop): POST /api/v1/auth/login
//   con { username, password } -> { token }. Credenciales de demo en el README.
// - CORRECCION: PUT /api/v1/matches/{matchId}/events/last
//   Requiere eventId con prefijo "CORRECCION:" y versionEsperada.
// ---------------------------------------------------------------------------

// Si es true, la app pide login antes de mostrar el marcador.
// Si el backend no tiene /auth/login, ponerlo en false.
export const AUTH_REQUERIDA = true

// Rutas relativas a baseURL (ver http.js)
export const ENDPOINTS = {
  login: '/auth/login',
  corregirTiro: (matchId) => `/matches/${matchId}/events/last`,
  uploadVideo: '/analysis/upload',
}

// Metodo HTTP de la correccion ('put', 'patch' o 'post')
export const METODO_CORRECCION = 'put'

// Nombres de los campos del login
export const CAMPOS_LOGIN = {
  usuario: 'username',
  password: 'password',
  token: 'token',
}

/**
 * Construye el payload para la corrección de tiro.
 * El backend espera:
 * - eventId: string con prefijo "CORRECCION:" + timestamp
 * - versionEsperada: cantidad de tiros actual de la partida (para optimistic locking)
 * - jugadorId, primeraBolaTocada, bolasEmbolsadas, troneras, duracionSegundos, motivo
 * 
 * @param {Object} params - { jugadorId, primeraBola, embolsadas, motivo, versionEsperada }
 * @returns {Object} Payload listo para enviar al backend
 */
export const armarCorreccion = ({ jugadorId, primeraBola, embolsadas, motivo, versionEsperada }) => {
  const numeros = Object.keys(embolsadas).map(Number).sort((a, b) => a - b)
  return {
    // eventId único para la corrección (requerido por backend)
    eventId: `CORRECCION:${Date.now()}`,
    jugadorId,
    primeraBolaTocada: primeraBola,
    bolasEmbolsadas: numeros,
    troneras: numeros.map((n) => embolsadas[n]),
    duracionSegundos: 5,
    motivo,
    versionEsperada // Obligatorio para validación de concurrencia
  }
}
