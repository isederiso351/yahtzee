package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.GamePlayer;
import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.repository.GamePlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GamePlayerService {

    private static final Logger logger = LoggerFactory.getLogger(GamePlayerService.class);

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerService playerService;

    // === GESTIONE PARTECIPAZIONE ===

    public GamePlayer addPlayerToGame(Game game, Player player) {
        // Validazioni
        if (gamePlayerRepository.existsByGameAndPlayer(game, player)) {
            logger.warn("Attempted to add player {} to game {} but already exists", player.getUsername(),
                    game.getGameCode());
            throw new IllegalStateException("Player is already in this game");
        }

        // Calcola automaticamente il prossimo joinOrder
        Integer maxOrder = gamePlayerRepository.getMaxJoinOrderInGame(game);
        int joinOrder = (maxOrder != null ? maxOrder : 0) + 1;

        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setGame(game);
        gamePlayer.setPlayer(player);
        gamePlayer.setJoinOrder(joinOrder);
        gamePlayer.setTotalScore(0);
        gamePlayer.setIsActive(true);

        GamePlayer savedGamePlayer = gamePlayerRepository.save(gamePlayer);

        // Registra attività del giocatore
        playerService.recordPlayerActivity(player);

        logger.info("Player {} added to game {} with join order {}", player.getUsername(), game.getGameCode(),
                joinOrder);

        return savedGamePlayer;
    }

    public void removePlayerFromGame(Game game, Player player, String reason) {
        Optional<GamePlayer> gamePlayerOpt = gamePlayerRepository.findByGameAndPlayer(game, player);

        if (gamePlayerOpt.isPresent()) {
            GamePlayer gamePlayer = gamePlayerOpt.get();
            gamePlayer.setIsActive(false);
            gamePlayerRepository.save(gamePlayer);

            logger.info("Player {} removed from game {}: {}", player.getUsername(), game.getGameCode(), reason);
        } else {
            logger.warn("Attempted to remove player {} from game {} but not found", player.getUsername(),
                    game.getGameCode());
            throw new IllegalArgumentException("Player is not in this game");
        }
    }

    public void reactivatePlayerInGame(Game game, Player player) {
        Optional<GamePlayer> gamePlayerOpt = gamePlayerRepository.findByGameAndPlayer(game, player);

        if (gamePlayerOpt.isPresent()) {
            GamePlayer gamePlayer = gamePlayerOpt.get();
            gamePlayer.setIsActive(true);
            gamePlayerRepository.save(gamePlayer);

            logger.info("Player {} reactivated in game {}", player.getUsername(), game.getGameCode());
        } else {
            throw new IllegalArgumentException("Player is not in this game");
        }
    }

    // === GESTIONE PUNTEGGI ===

    public void addScoreToPlayer(Game game, Player player, int score) {
        GamePlayer gamePlayer = getGamePlayer(game, player);

        int oldScore = gamePlayer.getTotalScore();
        gamePlayer.addScore(score);
        gamePlayerRepository.save(gamePlayer);

        logger.debug("Added {} points to player {} in game {} (total: {} -> {})", score, player.getUsername(),
                game.getGameCode(), oldScore, gamePlayer.getTotalScore());
    }

    public void setPlayerScore(Game game, Player player, int totalScore) {
        GamePlayer gamePlayer = getGamePlayer(game, player);

        int oldScore = gamePlayer.getTotalScore();
        gamePlayer.setTotalScore(totalScore);
        gamePlayerRepository.save(gamePlayer);

        logger.debug("Set total score for player {} in game {} ({} -> {})", player.getUsername(), game.getGameCode(),
                oldScore, totalScore);
    }

    public int getPlayerScore(Game game, Player player) {
        GamePlayer gamePlayer = getGamePlayer(game, player);
        return gamePlayer.getTotalScore();
    }

    // === GESTIONE TURNI E ORDINI ===

    public List<GamePlayer> getGamePlayersInOrder(Game game) {
        return gamePlayerRepository.findByGameOrderByJoinOrderAsc(game);
    }

    public List<GamePlayer> getActiveGamePlayersInOrder(Game game) {
        return gamePlayerRepository.findByGameAndIsActiveTrueOrderByJoinOrder(game);
    }

    public Optional<GamePlayer> getNextPlayerInOrder(Game game, Player currentPlayer) {
        List<GamePlayer> activePlayers = getActiveGamePlayersInOrder(game);

        if (activePlayers.isEmpty()) {
            return Optional.empty();
        }

        // Trova l'indice del giocatore corrente
        int currentIndex = -1;
        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getPlayer().getId().equals(currentPlayer.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            logger.warn("Current player {} not found in active players for game {}", currentPlayer.getUsername(),
                    game.getGameCode());
            return Optional.empty();
        }

        // Prossimo giocatore (con wrap-around)
        int nextIndex = (currentIndex + 1) % activePlayers.size();
        return Optional.of(activePlayers.get(nextIndex));
    }

    public Optional<GamePlayer> getFirstPlayerInOrder(Game game) {
        List<GamePlayer> activePlayers = getActiveGamePlayersInOrder(game);
        return activePlayers.isEmpty() ? Optional.empty() : Optional.of(activePlayers.getFirst());
    }

    public boolean isPlayerTurn(Game game, Player player) {
        return game.getCurrentPlayer() != null && game.getCurrentPlayer().getId().equals(player.getId());
    }

    // === STATISTICHE E CLASSIFICHE ===

    public List<GamePlayer> getPlayersByScore(Game game) {
        return gamePlayerRepository.findByGameOrderByTotalScoreDesc(game);
    }

    public Optional<GamePlayer> getWinningPlayer(Game game) {
        List<GamePlayer> playersByScore = getPlayersByScore(game);
        return playersByScore.isEmpty() ? Optional.empty() : Optional.of(playersByScore.get(0));
    }

    public Long getActivePlayersCount(Game game) {
        return gamePlayerRepository.countByGameAndIsActiveTrue(game);
    }

    public Long getTotalPlayersCount(Game game) {
        return gamePlayerRepository.countByGame(game);
    }

    // === VALIDAZIONI E VERIFICHE ===

    public boolean isPlayerInGame(Game game, Player player) {
        return gamePlayerRepository.existsByGameAndPlayer(game, player);
    }

    public boolean isPlayerActiveInGame(Game game, Player player) {
        Optional<GamePlayer> gamePlayerOpt = gamePlayerRepository.findByGameAndPlayer(game, player);
        return gamePlayerOpt.map(GamePlayer::getIsActive).orElse(false);
    }

    public boolean canPlayerJoinGame(Game game, Player player) {
        // Verifica che il giocatore non sia già nella partita
        if (isPlayerInGame(game, player)) {
            return false;
        }

        // Verifica che il gioco abbia spazio
        if (game.isFull()) {
            return false;
        }

        // Verifica che il gioco sia in attesa
        return game.getStatus() == Game.GameStatus.WAITING;
    }

    // === RICERCHE ===

    public GamePlayer getGamePlayer(Game game, Player player) {
        return gamePlayerRepository.findByGameAndPlayer(game, player).orElseThrow(() -> new IllegalArgumentException(
                String.format("Player %s is not in game %s", player.getUsername(), game.getGameCode())));
    }

    public Optional<GamePlayer> findGamePlayer(Game game, Player player) {
        return gamePlayerRepository.findByGameAndPlayer(game, player);
    }

    public List<GamePlayer> getPlayerGames(Player player) {
        return gamePlayerRepository.findByPlayerOrderByJoinedAtDesc(player);
    }

    public List<GamePlayer> getPlayerActiveGames(Player player) {
        return gamePlayerRepository.findActiveGamesByPlayer(player);
    }

    // === STATISTICHE GIOCATORE ===

    public Double getPlayerAverageScore(Player player) {
        return gamePlayerRepository.getAverageScoreByPlayer(player);
    }

    public Integer getPlayerHighestScore(Player player) {
        return gamePlayerRepository.getHighestScoreByPlayer(player);
    }

    public List<GamePlayer> getPlayerRecentGames(Player player, int limit) {
        List<GamePlayer> recentGames = gamePlayerRepository.findRecentGamesByPlayer(player);

        // Limita i risultati se necessario
        if (recentGames.size() > limit) {
            return recentGames.subList(0, limit);
        }

        return recentGames;
    }

    // === UTILITY E HELPER ===

    public String getPlayerRankingInGame(Game game, Player player) {
        List<GamePlayer> playersByScore = getPlayersByScore(game);

        for (int i = 0; i < playersByScore.size(); i++) {
            if (playersByScore.get(i).getPlayer().getId().equals(player.getId())) {
                return String.format("%d/%d", i + 1, playersByScore.size());
            }
        }

        return "N/A";
    }
}