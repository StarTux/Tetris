package com.cavetale.tetris;

import com.cavetale.mytems.util.BlockColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public final class TetrisBlock {
    private final Tetromino type;
    private final BlockColor color;
    @Getter @Setter private int rotation;
    @Getter @Setter private int x;
    @Getter @Setter private int y;

    public TetrisBoard getBoard(int newRotation) {
        TetrisBoard result = type.boards[newRotation].clone();
        result.colorize(color);
        return result;
    }

    public TetrisBoard getBoard() {
        TetrisBoard result = type.boards[rotation].clone();
        result.colorize(color);
        return result;
    }
}
