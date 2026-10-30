<script setup>
import { ref } from 'vue'
import { iniciarSesionApi } from '../api/auth'
import { toast } from '../utils/toast'

const emit = defineEmits(['ingreso'])

const usuario = ref('')
const password = ref('')
const cargando = ref(false)

const entrar = async () => {
  if (!usuario.value.trim() || !password.value) {
    toast.warning('Escribe tu usuario y tu contraseña')
    return
  }

  try {
    cargando.value = true
    const token = await iniciarSesionApi(usuario.value.trim(), password.value)
    localStorage.setItem('token', token)
    toast.success('Sesión iniciada')
    emit('ingreso', token)
  } catch (error) {
    const estado = error.response?.status
    if (estado === 401 || estado === 403) {
      toast.error('Usuario o contraseña incorrectos')
    } else {
      toast.error('No se pudo iniciar sesión. Revisa que el servidor esté activo.')
    }
    password.value = ''
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <section class="login">
    <h2>Iniciar sesión</h2>
    <p class="ayuda">Ingresa con tu cuenta para ver el marcador.</p>

    <form @submit.prevent="entrar">
      <label for="login-usuario">Usuario</label>
      <input
        id="login-usuario"
        v-model="usuario"
        type="text"
        autocomplete="username"
        :disabled="cargando"
      />

      <label for="login-password">Contraseña</label>
      <input
        id="login-password"
        v-model="password"
        type="password"
        autocomplete="current-password"
        :disabled="cargando"
      />

      <button type="submit" :disabled="cargando">
        {{ cargando ? 'Entrando...' : 'Entrar' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.login {
  max-width: 380px;
  margin: 2rem auto;
  padding: 2rem;
  background: #1e293b;
  color: #f1f5f9;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}
h2 { margin: 0 0 0.3rem; }
.ayuda { margin: 0 0 1.2rem; color: #94a3b8; font-size: 0.95rem; }
form { display: flex; flex-direction: column; gap: 0.5rem; }
label { font-size: 0.9rem; color: #cbd5e1; margin-top: 0.4rem; }
input {
  padding: 0.8rem;
  border-radius: 6px;
  border: 1px solid #334155;
  background: #0f172a;
  color: white;
  font-size: 1rem;
  outline: none;
}
input:focus { border-color: #22c55e; }
button {
  margin-top: 1.2rem;
  padding: 0.9rem;
  border: none;
  border-radius: 8px;
  background: #22c55e;
  color: #052e16;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
}
button:hover:not(:disabled) { background: #16a34a; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
