package com.cavetale.tetris;

import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@Getter
public final class TetrisPlayer {
    public final UUID uuid;
    @Getter @Setter private String name = "";
    @Getter @Setter private TetrisGame game;

    public TetrisPlayer(final Player player) {
        this.uuid = player.getUniqueId();
    }

    public void enable(Player p) {
        this.name = p.getName();
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    public void disable() {
        if (game != null) game.disable();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void applyPlayer(Consumer<Player> callback) {
        Player player = getPlayer();
        if (player != null) callback.accept(player);
    }

    public boolean is(Player p) {
        return uuid.equals(p.getUniqueId());
    }

    public boolean is(UUID u) {
        return uuid.equals(u);
    }
}
