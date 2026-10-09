package main.java.com.poolhawk.domain.table;

import java.util.Objects;

/**
 * Representa una posición bidimensional sobre la mesa de billar.
 *
 * <p>El sistema de coordenadas se define en centímetros, con el origen
 * {@code (0, 0)} en la esquina inferior izquierda de la mesa vista desde
 * la cámara cenital. El eje X crece hacia la derecha y el eje Y hacia
 * arriba.</p>
 *
 * <p>Esta clase es inmutable: una vez creada, sus valores no cambian.
 * Esto permite usarla como clave en mapas, evita efectos colaterales y
 * garantiza seguridad entre hilos de ejecución.</p>
 *
 * @param x posición horizontal en centímetros
 * @param y posición vertical en centímetros
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record Coordenada(double x, double y) {

    /**
     * Constructor compacto que valida los valores de entrada.
     *
     * <p>Se rechazan valores no finitos ({@code NaN} o infinitos) porque
     * una coordenada debe representar siempre una posición física real
     * sobre la mesa.</p>
     *
     * @throws IllegalArgumentException si algún valor no es finito
     */
    public Coordenada {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException(
                "La coordenada debe contener valores finitos. " +
                "Valores recibidos: x=" + x + ", y=" + y
            );
        }
    }

    /**
     * Calcula la distancia euclidiana entre esta coordenada y otra.
     *
     * <p>Se usa la fórmula clásica: {@code √((x₂-x₁)² + (y₂-y₁)²)}.</p>
     *
     * @param otra coordenada destino
     * @return distancia en centímetros, siempre mayor o igual a cero
     * @throws NullPointerException si {@code otra} es {@code null}
     */
    public double distanciaA(Coordenada otra) {
        Objects.requireNonNull(otra, "La coordenada destino no puede ser nula");
        double dx = this.x - otra.x;
        double dy = this.y - otra.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Verifica si esta coordenada está dentro de un radio determinado
     * respecto a otra.
     *
     * <p>Útil para detectar colisiones entre bolas o proximidad a una
     * tronera. Un valor típico de radio para una bola de billar estándar
     * es de aproximadamente 2.85 cm (diámetro 5.7 cm).</p>
     *
     * @param otra coordenada centro del radio
     * @param radio radio de tolerancia en centímetros, debe ser positivo
     * @return {@code true} si la distancia es menor o igual al radio
     * @throws NullPointerException si {@code otra} es {@code null}
     * @throws IllegalArgumentException si {@code radio} es negativo
     */
    public boolean estaCercaDe(Coordenada otra, double radio) {
        Objects.requireNonNull(otra, "La coordenada centro no puede ser nula");
        if (radio < 0) {
            throw new IllegalArgumentException(
                "El radio no puede ser negativo. Valor recibido: " + radio
            );
        }
        return distanciaA(otra) <= radio;
    }

    /**
     * Crea una nueva coordenada desplazada por un vector dado.
     *
     * <p>Como esta clase es inmutable, el desplazamiento devuelve una
     * nueva instancia en lugar de modificar la actual.</p>
     *
     * @param deltaX desplazamiento horizontal en centímetros
     * @param deltaY desplazamiento vertical en centímetros
     * @return nueva coordenada desplazada
     */
    public Coordenada desplazar(double deltaX, double deltaY) {
        return new Coordenada(this.x + deltaX, this.y + deltaY);
    }
}
