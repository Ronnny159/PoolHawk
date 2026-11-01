import { reactive } from 'vue'

// Lista reactiva de notificaciones visibles (las dibuja ToastContainer.vue).
export const toasts = reactive([])

const MAX_VISIBLES = 4
let siguienteId = 1

export function cerrar(id) {
  const i = toasts.findIndex((t) => t.id === id)
  if (i !== -1) toasts.splice(i, 1)
}

function mostrar(tipo, mensaje, duracion) {
  const id = siguienteId++
  toasts.push({ id, tipo, mensaje })
  if (toasts.length > MAX_VISIBLES) toasts.shift()
  setTimeout(() => cerrar(id), duracion)
  return id
}

// Uso: toast.success('Listo'), toast.error('Algo fallo'), etc.
export const toast = {
  success: (mensaje, duracion = 3500) => mostrar('success', mensaje, duracion),
  error: (mensaje, duracion = 6000) => mostrar('error', mensaje, duracion),
  warning: (mensaje, duracion = 5000) => mostrar('warning', mensaje, duracion),
  info: (mensaje, duracion = 4000) => mostrar('info', mensaje, duracion),
}
