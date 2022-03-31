package com.cavetale.tetris;

import com.cavetale.mytems.Mytems;

public enum Tetromino {
    I(Mytems.TETRIS_I),
    O(Mytems.TETRIS_O),
    T(Mytems.TETRIS_T),
    L(Mytems.TETRIS_L),
    J(Mytems.TETRIS_J),
    S(Mytems.TETRIS_S),
    Z(Mytems.TETRIS_Z);

    public final Mytems mytems;
    public final TetrisBoard[] boards = new TetrisBoard[4];

    Tetromino(final Mytems mytems) {
        this.mytems = mytems;
    }

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
