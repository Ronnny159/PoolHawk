import http from './http'

/**
 * Crea una nueva partida en el backend.
 * 
 * @param {Object} datos - { modalidad: 'BOLA_8' | 'POR_PUNTOS', equipoA: [], equipoB: [] }
 * @returns {Promise} Promesa con la partida creada.
 */
export const crearPartida = async (datos) => {
  const response = await http.post('/matches', datos)
  return response.data
}

/**
 * Obtiene el estado actual de una partida por su ID.
 * 
 * @param {string} matchId - UUID de la partida.
 * @returns {Promise} Promesa con el estado de la partida.
 */
export const obtenerPartida = async (matchId) => {
  const response = await http.get(`/matches/${matchId}`)
  return response.data
}

/**
 * Lista todas las partidas registradas.
 * 
 * @returns {Promise} Promesa con el arreglo de partidas.
 */
export const listarPartidas = async () => {
  const response = await http.get('/matches')
  return response.data
}

/**
 * Registra un tiro en una partida.
 * 
 * @param {string} matchId - UUID de la partida.
 * @param {Object} tiro - { jugadorId, primeraBolaTocada, bolasEmbolsadas, troneras, duracionSegundos }
 * @returns {Promise} Promesa con el resultado del tiro evaluado por las reglas.
 */
export const registrarTiro = async (matchId, tiro) => {
  const response = await http.post(`/matches/${matchId}/events`, tiro)
  return response.data
}