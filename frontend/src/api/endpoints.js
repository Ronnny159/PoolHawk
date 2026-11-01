// ---------------------------------------------------------------------------
// CONTRATO con el backend (login y correccion de tiros).
//
// - LOGIN: confirmado con AuthController (develop): POST /api/v1/auth/login
//   con { username, password } -> { token }. Credenciales de demo en el README.
// - CORRECCION: PROVISIONAL. El endpoint todavia no existe en develop; cuando
//   exista, se ajusta aqui (ruta, metodo y cuerpo) y nada mas.
// ---------------------------------------------------------------------------

// Si es true, la app pide login antes de mostrar el marcador.
// Si el backend no tiene /auth/login, ponerlo en false.
export const AUTH_REQUERIDA = true

// Rutas relativas a baseURL (ver http.js)
export const ENDPOINTS = {
  login: '/auth/login',
  corregirTiro: (matchId) => `/matches/${matchId}/events/last`,
}

// Metodo HTTP de la correccion ('put', 'patch' o 'post')
export const METODO_CORRECCION = 'put'

// Nombres de los campos del login
export const CAMPOS_LOGIN = {
  usuario: 'username',
  password: 'password',
  token: 'token',
}

// Cuerpo de la correccion a partir de lo que llena el arbitro en el modal.
// `embolsadas` es { numeroDeBola: nombreDeTronera }.
export const armarCorreccion = ({ jugadorId, primeraBola, embolsadas, motivo }) => {
  const numeros = Object.keys(embolsadas).map(Number).sort((a, b) => a - b)
  return {
    jugadorId,
    primeraBolaTocada: primeraBola,
    bolasEmbolsadas: numeros,
    troneras: numeros.map((n) => embolsadas[n]),
    duracionSegundos: 5,
    motivo,
  }
}
