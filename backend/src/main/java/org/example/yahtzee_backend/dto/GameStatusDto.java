package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class GameStatusDto {

    private GameDto game;
    private String currentPlayerUsername;
    private Boolean isMyTurn;
    private Integer playersWhoCompletedCurrentRound;
    private Integer totalActivePlayers;
    private Boolean canStartGame;
    private Boolean canAdvanceRound;
    private LocalDateTime lastActivity;
}