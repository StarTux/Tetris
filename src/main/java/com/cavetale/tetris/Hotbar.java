package com.cavetale.tetris;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.keybind;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * All hotbar slots.  Ordered how they should be displayed in
 * TetrisGame#educate.
 */
@RequiredArgsConstructor
public enum Hotbar {
    LEFT(0, Mytems.ARROW_LEFT, text("Left", GREEN)),
    DOWN(1, Mytems.ARROW_DOWN, text("Down", GREEN)),
    RIGHT(2, Mytems.ARROW_RIGHT, text("Right", GREEN)),
    EYE(3, Mytems.BLIND_EYE, text("Hide Players", BLUE)),
    HOME(4, Mytems.REDO, text("Warp Home", BLUE)),
    LOCK(5, Mytems.SILVER_KEYHOLE, text("Unlock Position", GREEN)),
    TURN_LEFT(6, Mytems.TURN_LEFT, text("Turn Left", GREEN)),
    TURN_RIGHT(8, Mytems.TURN_RIGHT, text("Turn Right", GREEN)),
    NEUTRAL(7, Mytems.INVISIBLE_ITEM, textOfChildren(text("[", GRAY),
                                                     keybind("key.drop", GREEN),
                                                     text("] Drop [", GRAY),
                                                     keybind("key.swapOffhand", GREEN),
                                                     text("] Swap", GRAY))),
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
