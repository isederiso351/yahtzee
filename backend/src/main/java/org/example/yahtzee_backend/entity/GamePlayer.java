package org.example.yahtzee_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @NotNull(message = "Player is required")
    private Player player;

    @Column(nullable = false)
    @Min(value = 1, message = "Join order must be positive")
    private Integer joinOrder; // Ordine di ingresso nella partita

    @Column(nullable = false)
    private Integer totalScore = 0; // Punteggio totale del giocatore in questa partita

    @Column(nullable = false)
    private Boolean isActive = true; // Se il giocatore è ancora attivo nella partita

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // Metodi di utilità
    public void addScore(int score) {
        this.totalScore += score;
    }
}
