package com.cavetale.tetris;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter @RequiredArgsConstructor
public final class TetrisMatch {
    private final TetrisPlugin plugin;
    private final Set<UUID> joined = new HashSet<>();
    private boolean enabled;
    private int ticks;
    private static final int MAX_TICKS = 20 * 60;

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
        joined.clear();
    }

    protected void tick() {
        ticks += 1;
        if (ticks >= MAX_TICKS) start();
    }

    public float getProgress() {
        return (float) ticks / (float) MAX_TICKS;
    }

    private void start() {
        List<Player> players = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (UUID uuid : joined) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            if (plugin.gameOf(player) != null) continue;
            players.add(player);
            names.add(player.getName());
        }
        if (players.isEmpty()) return;
        TetrisBattle battle = new TetrisBattle();
        for (Player player : players) {
            TetrisGame game = plugin.startGame(player);
            battle.getGames().add(game);
            game.setBattle(battle);
            player.sendMessage(text("Starting battle with " + String.join(", ", names), GREEN));
        }
        disable();
        plugin.getLogger().info("[Match] Battle started: " + String.join(", ", names));
    }
}
