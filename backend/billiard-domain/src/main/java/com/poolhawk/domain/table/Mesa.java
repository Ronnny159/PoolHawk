package com.poolhawk.domain.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Representa una mesa de billar con sus bolas y troneras.
 * ...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * Crea una mesa con dimensiones personalizadas y sus seis troneras...
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

    private static List<Tronera> construirTroneras(double ancho, double alto) {
        List<Tronera> lista = new ArrayList<>(6);
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.TOP_LEFT, new Coordenada(0, alto)));
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.TOP_RIGHT, new Coordenada(ancho, alto)));
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.MIDDLE_LEFT, new Coordenada(0, alto / 2)));
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.MIDDLE_RIGHT, new Coordenada(ancho, alto / 2)));
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.BOTTOM_LEFT, new Coordenada(0, 0)));
        lista.add(Tronera.conRadioPorDefecto(TroneraNombre.BOTTOM_RIGHT, new Coordenada(ancho, 0)));
        return Collections.unmodifiableList(lista);
    }

    public double getAncho() { return ancho; }
    public double getAlto() { return alto; }
    public List<Tronera> getTroneras() { return troneras; }

    /**
     * Retorna todas las bolas.
     * Usamos @JsonSetter para que Jackson sepa que esta es la lista REAL 
     * y pueda llenarla cuando lea el JSON.
     */
    @JsonSetter("bolas")
    public void setBolas(List<Bola> bolas) {
        this.bolas.clear();
        if (bolas != null) {
            this.bolas.addAll(bolas);
        }
    }

    public List<Bola> getBolas() {
        return Collections.unmodifiableList(bolas);
    }

    /**
     * Retorna las bolas en juego.
     * @JsonIgnore evita que Jackson intente modificarla al leer el JSON.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<Bola> getBolasEnMesa() {
        return bolas.stream().filter(Bola::enMesa).toList();
    }

    /**
     * Retorna las bolas embolsadas.
     * @JsonIgnore evita que Jackson intente modificarla al leer el JSON.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<Bola> getBolasEmbolsadas() {
        return bolas.stream().filter(b -> !b.enMesa()).toList();
    }

    public void agregarBola(Bola bola) {
        Objects.requireNonNull(bola, "La bola no puede ser nula");
        boolean yaExiste = bolas.stream().anyMatch(b -> b.numero() == bola.numero());
        if (yaExiste) {
            throw new IllegalArgumentException("Ya existe una bola con el número " + bola.numero() + " en la mesa");
        }
        if (bola.enMesa() && !estaDentroDeLimites(bola.posicion())) {
            throw new IllegalArgumentException("La bola " + bola.numero() + " está fuera de los límites de la mesa. Posición: (" + bola.posicion().x() + ", " + bola.posicion().y() + ")");
        }
        bolas.add(bola);
    }

    public void quitarBola(int numero) {
        boolean removida = bolas.removeIf(b -> b.numero() == numero);
        if (!removida) {
            throw new IllegalArgumentException("No existe una bola con el número " + numero + " en la mesa");
        }
    }

    public void moverBola(int numero, Coordenada nuevaPosicion) {
        Objects.requireNonNull(nuevaPosicion, "La nueva posición no puede ser nula");
        if (!estaDentroDeLimites(nuevaPosicion)) {
            throw new IllegalArgumentException("La nueva posición está fuera de los límites de la mesa. Posición: (" + nuevaPosicion.x() + ", " + nuevaPosicion.y() + ")");
        }
        for (int i = 0; i < bolas.size(); i++) {
            Bola bola = bolas.get(i);
            if (bola.numero() == numero) {
                bolas.set(i, bola.moverA(nuevaPosicion));
                return;
            }
        }
        throw new IllegalArgumentException("No existe una bola con el número " + numero + " en la mesa");
    }

    public void embolsarBola(int numero) {
        for (int i = 0; i < bolas.size(); i++) {
            Bola bola = bolas.get(i);
            if (bola.numero() == numero) {
                bolas.set(i, bola.embolsar());
                return;
            }
        }
        throw new IllegalArgumentException("No existe una bola con el número " + numero + " en la mesa");
    }

    public Optional<Bola> buscarBola(int numero) {
        return bolas.stream().filter(b -> b.numero() == numero).findFirst();
    }

    /**
     * Retorna la bola blanca (número 0).
     * @JsonIgnore para que no intente leerla del JSON (se infiere de las bolas).
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Bola getBolaBlanca() {
        return buscarBola(0).orElseThrow(() -> new IllegalStateException("La bola blanca no está en la mesa"));
    }

    public int contarBolasEnMesa() {
        return (int) bolas.stream().filter(Bola::enMesa).count();
    }

    public boolean quedanBolasDeGrupo(TipoBola tipo) {
        if (!tipo.esDeGrupo()) {
            throw new IllegalArgumentException("El tipo debe ser LISA o RAYADA. Recibido: " + tipo);
        }
        return bolas.stream().anyMatch(b -> b.enMesa() && b.tipo() == tipo);
    }

    public boolean estaDentroDeLimites(Coordenada posicion) {
        Objects.requireNonNull(posicion, "La posición no puede ser nula");
        return posicion.x() >= 0 && posicion.x() <= ancho && posicion.y() >= 0 && posicion.y() <= alto;
    }

    public boolean bolaEnAlgunaTronera(Bola bola) {
        Objects.requireNonNull(bola, "La bola no puede ser nula");
        return troneras.stream().anyMatch(t -> t.captura(bola));
    }

    public Tronera getTroneraMasCercana(Coordenada posicion) {
        Objects.requireNonNull(posicion, "La posición no puede ser nula");
        return troneras.stream()
            .min((t1, t2) -> Double.compare(t1.posicion().distanciaA(posicion), t2.posicion().distanciaA(posicion)))
            .orElseThrow();
    }

    public void limpiar() {
        bolas.clear();
    }
}