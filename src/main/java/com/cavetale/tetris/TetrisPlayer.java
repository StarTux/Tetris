package com.cavetale.tetris;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
public final class TetrisPlayer {
    public final UUID uuid;
    @Getter @Setter private String name = "";
    @Getter @Setter private TetrisGame game;

    public TetrisPlayer(final Player player) {
        this.uuid = player.getUniqueId();
    }

    public void enable(Player player) {
        this.name = player.getName();
    }

    public void disable() {
        if (game != null) game.disable();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
