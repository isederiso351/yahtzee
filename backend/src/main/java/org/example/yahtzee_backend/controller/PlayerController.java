package org.example.yahtzee_backend.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.yahtzee_backend.dto.*;
import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.entity.Transaction;
import org.example.yahtzee_backend.service.PlayerService;
import org.example.yahtzee_backend.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/players")
public class PlayerController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @Autowired
    private PlayerService playerService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // === REGISTRAZIONE E AUTENTICAZIONE ===

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody PlayerRegistrationDto registrationDto) {
        try {
            // Verifica disponibilità username/email
            if (!playerService.isUsernameAvailable(registrationDto.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Username already exists"));
            }

            if (!playerService.isEmailAvailable(registrationDto.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email already exists"));
            }

            // Registra il giocatore
            Player player = playerService.registerPlayer(
                    registrationDto.getUsername(),
                    registrationDto.getEmail(),
                    registrationDto.getPassword(),
                    passwordEncoder
            );

            logger.info("New player registered: {}", player.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Player registered successfully",
                            convertToProfileDto(player)));

        } catch (Exception e) {
            logger.error("Registration failed for {}: {}",
                    registrationDto.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody PlayerLoginDto loginDto,
                                   HttpServletRequest request) {
        try {
            // Crea token di autenticazione
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()
                    );

            // Spring Security verifica username/password
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Crea/aggiorna sessione
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Aggiorna ultimo login
            Player player = playerService.findByUsername(loginDto.getUsername()).get();
            playerService.recordPlayerLogin(player);

            logger.info("Player {} logged in successfully", player.getUsername());

            return ResponseEntity.ok(new ApiResponse(true, "Login successful",
                    convertToProfileDto(player)));

        } catch (AuthenticationException e) {
            logger.warn("Failed login attempt for username: {}", loginDto.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid username or password"));
        } catch (Exception e) {
            logger.error("Login error for {}: {}", loginDto.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Login failed"));
        }
    }

    // === GESTIONE PROFILO ===

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            logger.info("User logged out successfully");
            return ResponseEntity.ok(new ApiResponse(true, "Logout successful"));

        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Logout failed"));
        }
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<?> getPlayerProfile(@PathVariable Long playerId,
                                              Authentication authentication) {
        try {
            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved",
                    convertToProfileDto(currentPlayer)));

        } catch(AccessDeniedException e){
            logger.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        } catch (Exception e) {
            logger.error("Error retrieving profile for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving profile"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            Player currentPlayer = playerService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

            return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved",
                    convertToProfileDto(currentPlayer)));

        } catch (Exception e) {
            logger.error("Error retrieving current user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving profile"));
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getPlayerByUsername(@PathVariable String username) {
        try {
            Optional<Player> playerOpt = playerService.findByUsername(username);

            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                return ResponseEntity.ok(new ApiResponse(true, "Player found",
                        convertToProfileDto(player)));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving player by username {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving player"));
        }
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<?> getPlayerStats(@PathVariable Long playerId,
                                            Authentication authentication) {
        try {
            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            PlayerStatsDto stats = convertToStatsDto(currentPlayer);
            return ResponseEntity.ok(new ApiResponse(true, "Stats retrieved", stats));

        } catch(AccessDeniedException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        }catch (Exception e) {
            logger.error("Error retrieving stats for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving stats"));
        }
    }

    // === GESTIONE SALDO ===

    @PostMapping("/{playerId}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long playerId,
                                     @Valid @RequestBody BalanceOperationDto depositDto,
                                     Authentication authentication) {
        try {
            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            if (!currentPlayer.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Account is deactivated"));
            }

            playerService.deposit(currentPlayer, depositDto.getAmount(), depositDto.getDescription());

            // Refresh player per saldo aggiornato
            currentPlayer = playerService.refreshPlayer(currentPlayer);

            logger.info("Deposit of {} completed for player {}",
                    depositDto.getAmount(), currentPlayer.getUsername());

            return ResponseEntity.ok(new ApiResponse(true, "Deposit successful",
                    convertToProfileDto(currentPlayer)));

        } catch(AccessDeniedException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Deposit failed for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Deposit failed"));
        }
    }

    @PostMapping("/{playerId}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long playerId,
                                      @Valid @RequestBody BalanceOperationDto withdrawDto,
                                      Authentication authentication) {
        try {
            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            if (!currentPlayer.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Account is deactivated"));
            }

            playerService.withdrawal(currentPlayer, withdrawDto.getAmount(), withdrawDto.getDescription());

            // Refresh player per saldo aggiornato
            currentPlayer = playerService.refreshPlayer(currentPlayer);

            logger.info("Withdrawal of {} completed for player {}",
                    withdrawDto.getAmount(), currentPlayer.getUsername());

            return ResponseEntity.ok(new ApiResponse(true, "Withdrawal successful",
                    convertToProfileDto(currentPlayer)));

        } catch(AccessDeniedException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Withdrawal failed for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Withdrawal failed"));
        }
    }

    @GetMapping("/{playerId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long playerId,
                                        Authentication authentication) {
        try {

            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            BigDecimal balance = playerService.getPlayerBalance(currentPlayer);

            return ResponseEntity.ok(new ApiResponse(true, "Balance retrieved",
                    Collections.singletonMap("balance", balance)));

        } catch(AccessDeniedException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        }catch (Exception e) {
            logger.error("Error retrieving balance for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving balance"));
        }
    }

    // === TRANSAZIONI ===

    @GetMapping("/{playerId}/transactions")
    public ResponseEntity<?> getPlayerTransactions(@PathVariable Long playerId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   Authentication authentication) {
        try {
            Player currentPlayer = getAuthorizedPlayer(authentication, playerId);

            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = transactionService.getPlayerTransactions(currentPlayer, pageable);

            return ResponseEntity.ok(new ApiResponse(true, "Transactions retrieved",
                    Collections.singletonMap("transactions",
                            Collections.singletonMap("content", transactions.getContent()))));

        } catch(AccessDeniedException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        } catch (Exception e) {
            logger.error("Error retrieving transactions for player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving transactions"));
        }
    }

    // === LEADERBOARD E RICERCHE ===

    @GetMapping("/search")
    public ResponseEntity<?> searchPlayers(@RequestParam String username) {
        try {
            List<Player> players = playerService.searchPlayersByUsername(username);

            List<PlayerProfileDto> playerDtos = players.stream()
                    .map(this::convertToProfileDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Search completed", playerDtos));

        } catch (Exception e) {
            logger.error("Error searching players with username {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Search failed"));
        }
    }

    @GetMapping("/leaderboard/winrate")
    public ResponseEntity<?> getTopPlayersByWinRate(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Player> players = playerService.getTopPlayersByWinRate()
                    .stream()
                    .limit(limit)
                    .toList();

            List<PlayerStatsDto> playerStats = players.stream()
                    .map(this::convertToStatsDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Leaderboard retrieved", playerStats));

        } catch (Exception e) {
            logger.error("Error retrieving win rate leaderboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving leaderboard"));
        }
    }

    @GetMapping("/leaderboard/earnings")
    public ResponseEntity<?> getTopPlayersByEarnings(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Player> players = playerService.getTopPlayersByNetEarnings()
                    .stream()
                    .limit(limit)
                    .toList();

            List<PlayerStatsDto> playerStats = players.stream()
                    .map(this::convertToStatsDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Earnings leaderboard retrieved", playerStats));

        } catch (Exception e) {
            logger.error("Error retrieving earnings leaderboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving leaderboard"));
        }
    }

    @GetMapping("/leaderboard/highscore")
    public ResponseEntity<?> getTopPlayersByHighScore(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Player> players = playerService.getTopPlayersByHighestScore()
                    .stream()
                    .limit(limit)
                    .toList();

            List<PlayerStatsDto> playerStats = players.stream()
                    .map(this::convertToStatsDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "High score leaderboard retrieved", playerStats));

        } catch (Exception e) {
            logger.error("Error retrieving high score leaderboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving leaderboard"));
        }
    }

    // === VALIDAZIONI ===

    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username) {
        try {
            boolean available = playerService.isUsernameAvailable(username);
            return ResponseEntity.ok(new ApiResponse(true, "Username check completed",
                    Collections.singletonMap("available", available)));

        } catch (Exception e) {
            logger.error("Error checking username availability: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error checking username"));
        }
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmailAvailability(@PathVariable String email) {
        try {
            boolean available = playerService.isEmailAvailable(email);
            return ResponseEntity.ok(new ApiResponse(true, "Email check completed",
                    Collections.singletonMap("available", available)));

        } catch (Exception e) {
            logger.error("Error checking email availability: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error checking email"));
        }
    }

    // === ADMIN ENDPOINTS ===

    @PostMapping("/{playerId}/deactivate")
    public ResponseEntity<?> deactivatePlayer(@PathVariable Long playerId,
                                              @RequestBody Map<String, String> request) {
        try {
            Optional<Player> playerOpt = playerService.findById(playerId);

            if (!playerOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Player player = playerOpt.get();
            String reason = request.getOrDefault("reason", "Admin action");

            playerService.deactivatePlayer(player, reason);

            logger.warn("Player {} deactivated by admin: {}", player.getUsername(), reason);

            return ResponseEntity.ok(new ApiResponse(true, "Player deactivated successfully"));

        } catch (Exception e) {
            logger.error("Error deactivating player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error deactivating player"));
        }
    }

    @PostMapping("/{playerId}/reactivate")
    public ResponseEntity<?> reactivatePlayer(@PathVariable Long playerId) {
        try {
            Optional<Player> playerOpt = playerService.findById(playerId);

            if (!playerOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Player player = playerOpt.get();
            playerService.reactivatePlayer(player);

            logger.info("Player {} reactivated by admin", player.getUsername());

            return ResponseEntity.ok(new ApiResponse(true, "Player reactivated successfully"));

        } catch (Exception e) {
            logger.error("Error reactivating player {}: {}", playerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error reactivating player"));
        }
    }

    // === UTILITY METHODS ===

    private Player getAuthorizedPlayer(Authentication authentication, Long expectedId) {
        String username = authentication.getName();
        Player player = playerService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!player.getId().equals(expectedId)) {
            throw new AccessDeniedException("User "+username+" tried to access profile of user "+expectedId);
        }

        return player;
    }


    private PlayerProfileDto convertToProfileDto(Player player) {
        PlayerProfileDto dto = new PlayerProfileDto();
        dto.setId(player.getId());
        dto.setUsername(player.getUsername());
        dto.setEmail(player.getEmail());
        dto.setBalance(player.getBalance());
        dto.setIsActive(player.getIsActive());
        dto.setCreatedAt(player.getCreatedAt());
        dto.setLastLogin(player.getLastLogin());
        dto.setLastActivity(player.getLastActivity());
        return dto;
    }

    private PlayerStatsDto convertToStatsDto(Player player) {
        PlayerStatsDto dto = new PlayerStatsDto();
        dto.setId(player.getId());
        dto.setUsername(player.getUsername());
        dto.setGamesPlayed(player.getGamesPlayed());
        dto.setGamesWon(player.getGamesWon());
        dto.setGamesLost(player.getGamesLost());
        dto.setWinRate(playerService.getWinRate(player));
        dto.setTotalEarnings(player.getTotalEarnings());
        dto.setTotalLosses(player.getTotalLosses());
        dto.setNetEarnings(playerService.getNetEarnings(player));
        dto.setHighestScore(player.getHighestScore());
        dto.setBalance(player.getBalance());
        return dto;
    }
}