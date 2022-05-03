package com.cavetale.tetris.sql;

import com.cavetale.tetris.TetrisGame;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Data;

@Data
@Table(name = "scores",
       indexes = {
           @Index(name = "score", columnList = "score", unique = false),
           @Index(name = "player", columnList = "player", unique = false),
       })
public final class SQLScore {
    @Id
    private Integer id;
    @Column(nullable = false)
    private UUID player;
    @Column(nullable = false)
    private Date time;
    @Column(nullable = false)
    private int score;
    @Column(nullable = false)
    private int lines;
    @Column(nullable = false)
    private int level;

    public SQLScore() { }

    public SQLScore(final TetrisGame game) {
        this.player = game.getPlayer().getUuid();
        this.time = new Date();
        this.score = game.getScore();
        this.lines = game.getLines();
        this.level = game.getLevel();
    }
}
