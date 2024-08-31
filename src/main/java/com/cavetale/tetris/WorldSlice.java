package com.cavetale.tetris;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A slice of the world for the Allocator.
 */
@RequiredArgsConstructor
public final class WorldSlice {
    public final int x;
    public final int z;
    public final int size; // x,z
    @Getter @Setter private TetrisGame game;

    public boolean isUsed() {
        return game != null;
    }
}
