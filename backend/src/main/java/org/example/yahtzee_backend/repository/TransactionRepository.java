package org.example.yahtzee_backend.repository;

import org.example.yahtzee_backend.entity.Transaction;
import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Transazioni per giocatore
    List<Transaction> findByPlayerOrderByCreatedAtDesc(Player player);

    // Transazioni per giocatore con paginazione
    Page<Transaction> findByPlayerOrderByCreatedAtDesc(Player player, Pageable pageable);

    // Transazioni per gioco
    List<Transaction> findByGameOrderByCreatedAtAsc(Game game);

    // Transazioni per tipo
    List<Transaction> findByTypeOrderByCreatedAtDesc(Transaction.TransactionType type);

    // Transazioni per giocatore e tipo
    List<Transaction> findByPlayerAndTypeOrderByCreatedAtDesc(Player player, Transaction.TransactionType type);

    // Transazioni per giocatore in un periodo
    @Query("SELECT t FROM Transaction t WHERE t.player = :player AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByPlayerAndDateRange(@Param("player") Player player,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Somma transazioni per giocatore e tipo
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.player = :player AND t.type = :type")
    BigDecimal sumAmountByPlayerAndType(@Param("player") Player player, @Param("type") Transaction.TransactionType type);

    // Maggiori vincite recenti
    @Query("SELECT t FROM Transaction t WHERE t.type = 'WIN' ORDER BY t.amount DESC")
    List<Transaction> findBiggestWins(Pageable pageable);

    // Transazioni legate a un gioco specifico
    List<Transaction> findByGameOrderByCreatedAt(Game game);

    // Conteggio transazioni per giocatore
    Long countByPlayer(Player player);

    // Verifica se un giocatore ha transazioni
    boolean existsByPlayer(Player player);
}