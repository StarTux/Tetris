package com.cavetale.tetris;

import com.cavetale.mytems.util.BlockColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TetrisBoard {
    public final int width;
    public final int height;
    public final int area;
    public final int[] board;

    public TetrisBoard(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.area = width * height;
        this.board = new int[area];
    }

    public TetrisBoard(final int width, final int height, final int[] mirrorBoard) {
        this.width = width;
        this.height = height;
        this.area = width * height;
        this.board = new int[mirrorBoard.length];
        if (board.length != area) {
            throw new IllegalStateException(width + " x " + height + " != " + board.length);
        }
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                int y2 = height - y - 1;
                board[x + y2 * width] = mirrorBoard[x + y * width];
            }
        }
    }

    public TetrisBoard(final TetrisBoard other) {
        this.width = other.width;
        this.height = other.height;
        this.area = other.area;
        this.board = Arrays.copyOf(other.board, other.board.length);
    }

    public int get(int x, int y) {
        if (x < 0 || x >= width) return 0;
        if (y < 0 || y >= height) return 0;
        return board[x + y * width];
    }

    public void set(int x, int y, int c) {
        if (x < 0 || x >= width) return;
        if (y < 0 || y >= height) return;
        board[x + y * width] = c;
    }

    public BlockColor getColor(int x, int y) {
        int value = get(x, y);
        return value == 0
            ? null
            : BlockColor.values()[value - 1];
    }

    public void setColor(int x, int y, BlockColor color) {
        int value = color == null
            ? 0
            : color.ordinal() + 1;
        set(x, y, value);
    }

    /**
     * Paste other board onto this.
     */
    public void paste(TetrisBoard other, int offx, int offy) {
        for (int y = 0; y < other.height; y += 1) {
            for (int x = 0; x < other.width; x += 1) {
                int value = other.get(x, y);
                if (value == 0) continue;
                set(x + offx, y + offy, value);
            }
        }
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(int x, int y, BlockColor color);
    }

    public void forEachCell(CellConsumer cc) {
         for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                cc.accept(x, y, getColor(x, y));
            }
         }
    }

    public void dropRow(int offy) {
        for (int y = offy; y < height - 1; y += 1) {
            for (int x = 0; x < width; x += 1) {
                set(x, y, get(x, y + 1));
            }
        }
        for (int x = 0; x < width; x += 1) {
            set(x, height - 1, 0);
        }
    }

    public void clearRow(int y) {
        for (int x = 0; x < width; x += 1) {
            set(x, y, 0);
        }
    }

    @FunctionalInterface
    public interface CellFunction {
        BlockColor apply(int x, int y, BlockColor color);
    }

    public void applyCells(CellFunction cf) {
         for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                BlockColor newColor = cf.apply(x, y, getColor(x, y));
                setColor(x, y, newColor);
            }
         }
    }

    public void colorize(BlockColor newColor) {
        applyCells((x, y, c) -> c == null ? null : newColor);
    }

    public TetrisBoard clone() {
        return new TetrisBoard(this);
    }

    /**
     * Only works with 4x4 Tetrominos!
     */
    protected TetrisBoard rotate() {
        TetrisBoard result = new TetrisBoard(height, width);
        forEachCell((x, y, color) -> {
                if (color == null) return;
                int nx = y;
                int ny = 3 - x;
                result.setColor(nx, ny, color);
            });
        return result;
    }

    public List<Integer> getFullRows() {
        List<Integer> result = new ArrayList<>();
        ROW: for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                if (get(x, y) == 0) continue ROW;
            }
            result.add(y);
        }
        return result;
    }

    public void shiftUp() {
        for (int y = height - 1; y > 0; y -= 1) {
            for (int x = 0; x < width; x += 1) {
                set(x, y, get(x, y - 1));
            }
        }
        List<Integer> row = new ArrayList<>();
        for (int i = 0; i < width; i += 1) {
            row.add(i <= width / 2 ? BlockColor.BROWN.ordinal() + 1 : 0);
        }
        Collections.shuffle(row, Rnd.get());
        for (int i = 0; i < width; i += 1) {
            board[i] = row.get(i);
        }
    }
}
