package org.example.yahtzee_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfileDto {
    private Long id;
    private String username;
    private String email;
    private BigDecimal balance;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private LocalDateTime lastActivity;
}