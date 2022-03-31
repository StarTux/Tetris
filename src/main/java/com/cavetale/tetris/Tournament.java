package com.cavetale.tetris;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.item.font.Glyph;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Tournament {
    private final TetrisPlugin plugin;
    private Tag tag;
    private BukkitTask task;

    public void enable() {
        load();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void load() {
        tag = Json.load(new File(plugin.getDataFolder(), "tournament.json"), Tag.class, Tag::new);
    }

    public void save() {
        if (tag == null) tag = new Tag();
        plugin.getDataFolder().mkdirs();
        Json.save(new File(plugin.getDataFolder(), "tournament.json"), tag);
    }

    public void onVictory(TetrisGame winner, TetrisBattle battle) {
        for (TetrisGame game : battle.getGames()) {
            if (winner == game) {
                addRank(game.getPlayer(), 1);
            } else {
                addRank(game.getPlayer(), -1);
            }
        }
    }

    private void addRank(TetrisPlayer player, int rank) {
        int value = tag.ranks.getOrDefault(player.uuid, 0);
        int newValue = Math.max(0, value + rank);
        tag.ranks.put(player.uuid, value);
        plugin.getLogger().info("Tournament: " + player.getName() + " " + value + " => " + newValue);
    }

    public void getSidebarLines(List<Component> l) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.removeIf(p -> tag.ranks.getOrDefault(p.getUniqueId(), 0) == 0);
        if (players.isEmpty()) return;
        l.add(text(Unicode.tiny("opponents"), GOLD, ITALIC));
        Collections.sort(players, (a, b) -> Integer.compare(tag.ranks.getOrDefault(b.getUniqueId(), 0),
                                                            tag.ranks.getOrDefault(a.getUniqueId(), 0)));
        for (int i = 0; i < 9; i += 1) {
            if (i >= players.size()) break;
            Player p = players.get(i);
            l.add(join(separator(space()),
                       Glyph.toComponent("" + i),
                       p.displayName()));
        }
    }

    private void tick() {
    }

    private void tryToBuildBattles() {
        Map<Integer, List<Player>> map = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TetrisPlayer session = plugin.sessions.of(player);
            if (session.getGame() != null) continue;
            int rank = tag.ranks.get(player.getUniqueId());
            map.computeIfAbsent(rank, i -> new ArrayList<>()).add(player);
        }
        // TODO
    }

    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
        protected boolean auto = false;
    }
}
