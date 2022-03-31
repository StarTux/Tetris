package com.cavetale.tetris;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import com.cavetale.mytems.util.BlockColor;

public final class Rnd {
    private Rnd() { }

    public static Random get() {
        return ThreadLocalRandom.current();
    }

    public static BlockColor blockColor() {
        BlockColor[] values = BlockColor.values();
        return values[get().nextInt(values.length)];
    }

    public static BlockColor tetrisBlockColor() {
        BlockColor[] values = new BlockColor[] {
            BlockColor.PURPLE,
            BlockColor.PINK,
            BlockColor.LIME,
            BlockColor.YELLOW,
            BlockColor.LIGHT_BLUE,
            BlockColor.MAGENTA,
            BlockColor.ORANGE,
        };
        return values[get().nextInt(values.length)];
    }

    public static Tetromino tetromino() {
        Tetromino[] values = Tetromino.values();
        return values[get().nextInt(values.length)];
    }
}
