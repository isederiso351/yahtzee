package org.example.yahtzee_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameDto {

    @NotBlank(message = "Game code is required")
    @Size(min = 6, max = 10, message = "Game code must be between 6 and 10 characters")
    private String gameCode;
}