import http from './http'
import { ENDPOINTS, CAMPOS_LOGIN } from './endpoints'

/**
 * Inicia sesion en el backend y devuelve el token.
 * Lanza un error si las credenciales fallan o la respuesta no trae token.
 */
export async function iniciarSesionApi(usuario, password) {
  const { data } = await http.post(ENDPOINTS.login, {
    [CAMPOS_LOGIN.usuario]: usuario,
    [CAMPOS_LOGIN.password]: password,
  })
  const token = data?.[CAMPOS_LOGIN.token]
  if (!token) {
    throw new Error('La respuesta del login no trae token')
  }
  return token
}
