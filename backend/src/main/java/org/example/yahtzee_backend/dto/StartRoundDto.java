package org.example.yahtzee_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class StartRoundDto {

    @NotNull(message = "Round number required")
    @Min(value = 1, message = "Min round 1")
    @Max(value = 13, message = "Max round 13")
    private Integer roundNumber;
}