package org.example.yahtzee_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @NotNull(message = "Player is required")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game; // Nullable per transazioni non legate a partite (es. bonus)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Amount must be positive")
    private BigDecimal amount;

    @Column(precision = 20, scale = 2)
    private BigDecimal balanceAfter; // Saldo dopo la transazione

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Costruttori di utilità
    public Transaction(Player player, Game game, TransactionType type, BigDecimal amount, String description) {
        this.player = player;
        this.game = game;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.balanceAfter = player.getBalance();
    }

    public Transaction(Player player, TransactionType type, BigDecimal amount, String description) {
        this(player, null, type, amount, description);
    }

    // Enum per i tipi di transazione
    @Getter
    @AllArgsConstructor
    public enum TransactionType {
        DEPOSIT("Deposito"),
        WITHDRAWAL("Prelievo"),
        BET("Scommessa"),
        WIN("Vincita"),
        LOSE("Perdita"),
        REFUND("Rimborso"),
        BONUS("Bonus"),
        PENALTY("Penalità");

        private final String description;
    }
}