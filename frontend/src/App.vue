<script setup>
  import { onMounted } from 'vue'
  import { crearPartida } from './api/matches'

  onMounted(async () => {
    console.log('Conectando con el backend...')
    try {
      const nuevaPartida = await crearPartida({
        modalidad: 'BOLA_8',
        equipoA: ['Ana'],
        equipoB: ['Beto']
      })
      console.log('¡Conexión exitosa! Partida creada:', nuevaPartida)
    } catch (error) {
      console.error('Fallo al conectar con el backend. ¿Está Spring Boot corriendo?')
    }
  })
</script>

<template>
  <div style="padding: 20px;">
    <h1>PoolHawk Frontend</h1>
    <p>Abre la consola del navegador (F12) para ver los resultados.</p>
  </div>
</template>