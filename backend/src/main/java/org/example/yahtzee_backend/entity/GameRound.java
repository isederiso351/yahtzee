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
import java.util.ArrayList;
import java.util.List;

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

    // Lista dei lanci - ogni stringa rappresenta 5 dadi es. "14635"
    @ElementCollection
    @CollectionTable(name = "dice_rolls", joinColumns = @JoinColumn(name = "game_round_id"))
    @OrderColumn(name = "roll_order")
    @Column(name = "roll_result", length = 5)
    private List<String> diceRolls = new ArrayList<>();

    // Lista dei dadi tenuti - ogni stringa è 5 bit es. "10010"
    @ElementCollection
    @CollectionTable(name = "kept_dice", joinColumns = @JoinColumn(name = "game_round_id"))
    @OrderColumn(name = "kept_order") // ← E QUESTO!
    @Column(name = "kept_pattern", length = 5)
    private List<String> keptDice = new ArrayList<>();

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

    public int getRollCount(){
        return this.diceRolls.size();
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