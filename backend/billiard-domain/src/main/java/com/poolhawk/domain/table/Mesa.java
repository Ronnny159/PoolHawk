package com.poolhawk.domain.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Representa una mesa de billar con sus bolas y troneras.
 *
 * <p>La mesa es un contenedor mutable: las bolas se agregan, se mueven y
 * se quitan durante el desarrollo de una partida. Sin embargo, las bolas
 * en sí son inmutables (ver {@link Bola}), por lo que mover una bola
 * implica reemplazar la referencia en la lista interna.</p>
 *
 * <p>Las dimensiones por defecto corresponden a una mesa de 9 pies
 * (254 × 127 cm), el estándar de torneos profesionales según la WPA.</p>
 *
 * <p>Esta clase no conoce las reglas del juego. Solo conoce la física
 * básica: qué bolas están en juego, dónde están, y qué troneras existen.
 * Las reglas se implementan en clases aparte (Fase 3).</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class Mesa {

    /** Ancho por defecto de una mesa de 9 pies, en centímetros. */
    public static final double ANCHO_DEFECTO = 254.0;

    /** Alto por defecto de una mesa de 9 pies, en centímetros. */
    public static final double ALTO_DEFECTO = 127.0;

    private final double ancho;
    private final double alto;
    private final List<Tronera> troneras;
    private final List<Bola> bolas;

    /**
     * Crea una mesa con dimensiones personalizadas y sus seis troneras
     * ubicadas automáticamente en las posiciones estándar.
     *
     * @param ancho ancho de la mesa en centímetros
     * @param alto  alto de la mesa en centímetros
     * @throws IllegalArgumentException si el ancho o el alto no son positivos
     */
    public Mesa(double ancho, double alto) {
        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException(
                "Las dimensiones de la mesa deben ser positivas. " +
                "Recibido: ancho=" + ancho + ", alto=" + alto
            );
        }
        this.ancho = ancho;
        this.alto = alto;
        this.troneras = construirTroneras(ancho, alto);
        this.bolas = new ArrayList<>();
    }

    /**
     * Crea una mesa con las dimensiones estándar de 9 pies (254 × 127 cm).
     */
    public Mesa() {
        this(ANCHO_DEFECTO, ALTO_DEFECTO);
    }

    /**
     * Construye las seis troneras estándar de una mesa de billar.
     *
     * @param ancho ancho de la mesa
     * @param alto  alto de la mesa
     * @return lista inmutable de troneras
     */
    private static List<Tronera> construirTroneras(double ancho, double alto) {
        List<Tronera> lista = new ArrayList<>(6);
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.TOP_LEFT, new Coordenada(0, alto)));
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.TOP_RIGHT, new Coordenada(ancho, alto)));
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.MIDDLE_LEFT, new Coordenada(0, alto / 2)));
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.MIDDLE_RIGHT, new Coordenada(ancho, alto / 2)));
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.BOTTOM_LEFT, new Coordenada(0, 0)));
        lista.add(Tronera.conRadioPorDefecto(
            TroneraNombre.BOTTOM_RIGHT, new Coordenada(ancho, 0)));
        return Collections.unmodifiableList(lista);
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Retorna el ancho de la mesa en centímetros.
     *
     * @return ancho
     */
    public double getAncho() {
        return ancho;
    }

    /**
     * Retorna el alto de la mesa en centímetros.
     *
     * @return alto
     */
    public double getAlto() {
        return alto;
    }

    /**
     * Retorna una lista inmutable con las seis troneras de la mesa.
     *
     * @return troneras
     */
    public List<Tronera> getTroneras() {
        return troneras;
    }

    /**
     * Retorna una lista inmutable con todas las bolas (en mesa o embolsadas).
     *
     * @return todas las bolas
     */
    public List<Bola> getBolas() {
        return Collections.unmodifiableList(bolas);
    }

    /**
     * Retorna una lista inmutable con las bolas que aún están en la mesa.
     *
     * @return bolas en juego
     */
    public List<Bola> getBolasEnMesa() {
        return bolas.stream()
            .filter(Bola::enMesa)
            .toList();
    }

    /**
     * Retorna una lista inmutable con las bolas embolsadas.
     *
     * @return bolas fuera de juego
     */
    public List<Bola> getBolasEmbolsadas() {
        return bolas.stream()
            .filter(b -> !b.enMesa())
            .toList();
    }

    // ═══════════════════════════════════════════════════════════
    // OPERACIONES SOBRE BOLAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Agrega una bola a la mesa.
     *
     * <p>La bola debe estar dentro de los límites de la mesa. No se
     * permite agregar dos bolas con el mismo número.</p>
     *
     * @param bola bola a agregar
     * @throws NullPointerException     si la bola es nula
     * @throws IllegalArgumentException si ya existe una bola con ese número
     *                                  o si la bola está fuera de la mesa
     */
    public void agregarBola(Bola bola) {
        Objects.requireNonNull(bola, "La bola no puede ser nula");

        boolean yaExiste = bolas.stream()
            .anyMatch(b -> b.numero() == bola.numero());
        if (yaExiste) {
            throw new IllegalArgumentException(
                "Ya existe una bola con el número " + bola.numero() + " en la mesa"
            );
        }

        if (bola.enMesa() && !estaDentroDeLimites(bola.posicion())) {
            throw new IllegalArgumentException(
                "La bola " + bola.numero() + " está fuera de los límites de la mesa. " +
                "Posición: (" + bola.posicion().x() + ", " + bola.posicion().y() + ")"
            );
        }

        bolas.add(bola);
    }

    /**
     * Quita una bola de la mesa (por ejemplo, al final de la partida).
     *
     * @param numero número de la bola a quitar
     * @throws IllegalArgumentException si no existe una bola con ese número
     */
    public void quitarBola(int numero) {
        boolean removida = bolas.removeIf(b -> b.numero() == numero);
        if (!removida) {
            throw new IllegalArgumentException(
                "No existe una bola con el número " + numero + " en la mesa"
            );
        }
    }

    /**
     * Mueve una bola a una nueva posición.
     *
     * <p>Como las bolas son inmutables, este método reemplaza la referencia
     * por una nueva instancia con la posición actualizada.</p>
     *
     * @param numero        número de la bola a mover
     * @param nuevaPosicion nueva posición
     * @throws IllegalArgumentException si no existe la bola o si la nueva
     *                                  posición está fuera de los límites
     * @throws NullPointerException     si la posición es nula
     */
    public void moverBola(int numero, Coordenada nuevaPosicion) {
        Objects.requireNonNull(nuevaPosicion, "La nueva posición no puede ser nula");

        if (!estaDentroDeLimites(nuevaPosicion)) {
            throw new IllegalArgumentException(
                "La nueva posición está fuera de los límites de la mesa. " +
                "Posición: (" + nuevaPosicion.x() + ", " + nuevaPosicion.y() + ")"
            );
        }

        for (int i = 0; i < bolas.size(); i++) {
            Bola bola = bolas.get(i);
            if (bola.numero() == numero) {
                bolas.set(i, bola.moverA(nuevaPosicion));
                return;
            }
        }

        throw new IllegalArgumentException(
            "No existe una bola con el número " + numero + " en la mesa"
        );
    }

    /**
     * Marca una bola como embolsada.
     *
     * @param numero número de la bola
     * @throws IllegalArgumentException si no existe la bola
     */
    public void embolsarBola(int numero) {
        for (int i = 0; i < bolas.size(); i++) {
            Bola bola = bolas.get(i);
            if (bola.numero() == numero) {
                bolas.set(i, bola.embolsar());
                return;
            }
        }
        throw new IllegalArgumentException(
            "No existe una bola con el número " + numero + " en la mesa"
        );
    }

    /**
     * Busca una bola por su número.
     *
     * @param numero número de la bola
     * @return la bola si existe, o {@link Optional#empty()} si no
     */
    public Optional<Bola> buscarBola(int numero) {
        return bolas.stream()
            .filter(b -> b.numero() == numero)
            .findFirst();
    }

    /**
     * Retorna la bola blanca (número 0).
     *
     * @return la bola blanca
     * @throws IllegalStateException si la blanca no está en la mesa
     */
    public Bola getBolaBlanca() {
        return buscarBola(0)
            .orElseThrow(() -> new IllegalStateException(
                "La bola blanca no está en la mesa"));
    }

    /**
     * Cuenta cuántas bolas están actualmente en la mesa.
     *
     * @return número de bolas en juego
     */
    public int contarBolasEnMesa() {
        return (int) bolas.stream().filter(Bola::enMesa).count();
    }

    /**
     * Verifica si quedan bolas de un grupo (lisas o rayadas) en la mesa.
     *
     * @param tipo tipo de bola (debe ser LISA o RAYADA)
     * @return {@code true} si quedan bolas de ese grupo en juego
     * @throws IllegalArgumentException si el tipo no es de grupo
     */
    public boolean quedanBolasDeGrupo(TipoBola tipo) {
        if (!tipo.esDeGrupo()) {
            throw new IllegalArgumentException(
                "El tipo debe ser LISA o RAYADA. Recibido: " + tipo
            );
        }
        return bolas.stream()
            .anyMatch(b -> b.enMesa() && b.tipo() == tipo);
    }

    /**
     * Verifica si una posición está dentro de los límites de la mesa.
     *
     * @param posicion posición a verificar
     * @return {@code true} si está dentro
     */
    public boolean estaDentroDeLimites(Coordenada posicion) {
        Objects.requireNonNull(posicion, "La posición no puede ser nula");
        return posicion.x() >= 0 && posicion.x() <= ancho
            && posicion.y() >= 0 && posicion.y() <= alto;
    }

    /**
     * Verifica si una bola está dentro del radio de captura de alguna tronera.
     *
     * @param bola bola a verificar
     * @return {@code true} si la bola cayó en alguna tronera
     */
    public boolean bolaEnAlgunaTronera(Bola bola) {
        Objects.requireNonNull(bola, "La bola no puede ser nula");
        return troneras.stream().anyMatch(t -> t.captura(bola));
    }

    /**
     * Retorna la tronera más cercana a una posición dada.
     *
     * @param posicion posición de referencia
     * @return la tronera más cercana
     */
    public Tronera getTroneraMasCercana(Coordenada posicion) {
        Objects.requireNonNull(posicion, "La posición no puede ser nula");
        return troneras.stream()
            .min((t1, t2) -> Double.compare(
                t1.posicion().distanciaA(posicion),
                t2.posicion().distanciaA(posicion)))
            .orElseThrow();
    }

    /**
     * Elimina todas las bolas de la mesa.
     */
    public void limpiar() {
        bolas.clear();
    }
}