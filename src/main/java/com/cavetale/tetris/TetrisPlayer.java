package com.cavetale.tetris;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class TetrisPlayer {
    public final UUID uuid;
    public final String name;
    @Getter @Setter private TetrisGame game;

    public TetrisPlayer(final Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public void disable() {
        if (game != null) game.disable();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
