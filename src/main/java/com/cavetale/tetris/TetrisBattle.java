package com.cavetale.tetris;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class TetrisBattle {
    private boolean active;
    private final List<TetrisGame> games = new ArrayList<>();

    public void enable() {
        active = true;
    }

    public void disable() {
        for (TetrisGame game : games) {
            if (game.getBattle() == this) {
                game.setBattle(null);
            }
        }
    }

    public TetrisGame findWinner() {
        if (games.isEmpty()) return null;
        TetrisGame result = null;
        for (TetrisGame it : games) {
            if (!it.isActive()) continue;
            if (result != null) {
                // 2 or more games still active!
                return null;
            }
            result = it;
        }
        return result;
    }
}
