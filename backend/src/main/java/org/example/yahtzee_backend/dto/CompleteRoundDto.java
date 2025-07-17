package org.example.yahtzee_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRoundDto {

    @NotBlank(message = "Category required")
    private String category; // YahtzeeCategory name
}