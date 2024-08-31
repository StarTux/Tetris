package com.cavetale.tetris.sql;

import com.cavetale.tetris.TetrisGame;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("scores")
public final class SQLScore implements SQLRow {
    @Id private Integer id;
    @Keyed private UUID player;
    @Default("NOW()") private Date time;
    @Keyed private int score;
    private int lines;
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
