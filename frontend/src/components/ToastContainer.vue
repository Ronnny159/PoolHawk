<script setup>
import { toasts, cerrar } from '../utils/toast'

const iconos = {
  success: '\u2713',
  error: '\u2715',
  warning: '!',
  info: 'i',
}
</script>

<template>
  <div class="toast-container" role="status" aria-live="polite">
    <TransitionGroup name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="'toast-' + t.tipo"
        @click="cerrar(t.id)"
      >
        <span class="toast-icono" aria-hidden="true">{{ iconos[t.tipo] }}</span>
        <span class="toast-mensaje">{{ t.mensaje }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  max-width: min(360px, calc(100vw - 2rem));
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.8rem 1rem;
  background: #1e293b;
  color: #f1f5f9;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  font-size: 0.95rem;
  cursor: pointer;
  pointer-events: auto;
}

.toast-icono {
  flex: none;
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.toast-success .toast-icono { background: #22c55e; color: #052e16; }
.toast-error .toast-icono { background: #ef4444; color: #ffffff; }
.toast-warning .toast-icono { background: #f59e0b; color: #1c1917; }
.toast-info .toast-icono { background: #3b82f6; color: #ffffff; }

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
