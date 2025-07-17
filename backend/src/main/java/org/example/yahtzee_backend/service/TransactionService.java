package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.entity.Transaction;
import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    // === CREAZIONE TRANSAZIONI ===

    public Transaction createTransaction(Player player, Game game, Transaction.TransactionType type,
                                         BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        Transaction transaction = new Transaction();
        transaction.setPlayer(player);
        transaction.setGame(game);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setBalanceAfter(player.getBalance());

        Transaction savedTransaction = transactionRepository.save(transaction);

        logger.info("Transaction created: {} - {} {} for player {} (Balance after: {})",
                savedTransaction.getId(), type, amount, player.getUsername(), player.getBalance());

        return savedTransaction;
    }

    // === METODI SPECIFICI PER TIPI DI TRANSAZIONE ===

    public Transaction createBetTransaction(Player player, Game game, BigDecimal amount) {
        String description = String.format("Bet placed for game %s", game.getGameCode());

        Transaction transaction = createTransaction(player, game, Transaction.TransactionType.BET, amount, description);

        logger.info("Bet transaction: Player {} bet {} on game {}",
                player.getUsername(), amount, game.getGameCode());

        return transaction;
    }

    public Transaction createWinTransaction(Player player, Game game, BigDecimal amount) {
        String description = String.format("Prize won from game %s", game.getGameCode());

        Transaction transaction = createTransaction(player, game, Transaction.TransactionType.WIN, amount, description);

        logger.info("Win transaction: Player {} won {} from game {}",
                player.getUsername(), amount, game.getGameCode());

        return transaction;
    }

    public Transaction createLoseTransaction(Player player, Game game, BigDecimal amount) {
        String description = String.format("Loss from game %s", game.getGameCode());

        Transaction transaction = createTransaction(player, game, Transaction.TransactionType.LOSE, amount, description);

        logger.debug("Loss transaction: Player {} lost {} in game {}",
                player.getUsername(), amount, game.getGameCode());

        return transaction;
    }

    public Transaction createRefundTransaction(Player player, Game game, BigDecimal amount, String reason) {
        String description = String.format("Refund for game %s: %s", game.getGameCode(), reason);

        Transaction transaction = createTransaction(player, game, Transaction.TransactionType.REFUND, amount, description);

        logger.info("Refund transaction: Player {} refunded {} for game {} - {}",
                player.getUsername(), amount, game.getGameCode(), reason);

        return transaction;
    }

    public Transaction createDepositTransaction(Player player, BigDecimal amount, String description) {
        Transaction transaction = createTransaction(player, null, Transaction.TransactionType.DEPOSIT, amount, description);

        logger.info("Deposit transaction: Player {} deposited {} - {}",
                player.getUsername(), amount, description);

        return transaction;
    }

    public Transaction createWithdrawalTransaction(Player player, BigDecimal amount, String description) {
        Transaction transaction = createTransaction(player, null, Transaction.TransactionType.WITHDRAWAL, amount, description);

        logger.info("Withdrawal transaction: Player {} withdrew {} - {}",
                player.getUsername(), amount, description);

        return transaction;
    }

    public Transaction createBonusTransaction(Player player, BigDecimal amount, String description) {
        Transaction transaction = createTransaction(player, null, Transaction.TransactionType.BONUS, amount, description);

        logger.info("Bonus transaction: Player {} received bonus {} - {}",
                player.getUsername(), amount, description);

        return transaction;
    }

    public Transaction createPenaltyTransaction(Player player, BigDecimal amount, String description) {
        Transaction transaction = createTransaction(player, null, Transaction.TransactionType.PENALTY, amount, description);

        logger.warn("Penalty transaction: Player {} penalized {} - {}",
                player.getUsername(), amount, description);

        return transaction;
    }

    // === QUERY E RICERCHE ===

    public List<Transaction> getPlayerTransactions(Player player) {
        return transactionRepository.findByPlayerOrderByCreatedAtDesc(player);
    }

    public Page<Transaction> getPlayerTransactions(Player player, Pageable pageable) {
        return transactionRepository.findByPlayerOrderByCreatedAtDesc(player, pageable);
    }

    public List<Transaction> getGameTransactions(Game game) {
        return transactionRepository.findByGameOrderByCreatedAtAsc(game);
    }

    public List<Transaction> getTransactionsByType(Transaction.TransactionType type) {
        return transactionRepository.findByTypeOrderByCreatedAtDesc(type);
    }

    public List<Transaction> getPlayerTransactionsByType(Player player, Transaction.TransactionType type) {
        return transactionRepository.findByPlayerAndTypeOrderByCreatedAtDesc(player, type);
    }

    public List<Transaction> getPlayerTransactionsInPeriod(Player player, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByPlayerAndDateRange(player, startDate, endDate);
    }

    public List<Transaction> getTodaysTransactions() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return transactionRepository.findByDateRange(start,end);
    }

    // === CALCOLI E STATISTICHE ===

    public BigDecimal getTotalWinnings(Player player) {
        return transactionRepository.sumAmountByPlayerAndType(player, Transaction.TransactionType.WIN);
    }

    public BigDecimal getTotalLosses(Player player) {
        BigDecimal bets = transactionRepository.sumAmountByPlayerAndType(player, Transaction.TransactionType.BET);
        BigDecimal losses = transactionRepository.sumAmountByPlayerAndType(player, Transaction.TransactionType.LOSE);
        return bets.add(losses);
    }

    public BigDecimal getTotalDeposits(Player player) {
        return transactionRepository.sumAmountByPlayerAndType(player, Transaction.TransactionType.DEPOSIT);
    }

    public BigDecimal getTotalWithdrawals(Player player) {
        return transactionRepository.sumAmountByPlayerAndType(player, Transaction.TransactionType.WITHDRAWAL);
    }

    public BigDecimal getNetGambling(Player player) {
        BigDecimal winnings = getTotalWinnings(player);
        BigDecimal losses = getTotalLosses(player);
        return winnings.subtract(losses);
    }

    public BigDecimal getPlayerNetBalance(Player player) {
        BigDecimal deposits = getTotalDeposits(player);
        BigDecimal withdrawals = getTotalWithdrawals(player);
        BigDecimal netGambling = getNetGambling(player);

        return deposits.subtract(withdrawals).add(netGambling);
    }

    // === VALIDAZIONI E VERIFICHE ===

    public boolean hasPlayerTransactions(Player player) {
        return transactionRepository.existsByPlayer(player);
    }

    public Long getTransactionCount(Player player) {
        return transactionRepository.countByPlayer(player);
    }

    public List<Transaction> getBiggestWins(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return transactionRepository.findBiggestWins(pageable);
    }

    // === AUDIT E CONTROLLI ===

    public boolean validatePlayerBalance(Player player) {
        try {
            BigDecimal calculatedBalance = getPlayerNetBalance(player);
            BigDecimal actualBalance = player.getBalance();

            // Tolleranza di 0.01 per arrotondamenti (non dovrebbe servire, ma per sicurezza)
            BigDecimal difference = calculatedBalance.subtract(actualBalance).abs();
            boolean isValid = difference.compareTo(BigDecimal.valueOf(0.01)) <= 0;

            if (!isValid) {
                logger.error("Balance mismatch for player {}: calculated={}, actual={}, difference={}",
                        player.getUsername(), calculatedBalance, actualBalance, difference);
            } else {
                logger.debug("Balance validation OK for player {}: {}", player.getUsername(), actualBalance);
            }

            return isValid;
        } catch (Exception e) {
            logger.error("Error validating balance for player {}: {}", player.getUsername(), e.getMessage());
            return false;
        }
    }

    public void logTransactionSummary(Player player) {
        logger.info("Transaction summary for {}: Deposits={}, Withdrawals={}, Winnings={}, Losses={}, Net={}",
                player.getUsername(),
                getTotalDeposits(player),
                getTotalWithdrawals(player),
                getTotalWinnings(player),
                getTotalLosses(player),
                getNetGambling(player));
    }

    // === UTILITY ===

    public List<Transaction> getRecentTransactions(Player player, int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return transactionRepository.findByPlayerOrderByCreatedAtDesc(player, pageable).getContent();
    }
}