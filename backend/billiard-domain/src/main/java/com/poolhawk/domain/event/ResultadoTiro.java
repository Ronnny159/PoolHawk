package com.poolhawk.domain.event;

import com.poolhawk.domain.match.Grupo;
import java.util.Objects;

public record ResultadoTiro(
    boolean esFalta,
    FaltaMotivo motivo,
    int puntos,
    boolean cambiaTurno,
    boolean esVictoria,
    boolean esDerrota,
    Grupo grupoAsignado,
    String descripcion
) {

    public ResultadoTiro {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede ser nula ni vacía");
        }
        if (esFalta && motivo == null) {
            throw new IllegalArgumentException("Si es falta, debe especificarse el motivo");
        }
        if (!esFalta && !esDerrota && motivo != null) {
            throw new IllegalArgumentException("Si no es falta ni derrota, el motivo debe ser nulo");
        }
        if ((esFalta || esDerrota) && puntos != 0) {
            throw new IllegalArgumentException("Si es falta o derrota, los puntos deben ser 0");
        }
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        }
    }

    public static ResultadoTiro tiroLegalSinPuntos(boolean cambiaTurno) {
        return new ResultadoTiro(false, null, 0, cambiaTurno, false, false, null, "Tiro legal sin embolsar bolas");
    }

    public static ResultadoTiro tiroLegalConPuntos(int puntos) {
        return new ResultadoTiro(false, null, puntos, false, false, false, null, "Tiro legal, " + puntos + " punto(s)");
    }

    public static ResultadoTiro tiroLegalConPuntos(int puntos, Grupo grupoAsignado) {
        return new ResultadoTiro(false, null, puntos, false, false, false, grupoAsignado, "Tiro legal, " + puntos + " punto(s)");
    }

    public static ResultadoTiro falta(FaltaMotivo motivo) {
        Objects.requireNonNull(motivo, "El motivo de la falta no puede ser nulo");
        return new ResultadoTiro(true, motivo, 0, true, false, false, null, "Falta: " + motivo.getDescripcion());
    }

    public static ResultadoTiro victoria(int puntos) {
        return new ResultadoTiro(false, null, puntos, true, true, false, null, "¡Victoria! Se ganó la partida");
    }

    public static ResultadoTiro derrota(FaltaMotivo motivo) {
        Objects.requireNonNull(motivo, "El motivo de la derrota no puede ser nulo");
        return new ResultadoTiro(false, motivo, 0, true, false, true, null, "Derrota: " + motivo.getDescripcion());
    }

    public boolean fueExitoso() {
        return !esFalta && !esVictoria && !esDerrota && puntos > 0;
    }
}