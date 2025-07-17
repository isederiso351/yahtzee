package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class GameDto {

    // Getters and Setters
    private Long id;
    private String gameCode;
    private String status;
    private BigDecimal betAmount;
    private Integer maxPlayers;
    private Integer currentRound;
    private Integer maxRounds;
    private BigDecimal totalPrize;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer playersCount;
    private String currentPlayerUsername;
    private String winnerUsername;

}