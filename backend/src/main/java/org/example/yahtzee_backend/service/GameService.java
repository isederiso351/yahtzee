package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.entity.*;
import org.example.yahtzee_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private TransactionService transactionService;

    // === CREAZIONE E GESTIONE PARTITE ===

    public Game createGame(Player creator, BigDecimal betAmount, Integer maxPlayers) {
        // Validazioni
        if (betAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bet amount must be positive");
        }

        if (maxPlayers < 2 || maxPlayers > 6) {
            throw new IllegalArgumentException("Max players must be between 2 and 6");
        }

        if (!playerService.hasEnoughBalance(creator, betAmount)) {
            logger.warn("Player {} tried to create game with insufficient balance: {} required, {} available",
                    creator.getUsername(), betAmount, creator.getBalance());
            throw new IllegalArgumentException("Insufficient balance to create game");
        }

        // Verifica che il giocatore non sia già in una partita attiva
        if (isPlayerInActiveGame(creator)) {
            logger.warn("Player {} tried to create game while already in active game", creator.getUsername());
            throw new IllegalStateException("Player is already in an active game");
        }

        // Crea il gioco
        Game game = new Game();
        game.setGameCode(generateGameCode());
        game.setStatus(Game.GameStatus.WAITING);
        game.setBetAmount(betAmount);
        game.setMaxPlayers(maxPlayers);
        game.setTotalPrize(betAmount); // Inizialmente solo la scommessa del creator

        Game savedGame = gameRepository.save(game);

        // Aggiungi il creator come primo giocatore
        joinGame(savedGame, creator);

        logger.info("Game {} created by {} with bet {} and max {} players",
                savedGame.getGameCode(), creator.getUsername(), betAmount, maxPlayers);

        return savedGame;
    }

    public Game joinGame(Game game, Player player) {
        // Validazioni
        if (game.getStatus() != Game.GameStatus.WAITING) {
            logger.warn("Player {} tried to join game {} with status {}",
                    player.getUsername(), game.getGameCode(), game.getStatus());
            throw new IllegalStateException("Game is not accepting new players");
        }

        if (game.isFull()) {
            logger.warn("Player {} tried to join full game {}", player.getUsername(), game.getGameCode());
            throw new IllegalStateException("Game is full");
        }

        if (!playerService.hasEnoughBalance(player, game.getBetAmount())) {
            logger.warn("Player {} tried to join game {} with insufficient balance: {} required, {} available",
                    player.getUsername(), game.getGameCode(), game.getBetAmount(), player.getBalance());
            throw new IllegalArgumentException("Insufficient balance to join game");
        }

        if (isPlayerInActiveGame(player)) {
            logger.warn("Player {} tried to join game {} while already in active game",
                    player.getUsername(), game.getGameCode());
            throw new IllegalStateException("Player is already in an active game");
        }

        // Verifica che il giocatore non sia già in questa partita
        if (gamePlayerRepository.existsByGameAndPlayer(game, player)) {
            logger.warn("Player {} tried to join game {} but is already in it",
                    player.getUsername(), game.getGameCode());
            throw new IllegalStateException("Player is already in this game");
        }

        // Sottrai la scommessa dal saldo del giocatore
        transactionService.createBetTransaction(player, game, game.getBetAmount());

        // Aggiungi il giocatore alla partita
        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setGame(game);
        gamePlayer.setPlayer(player);
        gamePlayer.setJoinOrder(game.getPlayersCount() + 1);
        gamePlayer.setIsActive(true);

        gamePlayerRepository.save(gamePlayer);

        // Aggiorna il prize pool
        game.setTotalPrize(game.getTotalPrize().add(game.getBetAmount()));
        gameRepository.save(game);

        // Registra attività del giocatore
        playerService.recordPlayerActivity(player);

        // Se il gioco è pieno, può essere avviato
        if (game.isFull()) {
            logger.info("Game {} is now full ({} players)", game.getGameCode(), game.getMaxPlayers());
            // Auto-start se configurato, altrimenti aspetta comando manuale
            // startGame(game);
        }

        logger.info("Player {} joined game {} ({}/{} players)",
                player.getUsername(), game.getGameCode(), game.getPlayersCount(), game.getMaxPlayers());

        return game;
    }

    public boolean isPlayerCreator(Game game, Player player) {
        return getCreator(game).getId().equals(player.getId());
    }

    public Game startGame(Game game) {
        if (!game.canStart()) {
            logger.warn("Attempted to start game {} that cannot be started (status: {}, players: {})",
                    game.getGameCode(), game.getStatus(), game.getPlayersCount());
            throw new IllegalStateException("Game cannot be started");
        }

        game.setStatus(Game.GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());

        // Imposta il primo giocatore come giocatore corrente
        List<GamePlayer> players = gamePlayerRepository.findByGameOrderByJoinOrderAsc(game);
        if (!players.isEmpty()) {
            game.setCurrentPlayer(players.get(0).getPlayer());
        }

        logger.info("Game {} started with {} players. First player: {}",
                game.getGameCode(), players.size(),
                game.getCurrentPlayer() != null ? game.getCurrentPlayer().getUsername() : "None");

        return gameRepository.save(game);
    }

    public Game finishGame(Game game, Player winner) {
        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            logger.warn("Attempted to finish game {} with status {}", game.getGameCode(), game.getStatus());
            throw new IllegalStateException("Game is not in progress");
        }

        game.setStatus(Game.GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishedAt(LocalDateTime.now());

        // Distribuisci il premio al vincitore
        transactionService.createWinTransaction(winner, game, game.getTotalPrize());

        logger.info("Game {} finished. Winner: {} - Prize: {}",
                game.getGameCode(), winner.getUsername(), game.getTotalPrize());

        // Aggiorna statistiche di tutti i giocatori
        List<GamePlayer> gamePlayers = gamePlayerRepository.findByGameOrderByJoinOrder(game);
        for (GamePlayer gamePlayer : gamePlayers) {
            Player player = gamePlayer.getPlayer();
            boolean won = player.getId().equals(winner.getId());

            playerService.updatePlayerStats(
                    player,
                    won,
                    won ? game.getTotalPrize() : null,
                    won ? null : game.getBetAmount(),
                    gamePlayer.getTotalScore()
            );

            logger.debug("Updated stats for player {} in game {}: won={}, score={}",
                    player.getUsername(), game.getGameCode(), won, gamePlayer.getTotalScore());
        }

        return gameRepository.save(game);
    }

    public void cancelGame(Game game, String reason) {
        if (game.getStatus() == Game.GameStatus.FINISHED) {
            logger.warn("Attempted to cancel finished game {}", game.getGameCode());
            throw new IllegalStateException("Cannot cancel a finished game");
        }

        game.setStatus(Game.GameStatus.CANCELLED);
        gameRepository.save(game);

        logger.warn("Game {} cancelled: {}", game.getGameCode(), reason);

        // Rimborsa tutti i giocatori
        List<GamePlayer> gamePlayers = gamePlayerRepository.findByGameOrderByJoinOrder(game);
        for (GamePlayer gamePlayer : gamePlayers) {
            transactionService.createRefundTransaction(
                    gamePlayer.getPlayer(),
                    game,
                    game.getBetAmount(),
                    "Game cancelled: " + reason
            );
        }

        logger.info("Refunded {} players for cancelled game {}", gamePlayers.size(), game.getGameCode());
    }

    // === GESTIONE TURNI ===

    public Player getNextPlayer(Game game) {
        if (game.getCurrentPlayer() == null) {
            return null;
        }

        List<GamePlayer> players = gamePlayerRepository.findByGameAndIsActiveTrueOrderByJoinOrder(game);

        // Trova il giocatore corrente
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getId().equals(game.getCurrentPlayer().getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return null; // Giocatore corrente non trovato
        }

        // Prossimo giocatore (con wrap-around)
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex).getPlayer();
    }

    public Player getCreator(Game game){
        return game.getPlayers().getFirst().getPlayer();
    }

    public void advanceToNextPlayer(Game game) {
        Player nextPlayer = getNextPlayer(game);
        if (nextPlayer != null) {
            game.setCurrentPlayer(nextPlayer);
            gameRepository.save(game);

            logger.debug("Advanced to next player in game {}: {}",
                    game.getGameCode(), nextPlayer.getUsername());
        }
    }

    public void advanceToNextRound(Game game) {
        if (game.getCurrentRound() < game.getMaxRounds()) {
            game.setCurrentRound(game.getCurrentRound() + 1);

            // Reset al primo giocatore per il nuovo round
            List<GamePlayer> players = gamePlayerRepository.findByGameOrderByJoinOrderAsc(game);
            if (!players.isEmpty()) {
                game.setCurrentPlayer(players.get(0).getPlayer());
            }

            gameRepository.save(game);

            logger.info("Game {} advanced to round {}/{}",
                    game.getGameCode(), game.getCurrentRound(), game.getMaxRounds());
        } else {
            // Gioco finito, determina il vincitore
            logger.info("Game {} completed all rounds, determining winner", game.getGameCode());
            determineWinner(game);
        }
    }

    private void determineWinner(Game game) {
        List<GamePlayer> players = gamePlayerRepository.findByGameOrderByTotalScoreDesc(game);

        if (!players.isEmpty()) {
            Player winner = players.get(0).getPlayer();

            logger.info("Winner determined for game {}: {} with score {}",
                    game.getGameCode(), winner.getUsername(), players.get(0).getTotalScore());

            finishGame(game, winner);
        } else {
            logger.error("No players found for game {} when determining winner", game.getGameCode());
        }
    }

    // === RICERCHE E VALIDAZIONI ===

    public Optional<Game> findById(Long id) {
        return gameRepository.findById(id);
    }

    public Optional<Game> findByGameCode(String gameCode) {
        return gameRepository.findByGameCode(gameCode);
    }

    public List<Game> findAvailableGames() {
        return gameRepository.findAvailableGames();
    }
    public List<Game> findAvailableGames(Pageable pageable) {
        return gameRepository.findAvailableGames();
    }

    public List<Game> findAvailableGamesByBetAmount(BigDecimal betAmount) {
        return gameRepository.findAvailableGamesByBetAmount(betAmount);
    }

    public List<Game> findAvailableGamesByBetAmount(BigDecimal betAmount, Pageable pageable) {
        return gameRepository.findAvailableGamesByBetAmount(betAmount);
    }

    public List<Game> findPlayerGames(Player player) {
        return gameRepository.findByPlayer(player);
    }

    public List<Game> findActiveGamesByPlayer(Player player) {
        return gameRepository.findActiveGamesByPlayer(player);
    }

    public boolean isPlayerInActiveGame(Player player) {
        return gameRepository.isPlayerInActiveGame(player);
    }

    public List<Game> findJoinableGamesForPlayer(Player player) {
        return gameRepository.findJoinableGamesForPlayer(player, player.getBalance());
    }

    // === STATISTICHE ===

    public List<Game> getRecentlyFinishedGames(int limit) {
        return gameRepository.findRecentlyFinishedGames(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    public Long getGamesCountByStatus(Game.GameStatus status) {
        return gameRepository.countByStatus(status);
    }

    public BigDecimal getAveragePrizePool() {
        return gameRepository.getAveragePrizePool();
    }

    public BigDecimal getTotalPrizesDistributedSince(LocalDateTime since) {
        return gameRepository.getTotalPrizesDistributedSince(since);
    }

    // === UTILITY ===

    private String generateGameCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (gameRepository.findByGameCode(code).isPresent());

        return code;
    }

    public boolean canPlayerJoinGame(Game game, Player player) {
        try {
            // Verifica tutte le condizioni senza modificare dati
            if (game.getStatus() != Game.GameStatus.WAITING) return false;
            if (game.isFull()) return false;
            if (!playerService.hasEnoughBalance(player, game.getBetAmount())) return false;
            if (isPlayerInActiveGame(player)) return false;
            if (gamePlayerRepository.existsByGameAndPlayer(game, player)) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<GamePlayer> getGamePlayers(Game game) {
        return gamePlayerRepository.findByGameOrderByJoinOrder(game);
    }

    public Optional<GamePlayer> getGamePlayer(Game game, Player player) {
        return gamePlayerRepository.findByGameAndPlayer(game, player);
    }

    public boolean isPlayerTurn(Game game, Player player) {
        return game.getCurrentPlayer() != null &&
                game.getCurrentPlayer().getId().equals(player.getId());
    }

    public Game refreshGame(Game game) {
        return gameRepository.findById(game.getId())
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
    }


}