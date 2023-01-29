package com.cavetale.tetris;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.BlockColor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Tetromino {
    I(Mytems.TETRIS_I, BlockColor.YELLOW),
    O(Mytems.TETRIS_O, BlockColor.RED),
    T(Mytems.TETRIS_T, BlockColor.PINK),
    L(Mytems.TETRIS_L, BlockColor.LIGHT_BLUE),
    J(Mytems.TETRIS_J, BlockColor.ORANGE),
    S(Mytems.TETRIS_S, BlockColor.LIME),
    Z(Mytems.TETRIS_Z, BlockColor.LIGHT_GRAY);

    public final Mytems mytems;
    public final BlockColor color;
    public final TetrisBoard[] boards = new TetrisBoard[4];

    private void initialize() {
        TetrisBoard board = makeBoard();
        boards[0] = board;
        for (int i = 1; i < 4; i += 1) {
            board = board.rotate();
            boards[i] = board;
        }
    }

    static {
        for (Tetromino it : Tetromino.values()) {
            it.initialize();
        }
    }

    private TetrisBoard makeBoard() {
        switch (this) {
        case I: return new TetrisBoard(4, 4, new int[] {
                0, 0, 1, 0,
                0, 0, 1, 0,
                0, 0, 1, 0,
                0, 0, 1, 0,
            });
        case O: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 1, 1, 0,
                0, 1, 1, 0,
                0, 0, 0, 0,
            });
        case T: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 1, 1, 1,
                0, 0, 1, 0,
                0, 0, 0, 0,
            });
        case L: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 1, 0, 0,
                0, 1, 0, 0,
                0, 1, 1, 0,
            });
        case J: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 0, 1, 0,
                0, 0, 1, 0,
                0, 1, 1, 0,
            });
        case S: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 0, 1, 1,
                0, 1, 1, 0,
                0, 0, 0, 0,
            });
        case Z: return new TetrisBoard(4, 4, new int[] {
                0, 0, 0, 0,
                0, 1, 1, 0,
                0, 0, 1, 1,
                0, 0, 0, 0,
            });
        default: throw new IllegalStateException("Not implemented: " + this);
        }
    }
}
