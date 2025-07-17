package org.example.yahtzee_backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameDto {

    @NotNull(message = "Bet amount is required")
    @DecimalMin(value = "0.01", message = "Bet amount must be positive")
    private BigDecimal betAmount;

    @NotNull(message = "Max players is required")
    @Min(value = 2, message = "Minimum 2 players required")
    @Max(value = 6, message = "Maximum 6 players allowed")
    private Integer maxPlayers;
}