package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.entity.Transaction;
import org.example.yahtzee_backend.repository.PlayerRepository;
import org.example.yahtzee_backend.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // === REGISTRAZIONE E AUTENTICAZIONE ===

    public Player registerPlayer(String username, String email, String password) {
        // Validazioni
        if (playerRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (playerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Player player = new Player();
        player.setUsername(username);
        player.setEmail(email);
        player.setPassword(passwordEncoder.encode(password));
        player.setBalance(BigDecimal.valueOf(1000.00)); // Saldo iniziale
        player.setIsActive(true);

        Player savedPlayer = playerRepository.save(player);

        // Crea transazione di deposito iniziale
        transactionService.createTransaction(
                savedPlayer,
                null,
                Transaction.TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000.00),
                "Welcome bonus - initial deposit"
        );

        logger.info("New player registered: {} ({})", username, email);
        return savedPlayer;
    }

    public Optional<Player> authenticatePlayer(String username, String password) {
        Optional<Player> playerOpt = playerRepository.findByUsername(username);

        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            if (!player.getIsActive()) {
                logger.warn("Login attempt on deactivated account: {}", username);
                throw new IllegalStateException("Account is deactivated");
            }

            if (passwordEncoder.matches(password, player.getPassword())) {
                // Aggiorna ultimo login
                recordPlayerLogin(player);
                logger.info("Player {} logged in successfully", username);
                return Optional.of(player);
            } else {
                logger.warn("Failed login attempt for user: {}", username);
            }
        } else {
            logger.warn("Login attempt for non-existent user: {}", username);
        }

        return Optional.empty();
    }

    public void recordPlayerLogin(Player player) {
        player.updateLastLogin(LocalDateTime.now());
        playerRepository.save(player);
    }

    public void recordPlayerActivity(Player player) {
        player.updateLastActivity(LocalDateTime.now());
        playerRepository.save(player);
    }

    // === GESTIONE SALDI ===

    public boolean hasEnoughBalance(Player player, BigDecimal amount) {
        return player.hasEnoughBalance(amount);
    }

    public void deposit(Player player, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        player.addToBalance(amount);
        playerRepository.save(player);

        // Crea transazione
        transactionService.createTransaction(
                player,
                null,
                Transaction.TransactionType.DEPOSIT,
                amount,
                description
        );

        logger.info("Deposited {} to player {} balance. New balance: {}", amount, player.getUsername(), player.getBalance());
    }

    public void withdrawal(Player player, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!player.hasEnoughBalance(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        player.subtractFromBalance(amount);
        playerRepository.save(player);

        // Crea transazione
        transactionService.createTransaction(
                player,
                null,
                Transaction.TransactionType.WITHDRAWAL,
                amount,
                description
        );

        logger.info("Withdraw {} from player {} balance. New balance: {}", amount, player.getUsername(), player.getBalance());
    }

    public BigDecimal getPlayerBalance(Player player) {
        // Refresh dal database per essere sicuri
        Player refreshed = playerRepository.findById(player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        return refreshed.getBalance();
    }

    // === STATISTICHE GIOCATORE ===

    public void updatePlayerStats(Player player, boolean won, BigDecimal earning, BigDecimal loss, Integer score) {
        player.incrementGamesPlayed();

        if (won) {
            player.incrementGamesWon();
            if (earning != null) {
                player.setTotalEarnings(player.getTotalEarnings().add(earning));
            }
        } else {
            player.incrementGamesLost();
            if (loss != null) {
                player.setTotalLosses(player.getTotalLosses().add(loss));
            }
        }

        if (score != null && score > player.getHighestScore()) {
            player.setHighestScore(score);
            logger.info("New high score for {}: {}", player.getUsername(), score);
        }

        recordPlayerActivity(player);
        playerRepository.save(player);

        logger.debug("Updated stats for {}: games={}, wins={}, losses={}",
                player.getUsername(), player.getGamesPlayed(), player.getGamesWon(), player.getGamesLost());
    }

    public Double getWinRate(Player player) {
        return player.getWinRate();
    }

    public BigDecimal getNetEarnings(Player player) {
        return player.getNetEarnings();
    }

    // === GESTIONE ACCOUNT ===

    public void deactivatePlayer(Player player, String reason) {
        player.setIsActive(false);
        playerRepository.save(player);

        logger.warn("Player {} deactivated: {}", player.getUsername(), reason);
    }

    public void reactivatePlayer(Player player) {
        player.setIsActive(true);
        playerRepository.save(player);

        logger.info("Player {} reactivated", player.getUsername());
    }

    public List<Player> findInactivePlayersSince(int months) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(months);
        return playerRepository.findInactivePlayersSince(cutoffDate);
    }

    public void autoDeactivateInactivePlayers(int monthsInactive) {
        List<Player> inactivePlayers = findInactivePlayersSince(monthsInactive);

        logger.info("Auto-deactivating {} players inactive for {} months", inactivePlayers.size(), monthsInactive);

        for (Player player : inactivePlayers) {
            deactivatePlayer(player, "Auto-deactivated due to " + monthsInactive + " months of inactivity");
        }
    }

    // === RICERCHE ===

    public Optional<Player> findByUsername(String username) {
        return playerRepository.findByUsername(username);
    }

    public Optional<Player> findByEmail(String email) {
        return playerRepository.findByEmail(email);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    public List<Player> findActivePlayers() {
        return playerRepository.findByIsActiveTrue();
    }

    public List<Player> findPlayersWhoCanAffordBet(BigDecimal betAmount) {
        return playerRepository.findPlayersWhoCanAffordBet(betAmount);
    }

    public List<Player> searchPlayersByUsername(String username) {
        return playerRepository.findByUsernameContainingIgnoreCase(username);
    }

    // === LEADERBOARD ===

    public List<Player> getTopPlayersByWinRate() {
        return playerRepository.findTopPlayersByWinRate();
    }

    public List<Player> getTopPlayersByNetEarnings() {
        return playerRepository.findTopPlayersByNetEarnings();
    }

    public List<Player> getTopPlayersByHighestScore() {
        return playerRepository.findTop10ByOrderByHighestScoreDesc();
    }

    public List<Player> getMostActiveUsers() {
        return playerRepository.findTop10ByOrderByGamesPlayedDesc();
    }

    // === STATISTICHE SISTEMA ===

    public Long getActiveUsersCount() {
        return playerRepository.countActiveUsers();
    }

    public BigDecimal getAverageBalance() {
        return playerRepository.getAverageBalance();
    }

    public BigDecimal getTotalSystemBalance() {
        return playerRepository.getTotalBalanceInSystem();
    }

    // === VALIDAZIONI ===

    public boolean isUsernameAvailable(String username) {
        return !playerRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !playerRepository.existsByEmail(email);
    }

    public boolean isPlayerActive(Player player) {
        return player.getIsActive();
    }

    // === UTILITY ===

    public Player refreshPlayer(Player player) {
        return playerRepository.findById(player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }
}