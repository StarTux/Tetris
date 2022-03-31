package com.cavetale.tetris;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Hotbar {
    TURN_LEFT(0, Mytems.TURN_LEFT),
    NEUTRAL(1, null),
    TURN_RIGHT(2, Mytems.TURN_RIGHT),
    LEFT(6, Mytems.ARROW_LEFT),
    DOWN(7, Mytems.ARROW_DOWN),
    RIGHT(8, Mytems.ARROW_RIGHT),
    ;

    public final int slot;
    public final Mytems mytems;

    public static Hotbar ofSlot(int slot) {
        for (Hotbar it : Hotbar.values()) {
            if (it.slot == slot) return it;
        }
        return null;
    }
}
