<script setup>
defineProps({
  tipo: {
    type: String,
    default: 'neutro',
    validator: (value) => ['peligro', 'exito', 'neutro'].includes(value)
  },
  deshabilitado: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<template>
  <button 
    :class="['billiard-btn', `tipo-${tipo}`]" 
    :disabled="deshabilitado"
    @click="$emit('click')">
    <slot />
  </button>
</template>

<style scoped>
/* Importamos la fuente Oswald para los botones */
@import url('https://fonts.googleapis.com/css2?family=Oswald:wght@600&display=swap');

.billiard-btn {
  font-family: 'Oswald', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-radius: 50px; /* Forma de píldora */
  padding: 12px 24px;
  cursor: pointer;
  outline: none;
  border: none;
  transition: all 0.1s ease;
  width: 100%;
  /* Sombra externa sutil */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
}

/* =========================================
   VARIANTES DE COLOR Y EFECTO 3D
   ========================================= */

/* 🔴 PELIGRO */
.tipo-peligro {
  background-color: #ef4444;
  color: #ffffff;
  border-bottom: 5px solid #b91c1c; /* Efecto 3D */
}

/* 🟢 ÉXITO */
.tipo-exito {
  background-color: #22c55e;
  color: #000000;
  border-bottom: 5px solid #15803d;
}

/* ⚪ NEUTRO */
.tipo-neutro {
  background-color: #cbd5e1;
  color: #000000;
  border-bottom: 5px solid #64748b;
}

/* =========================================
   ESTADOS (HOVER, ACTIVE, DISABLED)
   ========================================= */

/* Hover: Se eleva ligeramente */
.billiard-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 8px rgba(0, 0, 0, 0.4);
}

/* Active: Se hunde simulando un clic físico */
.billiard-btn:active:not(:disabled) {
  transform: translateY(2px);
  border-bottom-width: 1px; /* Reduce el borde para dar efecto de hundimiento */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

/* Deshabilitado */
.billiard-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>