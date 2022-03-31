package com.cavetale.tetris;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public enum Hotbar {
    TURN_LEFT(0, Mytems.TURN_LEFT, text("Turn Left", GREEN)),
    NEUTRAL(1, null, null),
    TURN_RIGHT(2, Mytems.TURN_RIGHT, text("Turn Right", GREEN)),
    HOME(4, Mytems.REDO, text("Warp Home", BLUE)),
    LEFT(6, Mytems.ARROW_LEFT, text("Left", GREEN)),
    DOWN(7, Mytems.ARROW_DOWN, text("Down", GREEN)),
    RIGHT(8, Mytems.ARROW_RIGHT, text("Right", GREEN)),
    ;

    public final int slot;
    public final Mytems mytems;
    public final Component text;

    public static Hotbar ofSlot(int slot) {
        for (Hotbar it : Hotbar.values()) {
            if (it.slot == slot) return it;
        }
        return null;
    }
}
