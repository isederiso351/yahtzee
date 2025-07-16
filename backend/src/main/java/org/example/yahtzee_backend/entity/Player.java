package org.example.yahtzee_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
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
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Column(nullable = false, precision = 20, scale = 2)
    @Digits(integer = 18, fraction = 2)
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    private BigDecimal balance = BigDecimal.valueOf(1000.00); // Saldo iniziale

    // Statistiche giocatore
    @Column(nullable = false)
    private Integer gamesPlayed = 0;

    @Column(nullable = false)
    private Integer gamesWon = 0;

    @Column(nullable = false)
    private Integer gamesLost = 0;

    @Column(precision = 20, scale = 2)
    @Digits(integer = 18, fraction = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(precision = 20, scale = 2)
    @Digits(integer = 18, fraction = 2)
    private BigDecimal totalLosses = BigDecimal.ZERO;

    @Column
    private Integer highestScore = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLogin; // Ultimo accesso

    @Column
    private LocalDateTime lastActivity; // Ultima attività (gioco, transazione, etc.)

    // Relazioni (le implementeremo dopo)
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "winner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Game> gamesWonAsList = new ArrayList<>();

    // Metodi di utilità
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void subtractFromBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    public boolean hasEnoughBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
    }

    public void incrementGamesLost() {
        this.gamesLost++;
    }

    public Double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return (double) gamesWon / gamesPlayed * 100;
    }

    public BigDecimal getNetEarnings() {
        return totalEarnings.subtract(totalLosses);
    }

    public void updateLastLogin(LocalDateTime lastlogin) {
        this.lastLogin = lastlogin;
        this.lastActivity = lastlogin;
    }

    public void updateLastActivity(LocalDateTime lastactivity) {
        this.lastActivity = lastactivity;
    }
}
