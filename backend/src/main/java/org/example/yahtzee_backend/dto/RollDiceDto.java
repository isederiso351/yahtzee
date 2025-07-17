package org.example.yahtzee_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class RollDiceDto {

    private boolean[] keepDice; // 5 boolean per dadi da tenere
}