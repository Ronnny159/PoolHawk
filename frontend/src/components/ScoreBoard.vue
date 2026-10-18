<script setup>
defineProps({
  partida: { type: Object, required: true }
})
</script>

<template>
  <div class="scoreboard">
    <div class="header">
      <h2>{{ partida.modalidad === 'BOLA_8' ? 'Bola 8' : 'Por Puntos' }}</h2>
      <span class="fase">{{ partida.fase }}</span>
    </div>

    <div class="jugadores">
      <div class="jugador" :class="{ activo: partida.jugadorActual === partida.equipoA[0].nombre }">
        <span class="nombre">{{ partida.equipoA[0].nombre }}</span>
        <span class="puntos">{{ partida.puntajes[partida.equipoA[0].nombre] || 0 }}</span>
      </div>
      <div class="vs">VS</div>
      <div class="jugador" :class="{ activo: partida.jugadorActual === partida.equipoB[0].nombre }">
        <span class="nombre">{{ partida.equipoB[0].nombre }}</span>
        <span class="puntos">{{ partida.puntajes[partida.equipoB[0].nombre] || 0 }}</span>
      </div>
    </div>

    <div class="turno">
      Turno de: <strong>{{ partida.jugadorActual }}</strong>
    </div>
  </div>
</template>

<style scoped>
.scoreboard { background: #1e1e1e; border-radius: 12px; padding: 2rem; color: white; max-width: 800px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; margin-bottom: 2rem; }
h2 { margin: 0; }
.fase { background: #4ade80; color: #000; padding: 0.2rem 0.8rem; border-radius: 20px; font-weight: bold; font-size: 0.8rem; }
.jugadores { display: flex; align-items: center; justify-content: space-between; gap: 2rem; }
.jugador { flex: 1; text-align: center; padding: 1.5rem; background: #2a2a2a; border-radius: 12px; border: 2px solid transparent; transition: all 0.3s; }
.jugador.activo { border-color: #4ade80; background: #2a3a2a; }
.nombre { display: block; font-size: 1.2rem; color: #aaa; margin-bottom: 0.5rem; }
.puntos { display: block; font-size: 4rem; font-weight: bold; color: #4ade80; }
.vs { font-size: 1.5rem; font-weight: bold; color: #555; }
.turno { text-align: center; margin-top: 2rem; font-size: 1.2rem; color: #aaa; }
.turno strong { color: #4ade80; }
</style>