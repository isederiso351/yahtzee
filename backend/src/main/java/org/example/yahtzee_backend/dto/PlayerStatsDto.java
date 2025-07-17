package org.example.yahtzee_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsDto {

    private Long id;
    private String username;
    private Integer gamesPlayed;
    private Integer gamesWon;
    private Integer gamesLost;
    private Double winRate;
    private BigDecimal totalEarnings;
    private BigDecimal totalLosses;
    private BigDecimal netEarnings;
    private Integer highestScore;
    private BigDecimal balance;
}