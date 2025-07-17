package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.domain.YahtzeeCategory;
import org.example.yahtzee_backend.entity.GameRound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class YahtzeeScoreService {

    private static final Logger logger = LoggerFactory.getLogger(YahtzeeScoreService.class);

    // === CALCOLO PUNTEGGI PER CATEGORIA ===

    public int calculateScore(int[] dice, YahtzeeCategory category) {
        if (dice.length != 5) {
            throw new IllegalArgumentException("Must provide exactly 5 dice");
        }

        // Valida che i dadi siano tra 1 e 6
        for (int die : dice) {
            if (die < 1 || die > 6) {
                throw new IllegalArgumentException("Dice values must be between 1 and 6");
            }
        }

        int score = calculateScoreInternal(dice, category);

        logger.debug("Calculated score for {}: {} -> {} points",
                category.getDisplayName(), Arrays.toString(dice), score);

        return score;
    }

    public int calculateScore(String diceString, YahtzeeCategory category) {
        if (diceString.length() != 5) {
            throw new IllegalArgumentException("Dice string must be exactly 5 characters");
        }

        int[] dice = new int[5];
        for (int i = 0; i < 5; i++) {
            dice[i] = Character.getNumericValue(diceString.charAt(i));
        }

        return calculateScore(dice, category);
    }

    private int calculateScoreInternal(int[] dice, YahtzeeCategory category) {
        switch (category) {
            // Sezione superiore (conteggio)
            case ONES:   return countDice(dice, 1);
            case TWOS:   return countDice(dice, 2);
            case THREES: return countDice(dice, 3);
            case FOURS:  return countDice(dice, 4);
            case FIVES:  return countDice(dice, 5);
            case SIXES:  return countDice(dice, 6);

            // Sezione inferiore (combinazioni)
            case THREE_OF_A_KIND: return calculateThreeOfAKind(dice);
            case FOUR_OF_A_KIND:  return calculateFourOfAKind(dice);
            case FULL_HOUSE:      return calculateFullHouse(dice);
            case SMALL_STRAIGHT:  return calculateSmallStraight(dice);
            case LARGE_STRAIGHT:  return calculateLargeStraight(dice);
            case YAHTZEE:         return calculateYahtzee(dice);
            case CHANCE:          return calculateChance(dice);

            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
    }

    // === SEZIONE SUPERIORE ===

    private int countDice(int[] dice, int value) {
        int count = 0;
        for (int die : dice) {
            if (die == value) {
                count++;
            }
        }
        return count * value;
    }

    // === SEZIONE INFERIORE ===

    private int calculateThreeOfAKind(int[] dice) {
        Map<Integer, Integer> counts = getDiceCounts(dice);

        for (int count : counts.values()) {
            if (count >= 3) {
                return sumAllDice(dice);
            }
        }

        return 0;
    }

    private int calculateFourOfAKind(int[] dice) {
        Map<Integer, Integer> counts = getDiceCounts(dice);

        for (int count : counts.values()) {
            if (count >= 4) {
                return sumAllDice(dice);
            }
        }

        return 0;
    }

    private int calculateFullHouse(int[] dice) {
        Map<Integer, Integer> counts = getDiceCounts(dice);

        boolean hasThree = false;
        boolean hasTwo = false;

        for (int count : counts.values()) {
            if (count == 3) {
                hasThree = true;
            } else if (count == 2) {
                hasTwo = true;
            }
        }

        return (hasThree && hasTwo) ? 25 : 0;
    }

    private int calculateSmallStraight(int[] dice) {
        Set<Integer> uniqueDice = new HashSet<>();
        for (int die : dice) {
            uniqueDice.add(die);
        }

        // Possibili small straight: 1-2-3-4, 2-3-4-5, 3-4-5-6
        int[][] smallStraights = {
                {1, 2, 3, 4},
                {2, 3, 4, 5},
                {3, 4, 5, 6}
        };

        for (int[] straight : smallStraights) {
            if (containsSequence(uniqueDice, straight)) {
                return 30;
            }
        }

        return 0;
    }

    private int calculateLargeStraight(int[] dice) {
        Set<Integer> uniqueDice = new HashSet<>();
        for (int die : dice) {
            uniqueDice.add(die);
        }
        if(uniqueDice.size() ==5 && (!uniqueDice.contains(1)||!uniqueDice.contains(6)))
            return 40;
        return 0;
    }

    private int calculateYahtzee(int[] dice) {
        int firstDie = dice[0];
        for (int die : dice) {
            if (die != firstDie) {
                return 0;
            }
        }
        return 50;
    }

    private int calculateChance(int[] dice) {
        return sumAllDice(dice);
    }



    // === METODI DI VALIDAZIONE ===

    public boolean isValidForCategory(int[] dice, YahtzeeCategory category) {
        try {
            int score = calculateScore(dice, category);
            return score > 0 || category == YahtzeeCategory.CHANCE;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidForCategory(String diceString, YahtzeeCategory category) {
        try {
            int score = calculateScore(diceString, category);
            return score > 0 || category == YahtzeeCategory.CHANCE;
        } catch (Exception e) {
            return false;
        }
    }

    // === ANALISI DADI ===

    public boolean isYahtzee(int[] dice) {
        return calculateYahtzee(dice) > 0;
    }

    public boolean isYahtzee(String diceString) {
        return calculateScore(diceString, YahtzeeCategory.YAHTZEE) > 0;
    }

    public boolean isFullHouse(int[] dice) {
        return calculateFullHouse(dice) > 0;
    }

    public boolean isSmallStraight(int[] dice) {
        return calculateSmallStraight(dice) > 0;
    }

    public boolean isLargeStraight(int[] dice) {
        return calculateLargeStraight(dice) > 0;
    }

    public boolean hasThreeOfAKind(int[] dice) {
        return calculateThreeOfAKind(dice) > 0;
    }

    public boolean hasFourOfAKind(int[] dice) {
        return calculateFourOfAKind(dice) > 0;
    }

    // === SUGGERIMENTI E ANALISI ===

    public List<YahtzeeCategory> getSuggestedCategories(int[] dice) {
        List<YahtzeeCategory> suggestions = new ArrayList<>();

        // Controlla tutte le categorie e ordina per punteggio
        for (YahtzeeCategory category : YahtzeeCategory.values()) {
            int score = calculateScore(dice, category);
            if (score > 0) {
                suggestions.add(category);
            }
        }

        // Ordina per punteggio decrescente
        suggestions.sort((a, b) -> Integer.compare(
                calculateScore(dice, b),
                calculateScore(dice, a)
        ));

        logger.debug("Suggested categories for {}: {}",
                Arrays.toString(dice),
                suggestions.stream().map(YahtzeeCategory::getDisplayName).collect(Collectors.toList()));

        return suggestions;
    }

    public Map<YahtzeeCategory, Integer> getAllPossibleScores(int[] dice) {
        Map<YahtzeeCategory, Integer> scores = new HashMap<>();

        for (YahtzeeCategory category : YahtzeeCategory.values()) {
            scores.put(category, calculateScore(dice, category));
        }

        return scores;
    }

    public YahtzeeCategory getBestCategory(int[] dice) {
        return getSuggestedCategories(dice).stream()
                .findFirst()
                .orElse(YahtzeeCategory.CHANCE);
    }

    // === STATISTICHE E PROBABILITÀ ===


    // === UTILITY PRIVATE ===

    private Map<Integer, Integer> getDiceCounts(int[] dice) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int die : dice) {
            counts.put(die, counts.getOrDefault(die, 0) + 1);
        }
        return counts;
    }

    private int sumAllDice(int[] dice) {
        int sum = 0;
        for (int die : dice) {
            sum += die;
        }
        return sum;
    }

    private boolean containsSequence(Set<Integer> dice, int[] sequence) {
        for (int value : sequence) {
            if (!dice.contains(value)) {
                return false;
            }
        }
        return true;
    }

    // === METODI PER DEBUGGING ===

    public String analyzeDice(int[] dice) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Dice: ").append(Arrays.toString(dice)).append("\n");

        Map<Integer, Integer> counts = getDiceCounts(dice);
        analysis.append("Counts: ").append(counts).append("\n");

        analysis.append("Combinations found:\n");
        if (isYahtzee(dice)) analysis.append("- Yahtzee!\n");
        if (isFullHouse(dice)) analysis.append("- Full House\n");
        if (isLargeStraight(dice)) analysis.append("- Large Straight\n");
        if (isSmallStraight(dice)) analysis.append("- Small Straight\n");
        if (hasFourOfAKind(dice)) analysis.append("- Four of a Kind\n");
        if (hasThreeOfAKind(dice)) analysis.append("- Three of a Kind\n");

        return analysis.toString();
    }

    public void logDiceAnalysis(int[] dice) {
        logger.info("Dice analysis:\n{}", analyzeDice(dice));
    }
}