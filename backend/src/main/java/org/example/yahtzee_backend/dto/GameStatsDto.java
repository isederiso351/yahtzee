package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
public class GameStatsDto {

    // Getters and Setters
    private Long waitingGames;
    private Long activeGames;
    private Long finishedGames;
    private BigDecimal averagePrizePool;
}