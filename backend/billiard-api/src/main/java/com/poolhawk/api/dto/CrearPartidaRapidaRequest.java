package com.poolhawk.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPartidaRapidaRequest(
        @NotBlank @Size(min = 1, max = 20) String jugador1,
        @NotBlank @Size(min = 1, max = 20) String jugador2
) {
}