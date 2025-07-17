package org.example.yahtzee_backend.controller;

import org.example.yahtzee_backend.domain.YahtzeeCategory;
import org.example.yahtzee_backend.dto.*;
import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.GameRound;
import org.example.yahtzee_backend.entity.Player;
import org.example.yahtzee_backend.service.GameRoundService;
import org.example.yahtzee_backend.service.GameService;
import org.example.yahtzee_backend.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/games/{gameId}/rounds")
public class GameRoundController {

    private static final Logger logger = LoggerFactory.getLogger(GameRoundController.class);

    @Autowired
    private GameRoundService gameRoundService;

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayerService playerService;

    // === CORE GAMEPLAY ===

    @PostMapping("/start")
    public ResponseEntity<?> startRound(@PathVariable Long gameId,
                                        @Valid @RequestBody StartRoundDto startRoundDto,
                                        Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Game game = getGameAndValidateAccess(gameId, player);

            if (!gameService.isPlayerTurn(game, player)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Not your turn"));
            }

            GameRound gameRound = gameRoundService.startNewRound(game, player, startRoundDto.getRoundNumber());

            logger.info("Round {} started for {} in game {}",
                    startRoundDto.getRoundNumber(), player.getUsername(), game.getGameCode());

            return ResponseEntity.ok(new ApiResponse(true, "Round started", convertToRoundDto(gameRound)));

        } catch (Exception e) {
            logger.error("Error starting round: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{roundId}/roll")
    public ResponseEntity<?> rollDice(@PathVariable Long gameId,
                                      @PathVariable Long roundId,
                                      @RequestBody(required = false) RollDiceDto rollDiceDto,
                                      Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Game game = getGameAndValidateAccess(gameId, player);
            GameRound gameRound = getGameRound(roundId, player);

            if (!gameService.isPlayerTurn(game, player)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Not your turn"));
            }

            GameRound updatedRound;
            if (rollDiceDto != null && rollDiceDto.getKeepDice() != null) {
                updatedRound = gameRoundService.rollDice(gameRound, rollDiceDto.getKeepDice());
            } else {
                updatedRound = gameRoundService.rollDice(gameRound);
            }

            logger.info("Dice rolled for {} in game {} (roll {}/3)",
                    player.getUsername(), game.getGameCode(), updatedRound.getDiceRolls().size());

            return ResponseEntity.ok(new ApiResponse(true, "Dice rolled", convertToRoundDto(updatedRound)));

        } catch (Exception e) {
            logger.error("Error rolling dice: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{roundId}/complete")
    public ResponseEntity<?> completeRound(@PathVariable Long gameId,
                                           @PathVariable Long roundId,
                                           @Valid @RequestBody CompleteRoundDto completeRoundDto,
                                           Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Game game = getGameAndValidateAccess(gameId, player);
            GameRound gameRound = getGameRound(roundId, player);

            if (!gameService.isPlayerTurn(game, player)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Not your turn"));
            }

            YahtzeeCategory category = YahtzeeCategory.valueOf(completeRoundDto.getCategory());
            GameRound completedRound = gameRoundService.completeRound(gameRound, category);

            // Avanza al prossimo player
            gameService.advanceToNextPlayer(game);

            logger.info("Round completed by {} with {} for {} points",
                    player.getUsername(), category.getDisplayName(), completedRound.getScore());

            return ResponseEntity.ok(new ApiResponse(true, "Round completed", convertToRoundDto(completedRound)));

        } catch (Exception e) {
            logger.error("Error completing round: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRound(@PathVariable Long gameId, Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Game game = getGameAndValidateAccess(gameId, player);

            Optional<GameRound> currentRoundOpt = gameRoundService.getCurrentRoundForPlayer(game, player);

            if (currentRoundOpt.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Current round",
                        convertToRoundDto(currentRoundOpt.get())));
            } else {
                return ResponseEntity.ok(new ApiResponse(true, "No active round", null));
            }

        } catch (Exception e) {
            logger.error("Error getting current round: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get round"));
        }
    }

    // === HELPER METHODS ===

    private Player getCurrentPlayer(Authentication authentication) {
        String username = authentication.getName();
        return playerService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private Game getGameAndValidateAccess(Long gameId, Player player) {
        Optional<Game> gameOpt = gameService.findById(gameId);
        if (!gameOpt.isPresent()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();
        if (!gameService.getGamePlayer(game, player).isPresent()) {
            throw new IllegalArgumentException("You are not in this game");
        }

        return game;
    }

    private GameRound getGameRound(Long roundId, Player player) {
        // Semplificato - in un vero sistema dovresti fare query su GameRoundRepository
        // Per ora assumiamo che sia valido se appartiene al player
        return new GameRound(); // Placeholder - implementare la logica reale
    }

    private RoundDto convertToRoundDto(GameRound gameRound) {
        RoundDto dto = new RoundDto();
        dto.setId(gameRound.getId());
        dto.setRoundNumber(gameRound.getRoundNumber());
        dto.setDiceRolls(gameRound.getDiceRolls());
        dto.setRollCount(gameRound.getRollCount());
        dto.setCanRollAgain(gameRoundService.canPlayerRollAgain(gameRound));
        dto.setIsCompleted(gameRound.getIsCompleted());
        dto.setScore(gameRound.getScore());

        if (gameRound.getSelectedCategory() != null) {
            dto.setSelectedCategory(gameRound.getSelectedCategory().name());
        }

        return dto;
    }
}