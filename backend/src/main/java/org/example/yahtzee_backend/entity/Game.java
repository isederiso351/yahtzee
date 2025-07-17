package org.example.yahtzee_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String gameCode; // Codice univoco per identificare la partita

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Game status is required")
    private GameStatus status = GameStatus.WAITING;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "Bet amount must be positive")
    private BigDecimal betAmount;

    @Column(nullable = false)
    private Integer maxPlayers = 2; // Default 2 giocatori

    @Column(nullable = false)
    private Integer currentRound = 1;

    @Column(nullable = false)
    private Integer maxRounds = 13; // Yahtzee standard ha 13 round

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_player_id")
    private Player currentPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner; // Vincitore della partita

    @Column(precision = 20, scale = 2)
    private BigDecimal totalPrize; // Premio totale (somma delle scommesse)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GamePlayer> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GameRound> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Metodi di utilità
    public void addPlayer(Player player) {
        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setGame(this);
        gamePlayer.setPlayer(player);
        gamePlayer.setJoinOrder(this.players.size() + 1);
        this.players.add(gamePlayer);

        // Calcola il prize pool
        this.totalPrize = this.betAmount.multiply(BigDecimal.valueOf(this.players.size()));
    }

    public boolean isFull() {
        return this.players.size() >= this.maxPlayers;
    }

    public boolean canStart() {
        return this.players.size() >= 2 && this.status == GameStatus.WAITING;
    }

    public boolean isActive() {
        return this.status == GameStatus.IN_PROGRESS;
    }

    public int getPlayersCount() {
        return this.players.size();
    }

    // Enum per lo stato del gioco
    @Getter
    @AllArgsConstructor
    public enum GameStatus {
        WAITING("In attesa di giocatori"),
        IN_PROGRESS("In corso"),
        FINISHED("Terminata"),
        CANCELLED("Annullata");

        private final String description;
    }
}