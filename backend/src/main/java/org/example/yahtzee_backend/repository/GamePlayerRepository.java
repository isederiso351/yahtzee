package org.example.yahtzee_backend.repository;

import org.example.yahtzee_backend.entity.GamePlayer;
import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    // Trova per gioco e giocatore
    Optional<GamePlayer> findByGameAndPlayer(Game game, Player player);

    // Tutti i giocatori di un gioco
    List<GamePlayer> findByGameOrderByJoinOrder(Game game);

    // Giocatori attivi di un gioco
    List<GamePlayer> findByGameAndIsActiveTrueOrderByJoinOrder(Game game);

    // Tutti i giochi di un giocatore
    List<GamePlayer> findByPlayerOrderByJoinedAtDesc(Player player);

    // Giochi attivi di un giocatore
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.player = :player AND gp.game.status IN ('WAITING', 'IN_PROGRESS')")
    List<GamePlayer> findActiveGamesByPlayer(@Param("player") Player player);

    // Verifica se un giocatore è in un gioco specifico
    boolean existsByGameAndPlayer(Game game, Player player);

    // Conteggio giocatori in un gioco
    Long countByGame(Game game);

    // Conteggio giocatori attivi in un gioco
    Long countByGameAndIsActiveTrue(Game game);

    // Giocatori con punteggio più alto in un gioco
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game = :game ORDER BY gp.totalScore DESC")
    List<GamePlayer> findByGameOrderByTotalScoreDesc(@Param("game") Game game);

    // Primi N giocatori per punteggio in un gioco
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game = :game ORDER BY gp.totalScore DESC")
    List<GamePlayer> findTopScorersInGame(@Param("game") Game game);

    // Statistiche di un giocatore
    @Query("SELECT AVG(gp.totalScore) FROM GamePlayer gp WHERE gp.player = :player AND gp.game.status = 'FINISHED'")
    Double getAverageScoreByPlayer(@Param("player") Player player);

    @Query("SELECT MAX(gp.totalScore) FROM GamePlayer gp WHERE gp.player = :player")
    Integer getHighestScoreByPlayer(@Param("player") Player player);

    // Trova il prossimo giocatore in ordine di turno
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game = :game AND gp.joinOrder = :nextOrder")
    Optional<GamePlayer> findByGameAndJoinOrder(@Param("game") Game game, @Param("nextOrder") Integer nextOrder);

    // Giocatori ordinati per turno
    List<GamePlayer> findByGameOrderByJoinOrderAsc(Game game);

    // Ultima posizione in un gioco (per determinare il prossimo joinOrder)
    @Query("SELECT MAX(gp.joinOrder) FROM GamePlayer gp WHERE gp.game = :game")
    Integer getMaxJoinOrderInGame(@Param("game") Game game);

    // Partecipazioni recenti di un giocatore
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.player = :player ORDER BY gp.joinedAt DESC")
    List<GamePlayer> findRecentGamesByPlayer(@Param("player") Player player);
}