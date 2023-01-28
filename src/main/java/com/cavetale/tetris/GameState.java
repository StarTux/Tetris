package com.cavetale.tetris;

public enum GameState {
    INIT,
    FALL,
    LAND,
    CLEAR,
    LOSE,
    DISABLE;

    public boolean isDuringGame() {
        return this == FALL
            || this == LAND
            || this == CLEAR;
    }
}
