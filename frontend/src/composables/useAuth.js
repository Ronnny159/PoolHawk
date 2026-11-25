import { ref } from 'vue'

export function useAuth() {
  const tokenGuardado = localStorage.getItem('token')
  const token = ref(tokenGuardado && tokenGuardado.length > 20 ? tokenGuardado : null)
  if (!token.value) localStorage.removeItem('token')

  const iniciarSesion = (nuevoToken) => {
    token.value = nuevoToken
    localStorage.setItem('token', nuevoToken)
  }

  const cerrarSesion = () => {
    localStorage.removeItem('token')
    token.value = null
  }

  return { token, iniciarSesion, cerrarSesion }
}