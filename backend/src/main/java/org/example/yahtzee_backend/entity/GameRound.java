package org.example.yahtzee_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {

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
    @Min(value = 1, message = "Round number must be positive")
    @Max(value = 13, message = "Round number cannot exceed 13")
    private Integer roundNumber;

    @Column(nullable = false)
    @Min(value = 1, message = "Roll number must be positive")
    @Max(value = 3, message = "Roll number cannot exceed 3")
    private Integer rollNumber;

    // I 5 dadi (valori da 1 a 6)
    @Column(nullable = false)
    @Min(value = 1) @Max(value = 6)
    private Integer dice1;

    @Column(nullable = false)
    @Min(value = 1) @Max(value = 6)
    private Integer dice2;

    @Column(nullable = false)
    @Min(value = 1) @Max(value = 6)
    private Integer dice3;

    @Column(nullable = false)
    @Min(value = 1) @Max(value = 6)
    private Integer dice4;

    @Column(nullable = false)
    @Min(value = 1) @Max(value = 6)
    private Integer dice5;

    // Dadi che il giocatore vuole tenere per il prossimo lancio
    @Column(nullable = false)
    private Boolean keepDice1 = false;

    @Column(nullable = false)
    private Boolean keepDice2 = false;

    @Column(nullable = false)
    private Boolean keepDice3 = false;

    @Column(nullable = false)
    private Boolean keepDice4 = false;

    @Column(nullable = false)
    private Boolean keepDice5 = false;

    @Enumerated(EnumType.STRING)
    @Column
    private YahtzeeCategory selectedCategory; // Categoria scelta per il punteggio

    @Column(nullable = false)
    private Integer score = 0; // Punteggio ottenuto in questo round

    @Column(nullable = false)
    private Boolean isCompleted = false; // Se il round è completato

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Metodi di utilità
    public int[] getDiceValues() {
        return new int[]{dice1, dice2, dice3, dice4, dice5};
    }

    public void setDiceValues(int[] diceValues) {
        if (diceValues.length != 5) {
            throw new IllegalArgumentException("Must provide exactly 5 dice values");
        }
        this.dice1 = diceValues[0];
        this.dice2 = diceValues[1];
        this.dice3 = diceValues[2];
        this.dice4 = diceValues[3];
        this.dice5 = diceValues[4];
    }

    public boolean[] getKeptDice() {
        return new boolean[]{keepDice1, keepDice2, keepDice3, keepDice4, keepDice5};
    }

    public void setKeptDice(boolean[] keptDice) {
        if (keptDice.length != 5) {
            throw new IllegalArgumentException("Must provide exactly 5 boolean values");
        }
        this.keepDice1 = keptDice[0];
        this.keepDice2 = keptDice[1];
        this.keepDice3 = keptDice[2];
        this.keepDice4 = keptDice[3];
        this.keepDice5 = keptDice[4];
    }

    public void completeRound(YahtzeeCategory category, int finalScore) {
        this.selectedCategory = category;
        this.score = finalScore;
        this.isCompleted = true;
    }

    // Enum per le categorie del punteggio Yahtzee
    @Getter
    @AllArgsConstructor
    public enum YahtzeeCategory {
        // Sezione superiore
        ONES("Uni", 1),
        TWOS("Due", 2),
        THREES("Tre", 3),
        FOURS("Quattro", 4),
        FIVES("Cinque", 5),
        SIXES("Sei", 6),

        // Sezione inferiore
        THREE_OF_A_KIND("Tris", 0),
        FOUR_OF_A_KIND("Poker", 0),
        FULL_HOUSE("Full House", 25),
        SMALL_STRAIGHT("Scala Minore", 30),
        LARGE_STRAIGHT("Scala Maggiore", 40),
        YAHTZEE("Yahtzee", 50),
        CHANCE("Chance", 0);

        private final String displayName;
        private final int fixedScore; // 0 = punteggio variabile

        public boolean hasFixedScore() {
            return fixedScore > 0;
        }
    }
}