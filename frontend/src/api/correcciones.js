import http from './http'
import { ENDPOINTS, METODO_CORRECCION, armarCorreccion } from './endpoints'

/**
 * Envia al backend la correccion manual de un tiro hecha por el arbitro.
 * La ruta, el metodo y el cuerpo se definen en endpoints.js.
 */
export async function corregirTiro(matchId, datos) {
  const { data } = await http[METODO_CORRECCION](
    ENDPOINTS.corregirTiro(matchId),
    armarCorreccion(datos)
  )
  return data
}
