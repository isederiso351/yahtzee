package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.domain.YahtzeeCategory;
import org.example.yahtzee_backend.entity.*;
import org.example.yahtzee_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameRoundService {

    private static final Logger logger = LoggerFactory.getLogger(GameRoundService.class);

    @Autowired
    private GameRoundRepository gameRoundRepository;

    @Autowired
    private YahtzeeScoreService yahtzeeScoreService;

    @Autowired
    private GamePlayerService gamePlayerService;

    @Autowired
    private PlayerService playerService;

    private final Random random = new Random();

    // === BUSINESS LOGIC METHODS (spostati dall'entity) ===

    public String createDiceString(int[] dice) {
        if (dice.length != 5) {
            throw new IllegalArgumentException("Must provide exactly 5 dice values");
        }
        StringBuilder sb = new StringBuilder();
        for (int die : dice) {
            if (die < 1 || die > 6) {
                throw new IllegalArgumentException("Dice values must be between 1 and 6");
            }
            sb.append(die);
        }
        return sb.toString();
    }

    public String createKeptString(boolean[] kept) {
        if (kept.length != 5) {
            throw new IllegalArgumentException("Must provide exactly 5 boolean values");
        }
        StringBuilder sb = new StringBuilder();
        for (boolean k : kept) {
            sb.append(k ? "1" : "0");
        }
        return sb.toString();
    }

    public void addRollToRound(GameRound gameRound, String diceResult) {
        if (diceResult.length() != 5) {
            throw new IllegalArgumentException("Dice result must be exactly 5 characters");
        }
        if (gameRound.getDiceRolls().size() >= 3) {
            throw new IllegalStateException("Cannot have more than 3 rolls per round");
        }

        gameRound.getDiceRolls().add(diceResult);
        logger.debug("Added roll {} to round {}", diceResult, gameRound.getId());
    }

    public void addKeptDiceToRound(GameRound gameRound, String keptPattern) {
        if (keptPattern.length() != 5) {
            throw new IllegalArgumentException("Kept pattern must be exactly 5 characters");
        }
        if (gameRound.getKeptDice().size() >= 2) {
            throw new IllegalStateException("Cannot have more than 2 kept patterns per round");
        }

        gameRound.getKeptDice().add(keptPattern);
        logger.debug("Added kept pattern {} to round {}", keptPattern, gameRound.getId());
    }

    public boolean canPlayerRollAgain(GameRound gameRound) {
        return gameRound.getDiceRolls().size() < 3 && !gameRound.getIsCompleted();
    }

    public void completeRoundWithCategory(GameRound gameRound, YahtzeeCategory category, int score) {
        gameRound.setSelectedCategory(category);
        gameRound.setScore(score);
        gameRound.setIsCompleted(true);

        logger.debug("Completed round {} with category {} and score {}",
                gameRound.getId(), category.getDisplayName(), score);
    }

    // === GESTIONE TURNI ===

    public GameRound startNewRound(Game game, Player player, Integer roundNumber) {
        // Validazioni
        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        if (!gamePlayerService.isPlayerActiveInGame(game, player)) {
            throw new IllegalArgumentException("Player is not active in this game");
        }

        // Verifica che il giocatore non abbia già un round attivo
        Optional<GameRound> existingRound = gameRoundRepository.findCurrentRoundForPlayer(game, player);
        if (existingRound.isPresent()) {
            logger.warn("Player {} already has an active round in game {}",
                    player.getUsername(), game.getGameCode());
            throw new IllegalStateException("Player already has an active round");
        }

        // Crea nuovo round
        GameRound gameRound = new GameRound();
        gameRound.setGame(game);
        gameRound.setPlayer(player);
        gameRound.setRoundNumber(roundNumber);
        gameRound.setIsCompleted(false);

        GameRound savedRound = gameRoundRepository.save(gameRound);

        // Registra attività del giocatore
        playerService.recordPlayerActivity(player);

        logger.info("Started new round {} for player {} in game {}",
                roundNumber, player.getUsername(), game.getGameCode());

        return savedRound;
    }

    public GameRound rollDice(GameRound gameRound) {
        return rollDice(gameRound, null);
    }

    public GameRound rollDice(GameRound gameRound, boolean[] keepDice) {
        // Validazioni
        if (gameRound.getIsCompleted()) {
            throw new IllegalStateException("Round is already completed");
        }

        if (!canPlayerRollAgain(gameRound)) {
            throw new IllegalStateException("Maximum rolls reached for this round");
        }

        // Determina quali dadi lanciare
        int[] newDice = generateDiceRoll(gameRound, keepDice);

        // Aggiungi il lancio al round usando business logic
        String diceString = createDiceString(newDice);
        addRollToRound(gameRound, diceString);

        // Se non è il primo lancio, aggiungi il pattern kept del lancio precedente
        if (keepDice != null && gameRound.getDiceRolls().size() > 1) {
            String keptString = createKeptString(keepDice);
            addKeptDiceToRound(gameRound, keptString);
        }

        GameRound savedRound = gameRoundRepository.save(gameRound);

        logger.info("Player {} rolled dice in game {} round {}: {} (roll {}/3)",
                gameRound.getPlayer().getUsername(),
                gameRound.getGame().getGameCode(),
                gameRound.getRoundNumber(),
                diceString,
                gameRound.getDiceRolls().size());

        return savedRound;
    }

    public GameRound completeRound(GameRound gameRound, YahtzeeCategory category) {
        // Validazioni
        if (gameRound.getIsCompleted()) {
            throw new IllegalStateException("Round is already completed");
        }

        if (gameRound.getDiceRolls().isEmpty()) {
            throw new IllegalStateException("No dice rolls in this round");
        }

        // Calcola il punteggio
        int[] finalDice = getFinalDiceValues(gameRound);
        int score = yahtzeeScoreService.calculateScore(finalDice, category);

        // Completa il round usando business logic
        completeRoundWithCategory(gameRound, category, score);
        GameRound savedRound = gameRoundRepository.save(gameRound);

        // Aggiorna il punteggio del giocatore nella partita
        gamePlayerService.addScoreToPlayer(
                gameRound.getGame(),
                gameRound.getPlayer(),
                score
        );

        // Registra attività del giocatore
        playerService.recordPlayerActivity(gameRound.getPlayer());

        logger.info("Completed round {} for player {} in game {}: {} -> {} points",
                gameRound.getRoundNumber(),
                gameRound.getPlayer().getUsername(),
                gameRound.getGame().getGameCode(),
                category.getDisplayName(),
                score);

        return savedRound;
    }

    // === PARSING E UTILITY ===

    public int[] getFinalDiceValues(GameRound gameRound) {
        if (gameRound.getDiceRolls().isEmpty()) {
            throw new IllegalStateException("No dice rolls recorded");
        }
        String lastRoll = gameRound.getDiceRolls().get(gameRound.getDiceRolls().size() - 1);
        return parseDiceString(lastRoll);
    }

    public int[] getDiceValues(GameRound gameRound, int rollNumber) {
        if (rollNumber < 1 || rollNumber > gameRound.getDiceRolls().size()) {
            throw new IllegalArgumentException("Invalid roll number: " + rollNumber);
        }
        String rollResult = gameRound.getDiceRolls().get(rollNumber - 1);
        return parseDiceString(rollResult);
    }

    public boolean[] getKeptDicePattern(GameRound gameRound, int rollNumber) {
        if (rollNumber < 1 || rollNumber > gameRound.getKeptDice().size()) {
            throw new IllegalArgumentException("Invalid roll number for kept dice: " + rollNumber);
        }
        String keptPattern = gameRound.getKeptDice().get(rollNumber - 1);
        return parseKeptString(keptPattern);
    }

    private int[] parseDiceString(String diceString) {
        int[] dice = new int[5];
        for (int i = 0; i < 5; i++) {
            dice[i] = Character.getNumericValue(diceString.charAt(i));
        }
        return dice;
    }

    private boolean[] parseKeptString(String keptString) {
        boolean[] kept = new boolean[5];
        for (int i = 0; i < 5; i++) {
            kept[i] = keptString.charAt(i) == '1';
        }
        return kept;
    }

    // === GENERAZIONE DADI ===

    private int[] generateDiceRoll(GameRound gameRound, boolean[] keepDice) {
        int[] newDice = new int[5];

        if (gameRound.getDiceRolls().isEmpty() || keepDice == null) {
            // Primo lancio - genera tutti i dadi
            for (int i = 0; i < 5; i++) {
                newDice[i] = random.nextInt(6) + 1;
            }
        } else {
            // Lancio successivo - mantieni dadi specificati, rigenera gli altri
            int[] previousDice = getFinalDiceValues(gameRound);

            for (int i = 0; i < 5; i++) {
                if (keepDice[i]) {
                    newDice[i] = previousDice[i]; // Mantieni
                } else {
                    newDice[i] = random.nextInt(6) + 1; // Rigenera
                }
            }
        }

        return newDice;
    }

    public int[] rollDiceOnly() {
        int[] dice = new int[5];
        for (int i = 0; i < 5; i++) {
            dice[i] = random.nextInt(6) + 1;
        }
        return dice;
    }

    // === QUERY E RICERCHE ===

    public Optional<GameRound> getCurrentRoundForPlayer(Game game, Player player) {
        return gameRoundRepository.findCurrentRoundForPlayer(game, player);
    }

    public List<GameRound> getPlayerRoundsInGame(Game game, Player player) {
        return gameRoundRepository.findByGameAndPlayerOrderByRoundNumberAsc(game, player);
    }

    public List<GameRound> getCompletedRoundsInGame(Game game) {
        return gameRoundRepository.findByGameAndIsCompletedTrueOrderByRoundNumberAsc(game);
    }

    public Optional<GameRound> getSpecificRound(Game game, Player player, Integer roundNumber) {
        return gameRoundRepository.findByGameAndPlayerAndRoundNumber(game, player, roundNumber);
    }

    // === VALIDAZIONI ===

    public boolean canPlayerRoll(Game game, Player player) {
        Optional<GameRound> currentRound = getCurrentRoundForPlayer(game, player);
        return currentRound.map(this::canPlayerRollAgain).orElse(true);
    }

    public boolean hasPlayerCompletedRound(Game game, Player player, Integer roundNumber) {
        return gameRoundRepository.hasPlayerCompletedRound(game, player, roundNumber);
    }

    public boolean isRoundComplete(GameRound gameRound) {
        return gameRound.getIsCompleted();
    }

    public boolean hasAllPlayersCompletedRound(Game game, Integer roundNumber) {
        List<GamePlayer> activePlayers = gamePlayerService.getActiveGamePlayersInOrder(game);

        for (GamePlayer gamePlayer : activePlayers) {
            if (!hasPlayerCompletedRound(game, gamePlayer.getPlayer(), roundNumber)) {
                return false;
            }
        }

        return true;
    }

    // === ANALISI E SUGGERIMENTI ===


    public List<YahtzeeCategory> getSuggestedCategories(GameRound gameRound) {
        if (gameRound.getDiceRolls().isEmpty()) {
            return new ArrayList<>();
        }

        int[] finalDice = getFinalDiceValues(gameRound);
        return yahtzeeScoreService.getSuggestedCategories(finalDice);
    }

    public YahtzeeCategory getBestCategory(GameRound gameRound) {
        if (gameRound.getDiceRolls().isEmpty()) {
            return YahtzeeCategory.CHANCE;
        }

        int[] finalDice = getFinalDiceValues(gameRound);
        return yahtzeeScoreService.getBestCategory(finalDice);
    }

    // === STATISTICHE ===

    public List<GameRound> findYahtzees(Player player) {
        List<GameRound> completedRounds = gameRoundRepository.findByPlayerAndIsCompletedTrueOrderByCreatedAtDesc(player);

        return completedRounds.stream()
                .filter(round -> {
                    try {
                        int[] dice = getFinalDiceValues(round);
                        return yahtzeeScoreService.isYahtzee(dice);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<GameRound> findAllYahtzees() {
        List<GameRound> completedRounds = gameRoundRepository.findByIsCompletedTrueOrderByCreatedAtDesc();

        return completedRounds.stream()
                .filter(round -> {
                    try {
                        int[] dice = getFinalDiceValues(round);
                        return yahtzeeScoreService.isYahtzee(dice);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public Long getCompletedRoundsCount(Game game, Player player) {
        return gameRoundRepository.countCompletedRounds(game, player);
    }

    // === GESTIONE ERRORI E RECOVERY ===

    public void cancelRound(GameRound gameRound, String reason) {
        if (gameRound.getIsCompleted()) {
            throw new IllegalStateException("Cannot cancel completed round");
        }

        gameRoundRepository.delete(gameRound);

        logger.warn("Cancelled round {} for player {} in game {}: {}",
                gameRound.getRoundNumber(),
                gameRound.getPlayer().getUsername(),
                gameRound.getGame().getGameCode(),
                reason);
    }

    public GameRound resetRound(GameRound gameRound) {
        if (gameRound.getIsCompleted()) {
            throw new IllegalStateException("Cannot reset completed round");
        }

        // Cancella il round esistente e creane uno nuovo
        Game game = gameRound.getGame();
        Player player = gameRound.getPlayer();
        Integer roundNumber = gameRound.getRoundNumber();

        gameRoundRepository.delete(gameRound);

        logger.info("Reset round {} for player {} in game {}",
                roundNumber, player.getUsername(), game.getGameCode());

        return startNewRound(game, player, roundNumber);
    }

    // === UTILITY ===

    public String getDiceString(GameRound gameRound) {
        if (gameRound.getDiceRolls().isEmpty()) {
            return null;
        }
        return gameRound.getDiceRolls().get(gameRound.getDiceRolls().size() - 1);
    }

    public int[] getDiceArray(GameRound gameRound) {
        try {
            return getFinalDiceValues(gameRound);
        } catch (Exception e) {
            logger.error("Error getting dice array for round {}: {}", gameRound.getId(), e.getMessage());
            return new int[0];
        }
    }

    public void logRoundSummary(GameRound gameRound) {
        if (gameRound.getIsCompleted()) {
            logger.info("Round summary - Player: {}, Game: {}, Round: {}, Category: {}, Score: {}, Rolls: {}",
                    gameRound.getPlayer().getUsername(),
                    gameRound.getGame().getGameCode(),
                    gameRound.getRoundNumber(),
                    gameRound.getSelectedCategory() != null ? gameRound.getSelectedCategory().getDisplayName() : "None",
                    gameRound.getScore(),
                    gameRound.getDiceRolls().size());
        }
    }
}