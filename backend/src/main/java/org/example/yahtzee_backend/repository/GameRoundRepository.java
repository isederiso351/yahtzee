package org.example.yahtzee_backend.repository;

import org.example.yahtzee_backend.entity.GameRound;
import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    // Rounds (turni) di un gioco
    List<GameRound> findByGameOrderByRoundNumberAsc(Game game);

    // Rounds di un giocatore in un gioco
    List<GameRound> findByGameAndPlayerOrderByRoundNumberAsc(Game game, Player player);

    // Round specifico di un giocatore in un gioco
    Optional<GameRound> findByGameAndPlayerAndRoundNumber(Game game, Player player, Integer roundNumber);

    // Rounds completati di un gioco
    List<GameRound> findByGameAndIsCompletedTrueOrderByRoundNumberAsc(Game game);

    // Rounds completati di un giocatore
    List<GameRound> findByPlayerAndIsCompletedTrueOrderByCreatedAtDesc(Player player);

    // Rounds incompleti (in corso)
    List<GameRound> findByGameAndIsCompletedFalseOrderByCreatedAtAsc(Game game);

    // Ultimo round completato di un giocatore in un gioco
    @Query("SELECT gr FROM GameRound gr WHERE gr.game = :game AND gr.player = :player AND gr.isCompleted = true ORDER BY gr.roundNumber DESC")
    Optional<GameRound> findLastCompletedRound(@Param("game") Game game, @Param("player") Player player);

    // Conteggio rounds completati per giocatore in un gioco
    @Query("SELECT COUNT(gr) FROM GameRound gr WHERE gr.game = :game AND gr.player = :player AND gr.isCompleted = true")
    Long countCompletedRounds(@Param("game") Game game, @Param("player") Player player);

    // Rounds di un gioco per round number
    List<GameRound> findByGameAndRoundNumberOrderByPlayerAsc(Game game, Integer roundNumber);

    // Verifica se un giocatore ha completato un round specifico
    @Query("SELECT COUNT(gr) > 0 FROM GameRound gr WHERE gr.game = :game AND gr.player = :player AND gr.roundNumber = :roundNumber AND gr.isCompleted = true")
    boolean hasPlayerCompletedRound(@Param("game") Game game, @Param("player") Player player, @Param("roundNumber") Integer roundNumber);

    // Round attualmente in corso per un giocatore (non completato)
    @Query("SELECT gr FROM GameRound gr WHERE gr.game = :game AND gr.player = :player AND gr.isCompleted = false")
    Optional<GameRound> findCurrentRoundForPlayer(@Param("game") Game game, @Param("player") Player player);

    // Tutti i rounds completati (per statistiche generali)
    List<GameRound> findByIsCompletedTrueOrderByCreatedAtDesc();

    // Rounds per categoria (per statistiche)
    List<GameRound> findBySelectedCategoryAndIsCompletedTrue(GameRound.YahtzeeCategory category);
}