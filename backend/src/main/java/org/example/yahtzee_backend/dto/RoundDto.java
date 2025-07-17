package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class RoundDto {

    // Getters and Setters
    private Long id;
    private Integer roundNumber;
    private List<String> diceRolls; // es. ["12345", "23456"]
    private Integer rollCount;
    private Boolean canRollAgain;
    private Boolean isCompleted;
    private Integer score;
    private String selectedCategory;
}