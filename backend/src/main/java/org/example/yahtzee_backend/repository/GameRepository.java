package org.example.yahtzee_backend.repository;

import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    // Trova per codice gioco
    Optional<Game> findByGameCode(String gameCode);

    // Giochi per stato
    List<Game> findByStatusOrderByCreatedAtDesc(Game.GameStatus status);

    // Giochi in attesa (dove possono entrare altri giocatori)
    @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' AND SIZE(g.players) < g.maxPlayers ORDER BY g.createdAt ASC")
    List<Game> findAvailableGames();

    // Giochi in attesa per una specifica scommessa
    @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' AND g.betAmount = :betAmount AND SIZE(g.players) < g.maxPlayers ORDER BY g.createdAt ASC")
    List<Game> findAvailableGamesByBetAmount(@Param("betAmount") BigDecimal betAmount);

    // Giochi con paginazione
    Page<Game> findByStatusOrderByCreatedAtDesc(Game.GameStatus status, Pageable pageable);

    // Giochi attivi
    List<Game> findByStatusInOrderByCreatedAtDesc(List<Game.GameStatus> statuses);

    // Giochi di un giocatore specifico
    @Query("SELECT DISTINCT g FROM Game g JOIN g.players gp WHERE gp.player = :player ORDER BY g.createdAt DESC")
    List<Game> findByPlayer(@Param("player") Player player);

    // Giochi vinti da un giocatore
    List<Game> findByWinnerOrderByFinishedAtDesc(Player winner);

    // Giochi in corso di un giocatore
    @Query("SELECT DISTINCT g FROM Game g JOIN g.players gp WHERE gp.player = :player AND g.status = 'IN_PROGRESS'")
    List<Game> findActiveGamesByPlayer(@Param("player") Player player);

    // Giochi finiti di recente
    @Query("SELECT g FROM Game g WHERE g.status = 'FINISHED' ORDER BY g.finishedAt DESC")
    List<Game> findRecentlyFinishedGames(Pageable pageable);

    // Giochi per range di scommesse
    List<Game> findByBetAmountBetweenOrderByCreatedAtDesc(BigDecimal minBet, BigDecimal maxBet);

    // Statistiche giochi per giocatore
    @Query("SELECT COUNT(DISTINCT g) FROM Game g JOIN g.players gp WHERE gp.player = :player")
    Long countGamesByPlayer(@Param("player") Player player);

    @Query("SELECT COUNT(g) FROM Game g WHERE g.winner = :player")
    Long countWinsByPlayer(@Param("player") Player player);

    // Giochi creati in un periodo
    List<Game> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Statistiche generali
    @Query("SELECT COUNT(g) FROM Game g WHERE g.status = :status")
    Long countByStatus(@Param("status") Game.GameStatus status);

    @Query("SELECT AVG(g.totalPrize) FROM Game g WHERE g.status = 'FINISHED'")
    BigDecimal getAveragePrizePool();

    @Query("SELECT SUM(g.totalPrize) FROM Game g WHERE g.status = 'FINISHED' AND g.finishedAt >= :startDate")
    BigDecimal getTotalPrizesDistributedSince(@Param("startDate") LocalDateTime startDate);

    // Giochi più redditizi
    @Query("SELECT g FROM Game g WHERE g.status = 'FINISHED' ORDER BY g.totalPrize DESC")
    List<Game> findHighestPrizeGames(Pageable pageable);

    // Giochi aperti da più tempo
    @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' ORDER BY g.createdAt ASC")
    List<Game> findOldestWaitingGames();

    // Verifica se un giocatore è in una partita attiva
    @Query("SELECT COUNT(g) > 0 FROM Game g JOIN g.players gp WHERE gp.player = :player AND g.status IN ('WAITING', 'IN_PROGRESS')")
    boolean isPlayerInActiveGame(@Param("player") Player player);

    // Trova giochi dove un giocatore può entrare
    @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' AND g.betAmount <= :playerBalance AND SIZE(g.players) < g.maxPlayers " +
            "AND g.id NOT IN (SELECT gp.game.id FROM GamePlayer gp WHERE gp.player = :player) ORDER BY g.createdAt ASC")
    List<Game> findJoinableGamesForPlayer(@Param("player") Player player, @Param("playerBalance") BigDecimal playerBalance);


}