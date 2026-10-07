package com.example.gamedemo;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GameController {

    private final JdbcTemplate jdbcTemplate;


    public GameController(
        JdbcTemplate jdbcTemplate
    ) {

        this.jdbcTemplate =
            jdbcTemplate;

        createTable();
    }


    private void createTable() {

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS scores (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                score INT NOT NULL,
                difficulty VARCHAR(20) NOT NULL
            )
            """
        );
    }


    @GetMapping("/config/{difficulty}")
    public GameConfig getConfig(
        @PathVariable String difficulty
    ) {

        String selectedDifficulty =
            difficulty.toLowerCase();


        if (
            selectedDifficulty.equals("easy")
        ) {

            return new GameConfig(
                "Spring Boot",
                "Easy",
                30,
                110,
                0
            );
        }


        if (
            selectedDifficulty.equals("hard")
        ) {

            return new GameConfig(
                "Spring Boot",
                "Hard",
                20,
                50,
                850
            );
        }


        return new GameConfig(
            "Spring Boot",
            "Medium",
            25,
            75,
            1600
        );
    }


    @PostMapping("/score")
    public ResponseEntity<?> saveScore(
        @RequestBody ScoreRequest request
    ) {

        jdbcTemplate.update(
            """
            INSERT INTO scores
            (name, score, difficulty)
            VALUES (?, ?, ?)
            """,

            request.name(),
            request.score(),
            request.difficulty()
        );


        Integer highScore =
            jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(score), 0)
                FROM scores
                """,

                Integer.class
            );


        return ResponseEntity.ok(
            new ScoreResponse(
                "Great job, " +
                request.name() +
                "! Your score was saved.",

                request.score(),

                highScore
            )
        );
    }


    @GetMapping("/scores")
    public List<ScoreEntry> getAllScores() {

        return jdbcTemplate.query(
            """
            SELECT name, score, difficulty
            FROM scores
            ORDER BY score DESC
            """,

            (rs, rowNum) ->
                new ScoreEntry(
                    rs.getString("name"),
                    rs.getInt("score"),
                    rs.getString("difficulty")
                )
        );
    }


    @GetMapping("/scores/{name}")
    public ResponseEntity<?> getScoresByPlayer(
        @PathVariable String name
    ) {

        List<ScoreEntry> scores =
            jdbcTemplate.query(
                """
                SELECT name, score, difficulty
                FROM scores
                WHERE name = ?
                ORDER BY score DESC
                """,

                (rs, rowNum) ->
                    new ScoreEntry(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getString("difficulty")
                    ),

                name
            );


        if (scores.isEmpty()) {

            return ResponseEntity
                .status(404)
                .body(
                    new ErrorResponse(
                        "No scores found for " +
                        name
                    )
                );
        }


        return ResponseEntity.ok(scores);
    }


    @PostMapping("/scores/reset")
    public ResponseEntity<?> resetScores() {

        jdbcTemplate.update(
            "DELETE FROM scores"
        );


        return ResponseEntity.ok(
            Map.of(
                "message",
                "All scores have been reset."
            )
        );
    }


    public record GameConfig(
        String backend,
        String difficulty,
        int gameTime,
        int targetSize,
        int targetSpeed
    ) {}


    public record ScoreRequest(
        String name,
        int score,
        String difficulty
    ) {}


    public record ScoreResponse(
        String message,
        int score,
        int highScore
    ) {}


    public record ScoreEntry(
        String name,
        int score,
        String difficulty
    ) {}


    public record ErrorResponse(
        String message
    ) {}
}