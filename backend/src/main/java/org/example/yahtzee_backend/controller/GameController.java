package org.example.yahtzee_backend.controller;

import org.example.yahtzee_backend.dto.*;
import org.example.yahtzee_backend.entity.Game;
import org.example.yahtzee_backend.entity.Player;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/games")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayerService playerService;

    // === CORE ENDPOINTS ===

    @PostMapping
    public ResponseEntity<?> createGame(@Valid @RequestBody CreateGameDto createGameDto,
                                        Authentication authentication) {
        try {
            Player creator = getCurrentPlayer(authentication);
            Game game = gameService.createGame(creator, createGameDto.getBetAmount(), createGameDto.getMaxPlayers());

            logger.info("Game {} created by {}", game.getGameCode(), creator.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Game created", convertToGameDto(game)));

        } catch (Exception e) {
            logger.error("Error creating game: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<?> joinGame(@PathVariable Long gameId, Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Optional<Game> gameOpt = gameService.findById(gameId);

            if (!gameOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            gameService.joinGame(gameOpt.get(), player);

            logger.info("Player {} joined game {}", player.getUsername(), gameOpt.get().getGameCode());
            return ResponseEntity.ok(new ApiResponse(true, "Joined successfully"));

        } catch (Exception e) {
            logger.error("Error joining game: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<?> startGame(@PathVariable Long gameId, Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Optional<Game> gameOpt = gameService.findById(gameId);

            if (!gameOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Game game = gameOpt.get();

            // Verifica che il player sia nella partita
            if (!gameService.getGamePlayer(game, player).isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "You are not in this game"));
            }

            gameService.startGame(game);

            logger.info("Game {} started", game.getGameCode());
            return ResponseEntity.ok(new ApiResponse(true, "Game started"));

        } catch (Exception e) {
            logger.error("Error starting game: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAvailableGames() {
        try {
            List<Game> games = gameService.findAvailableGames();
            List<GameDto> gameDtos = games.stream()
                    .map(this::convertToGameDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Available games", gameDtos));

        } catch (Exception e) {
            logger.error("Error getting games: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get games"));
        }
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameDetails(@PathVariable Long gameId, Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            Optional<Game> gameOpt = gameService.findById(gameId);

            if (!gameOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Game game = gameOpt.get();

            // Verifica accesso
            if (!gameService.getGamePlayer(game, player).isPresent() &&
                    game.getStatus() != Game.GameStatus.WAITING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Access denied"));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Game details", convertToGameDto(game)));

        } catch (Exception e) {
            logger.error("Error getting game details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get game"));
        }
    }

    @GetMapping("/my-games")
    public ResponseEntity<?> getMyGames(Authentication authentication) {
        try {
            Player player = getCurrentPlayer(authentication);
            List<Game> games = gameService.findPlayerGames(player);

            List<GameDto> gameDtos = games.stream()
                    .map(this::convertToGameDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Your games", gameDtos));

        } catch (Exception e) {
            logger.error("Error getting player games: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get games"));
        }
    }

    // === HELPER METHODS ===

    private Player getCurrentPlayer(Authentication authentication) {
        String username = authentication.getName();
        return playerService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private GameDto convertToGameDto(Game game) {
        GameDto dto = new GameDto();
        dto.setId(game.getId());
        dto.setGameCode(game.getGameCode());
        dto.setStatus(game.getStatus().name());
        dto.setBetAmount(game.getBetAmount());
        dto.setMaxPlayers(game.getMaxPlayers());
        dto.setCurrentRound(game.getCurrentRound());
        dto.setTotalPrize(game.getTotalPrize());
        dto.setCreatedAt(game.getCreatedAt());
        dto.setPlayersCount(game.getPlayersCount());

        if (game.getCurrentPlayer() != null) {
            dto.setCurrentPlayerUsername(game.getCurrentPlayer().getUsername());
        }

        if (game.getWinner() != null) {
            dto.setWinnerUsername(game.getWinner().getUsername());
        }

        return dto;
    }
}