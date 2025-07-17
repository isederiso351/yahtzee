package org.example.yahtzee_backend.repository;

import org.example.yahtzee_backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Ricerca per username
    Optional<Player> findByUsername(String username);

    // Ricerca per email
    Optional<Player> findByEmail(String email);

    // Verifica se username esiste
    boolean existsByUsername(String username);

    // Verifica se email esiste
    boolean existsByEmail(String email);

    // Trova giocatori attivi
    List<Player> findByIsActiveTrue();

    // Trova giocatori con saldo minimo
    List<Player> findByBalanceGreaterThanEqual(BigDecimal minBalance);

    // Trova giocatori che possono permettersi una certa scommessa
    @Query("SELECT p FROM Player p WHERE p.balance >= :betAmount AND p.isActive = true")
    List<Player> findPlayersWhoCanAffordBet(@Param("betAmount") BigDecimal betAmount);

    // Top giocatori per win rate
    @Query("SELECT p FROM Player p WHERE p.gamesPlayed > 0 ORDER BY (CAST(p.gamesWon AS double) / p.gamesPlayed) DESC")
    List<Player> findTopPlayersByWinRate();

    // Top giocatori per guadagni netti
    @Query("SELECT p FROM Player p ORDER BY (p.totalEarnings - p.totalLosses) DESC")
    List<Player> findTopPlayersByNetEarnings();

    // Top giocatori per punteggio più alto
    List<Player> findTop10ByOrderByHighestScoreDesc();

    // Giocatori più attivi
    List<Player> findTop10ByOrderByGamesPlayedDesc();

    // Cerca giocatori per username (case insensitive)
    @Query("SELECT p FROM Player p WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :username, '%'))")
    List<Player> findByUsernameContainingIgnoreCase(@Param("username") String username);

    // Statistiche generali
    @Query("SELECT COUNT(p) FROM Player p WHERE p.isActive = true")
    Long countActiveUsers();

    @Query("SELECT AVG(p.balance) FROM Player p WHERE p.isActive = true")
    BigDecimal getAverageBalance();

    @Query("SELECT SUM(p.balance) FROM Player p WHERE p.isActive = true")
    BigDecimal getTotalBalanceInSystem();
}