<script setup>
import { ref, computed } from 'vue'
import { crearPartida } from '../api/matches'

const emit = defineEmits(['partidaIniciada'])

const modalidad = ref('BOLA_8')
const formato = ref('INDIVIDUAL') // INDIVIDUAL o PAREJAS
const tamanoMesa = ref('9')
const mostrarConfig = ref(false) // Controla el engranaje

// Jugadores (soportamos hasta 4 para el 1v1v1v1)
const jugador1 = ref('')
const jugador2 = ref('')
const jugador3 = ref('')
const jugador4 = ref('')

const cargando = ref(false)
const error = ref('')

const medidasMesa = {
  '7': { ancho: 198, alto: 99 },
  '8': { ancho: 224, alto: 112 },
  '9': { ancho: 254, alto: 127 }
}

// Calculamos cuántos inputs mostrar
const cantidadJugadores = computed(() => {
  if (formato.value === 'PAREJAS') return 4
  // Si es individual, pero eligen "1v1v1v1" (lo manejamos como individual con 4 jugadores)
  if (jugador3.value !== '' || jugador4.value !== '') return 4
  return 2
})

const crear = async () => {
  try {
    cargando.value = true
    error.value = ''

    // Recolectamos solo los nombres que no estén vacíos
    let nombres = [jugador1.value, jugador2.value]
    if (formato.value === 'PAREJAS' || jugador3.value || jugador4.value) {
      nombres.push(jugador3.value, jugador4.value)
    }

    if (nombres.some(n => !n.trim())) {
      throw new Error('Todos los nombres de los jugadores seleccionados son obligatorios')
    }

    const medidas = medidasMesa[tamanoMesa.value]

    // Ajustamos el payload al nuevo DTO del backend (lista plana)
    const partida = await crearPartida({
      modalidad: modalidad.value,
      formato: formato.value,
      jugadores: nombres,
      anchoMesa: medidas.ancho,
      altoMesa: medidas.alto
    })

    emit('partidaIniciada', partida)
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="mesa-wrapper">
    <div class="form-container">
      
      <!-- ENGRANAJE DE CONFIGURACIÓN -->
      <div class="config-icon" @click="mostrarConfig = !mostrarConfig" title="Configuración de mesa">
        ⚙️
      </div>

      <!-- MENÚ DESPLEGABLE DE CONFIGURACIÓN -->
      <div v-if="mostrarConfig" class="config-panel">
        <h4>Configuración Avanzada</h4>
        
        <div class="config-item">
          <label>Tamaño de mesa:</label>
          <div class="opciones-mesa">
            <button :class="{ active: tamanoMesa === '7' }" @click="tamanoMesa = '7'">7p</button>
            <button :class="{ active: tamanoMesa === '8' }" @click="tamanoMesa = '8'">8p</button>
            <button :class="{ active: tamanoMesa === '9' }" @click="tamanoMesa = '9'">9p</button>
          </div>
        </div>

        <div class="config-item">
          <label>Jugadores:</label>
          <div class="opciones-mesa">
            <button :class="{ active: formato === 'INDIVIDUAL' && cantidadJugadores === 2 }" @click="formato = 'INDIVIDUAL'; jugador3 = ''; jugador4 = ''">1 vs 1</button>
            <button :class="{ active: formato === 'INDIVIDUAL' && cantidadJugadores === 4 }" @click="formato = 'INDIVIDUAL'; jugador3 = 'J3'; jugador4 = 'J4'">Todos vs Todos (4)</button>
            <button :class="{ active: formato === 'PAREJAS' }" @click="formato = 'PAREJAS'">2 vs 2</button>
          </div>
        </div>
      </div>

      <!-- CABECERA -->
      <div class="header-form">
        <h2>NUEVA PARTIDA</h2>
        <p class="subtitulo-form">Configura tu mesa y prepara el taco</p>

        <div class="selector-modalidad">
          <button :class="{ active: modalidad === 'BOLA_8' }" @click="modalidad = 'BOLA_8'">Bola 8</button>
          <button :class="{ active: modalidad === 'POR_PUNTOS' }" @click="modalidad = 'POR_PUNTOS'">Por Puntos</button>
        </div>
      </div>

      <!-- BOLAS / EQUIPOS -->
      <div class="equipos-container">
        <!-- BOLA 1 (Amarilla) -->
        <div class="bola-wrapper">
          <div class="bola-equipo equipo-a">
            <div class="brillo"></div>
            <div class="centro-blanco">
              <label>Jugador 1</label>
              <input v-model="jugador1" placeholder="Nombre" maxlength="12" />
              <input v-if="formato === 'PAREJAS' || cantidadJugadores === 4" v-model="jugador3" placeholder="Jugador 3" maxlength="12" />
            </div>
          </div>
        </div>

        <div class="vs-badge">VS</div>

        <!-- BOLA 2 (Azul) -->
        <div class="bola-wrapper">
          <div class="bola-equipo equipo-b">
            <div class="brillo"></div>
            <div class="centro-blanco">
              <label>Jugador 2</label>
              <input v-model="jugador2" placeholder="Nombre" maxlength="12" />
              <input v-if="formato === 'PAREJAS' || cantidadJugadores === 4" v-model="jugador4" placeholder="Jugador 4" maxlength="12" />
            </div>
          </div>
        </div>
      </div>

      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <button class="btn-iniciar" @click="crear" :disabled="cargando">
        {{ cargando ? 'CREANDO...' : 'INICIAR PARTIDA' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   ESTILOS ORIGINALES (Intactos)
   ============================================================ */
.mesa-wrapper {
  max-width: 920px;
  margin: 0 auto;
  padding: 10px;
  border-radius: 26px;
  background: linear-gradient(145deg, #0a0f1a 0%, #1a2332 50%, #0a0f1a 100%);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.85), 0 10px 20px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.form-container {
  position: relative;
  background: radial-gradient(ellipse at 50% 0%, rgba(34, 197, 94, 0.14) 0%, transparent 55%), linear-gradient(160deg, #1e293b 0%, #0f172a 100%);
  padding: 3rem 2.5rem 2.5rem;
  border-radius: 18px;
  color: var(--text-main);
  border: 1px solid #334155;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05), inset 0 -40px 80px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}

.header-form { text-align: center; margin-bottom: 2.5rem; position: relative; z-index: 1; }
.header-form h2 { font-size: 3rem; margin: 0; color: var(--text-main); text-shadow: 0 2px 0 #000, 0 4px 12px rgba(0, 0, 0, 0.7); letter-spacing: 3px; }
.subtitulo-form { margin: 0.4rem 0 0; color: var(--text-muted); font-size: 0.95rem; font-family: 'Inter', sans-serif; }

.selector-modalidad {
  display: inline-flex; background: #0a0f1a; padding: 6px; border-radius: 30px; margin-top: 1.2rem; border: 1px solid #1e293b;
  box-shadow: inset 0 3px 6px rgba(0, 0, 0, 0.6), 0 1px 0 rgba(255, 255, 255, 0.04);
}
.selector-modalidad button {
  background: transparent; color: var(--text-muted); border: none; padding: 10px 28px; border-radius: 25px;
  font-family: 'Inter', sans-serif; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: all 0.25s ease;
}
.selector-modalidad button.active {
  background: linear-gradient(180deg, #22c55e 0%, #15803d 100%); color: #ffffff;
  box-shadow: 0 3px 10px rgba(21, 128, 61, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.equipos-container { display: flex; gap: 1rem; justify-content: center; align-items: center; margin-bottom: 2.5rem; flex-wrap: wrap; position: relative; z-index: 1; }
.vs-badge { font-family: 'Bebas Neue', sans-serif; font-size: 2rem; color: #475569; letter-spacing: 2px; }
.bola-wrapper { position: relative; }
.bola-equipo {
  width: 280px; height: 280px; border-radius: 50%; display: flex; justify-content: center; align-items: center; position: relative;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.45s ease;
  box-shadow: inset -22px -22px 55px rgba(0, 0, 0, 0.7), inset 18px 18px 35px rgba(255, 255, 255, 0.35), 0 22px 40px rgba(0, 0, 0, 0.7);
}
.bola-equipo:hover { transform: translateY(-10px) scale(1.02); }
.brillo { position: absolute; top: 12%; left: 18%; width: 92px; height: 68px; background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.75) 0%, rgba(255, 255, 255, 0) 70%); border-radius: 50%; filter: blur(5px); }
.equipo-a { background: radial-gradient(circle at 30% 25%, #fef9c3 0%, var(--ball-yellow) 42%, #ca8a04 78%, #713f12 100%); }
.equipo-b { background: radial-gradient(circle at 30% 25%, #dbeafe 0%, var(--ball-blue) 42%, #1d4ed8 78%, #1e3a8a 100%); }

.centro-blanco {
  width: 168px; height: 168px; background: radial-gradient(circle at 38% 30%, #ffffff 0%, #f1f5f9 65%, #dbe3ec 100%);
  border-radius: 50%; display: flex; flex-direction: column; justify-content: center; align-items: center; padding: 0.75rem; z-index: 10;
  box-shadow: inset 3px 3px 10px rgba(0, 0, 0, 0.18), inset -2px -2px 6px rgba(255, 255, 255, 0.9), 0 3px 8px rgba(0, 0, 0, 0.35);
}
.centro-blanco label { color: #0f172a; font-family: 'Bebas Neue', sans-serif; font-size: 1.05rem; margin-bottom: 0.45rem; }
.centro-blanco input {
  width: 128px; padding: 0.42rem; margin-bottom: 0.35rem; border: 2px solid #cbd5e1; border-radius: 7px; text-align: center;
  font-family: 'Inter', sans-serif; font-weight: 700; font-size: 0.9rem; color: #0f172a; outline: none;
}
.centro-blanco input:focus { border-color: var(--felt-light); box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.22); }

.btn-iniciar {
  display: block; width: 100%; max-width: 420px; margin: 0 auto; padding: 1.15rem; background: linear-gradient(180deg, #22c55e 0%, #15803d 100%);
  color: white; font-family: 'Bebas Neue', sans-serif; font-size: 1.55rem; letter-spacing: 3px; border: none; border-radius: 12px; cursor: pointer;
  box-shadow: 0 6px 0 #0f5132, 0 10px 22px rgba(0, 0, 0, 0.55);
}
.btn-iniciar:active { transform: translateY(4px); box-shadow: 0 2px 0 #0f5132; }

.error { color: #fecaca; background: rgba(239, 68, 68, 0.12); border-left: 4px solid var(--ball-red); padding: 0.85rem; border-radius: 6px; text-align: left; margin: 0 auto 1.5rem; max-width: 420px; }

/* ============================================================
   NUEVOS ESTILOS (Engranaje y Panel Flotante)
   ============================================================ */
.config-icon {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  font-size: 1.8rem;
  cursor: pointer;
  z-index: 100;
  transition: transform 0.3s ease;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
}
.config-icon:hover { transform: rotate(90deg); }

.config-panel {
  position: absolute;
  top: 4.5rem;
  right: 1.5rem;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 1.5rem;
  z-index: 99;
  box-shadow: 0 10px 30px rgba(0,0,0,0.8);
  width: 280px;
}

.config-panel h4 { margin: 0 0 1rem 0; color: #94a3b8; font-family: 'Inter', sans-serif; font-size: 0.9rem; text-transform: uppercase; letter-spacing: 1px; }
.config-item { margin-bottom: 1rem; display: flex; flex-direction: column; gap: 0.5rem; }
.config-item label { color: #cbd5e1; font-size: 0.85rem; font-family: 'Inter', sans-serif; }

.opciones-mesa { display: flex; gap: 0.5rem; }
.opciones-mesa button {
  flex: 1; background: #1e293b; color: #94a3b8; border: 1px solid #334155; padding: 6px 10px; border-radius: 6px;
  font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.opciones-mesa button.active { background: #22c55e; color: #fff; border-color: #22c55e; }
</style>