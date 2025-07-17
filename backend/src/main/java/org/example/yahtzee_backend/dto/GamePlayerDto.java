package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@NoArgsConstructor
public class GamePlayerDto {

    // Getters and Setters
    private Long playerId;
    private String username;
    private Integer joinOrder;
    private Integer totalScore;
    private Boolean isActive;
    private LocalDateTime joinedAt;

}