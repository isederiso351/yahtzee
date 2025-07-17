package org.example.yahtzee_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Enum per le categorie del punteggio Yahtzee
@Getter
@AllArgsConstructor
public enum YahtzeeCategory {
    // Sezione superiore
    ONES("Uni", 1), TWOS("Due", 2), THREES("Tre", 3), FOURS("Quattro", 4), FIVES("Cinque", 5), SIXES("Sei", 6),

    // Sezione inferiore
    THREE_OF_A_KIND("Tris", 0), FOUR_OF_A_KIND("Poker", 0), FULL_HOUSE("Full House", 25), SMALL_STRAIGHT("Scala Minore",
            30), LARGE_STRAIGHT("Scala Maggiore", 40), YAHTZEE("Yahtzee", 50), CHANCE("Chance", 0);

    private final String displayName;
    private final int fixedScore; // 0 = punteggio variabile

    public boolean hasFixedScore() {
        return fixedScore > 0;
    }
}
    