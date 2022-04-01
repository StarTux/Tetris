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
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Tournament {
    private final TetrisPlugin plugin;
    @Getter private Tag tag;
    private BukkitTask task;
    @Getter @Setter private boolean auto = false;

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
        Json.save(new File(plugin.getDataFolder(), "tournament.json"), tag, true);
    }

    public void onVictory(TetrisGame winner, TetrisBattle battle) {
        for (TetrisGame game : battle.getGames()) {
            if (winner == game) {
                addRank(game.getPlayer(), 1);
            } else {
                addRank(game.getPlayer(), -1);
            }
        }
        save();
    }

    public void addRank(TetrisPlayer player, int rank) {
        int value = tag.ranks.getOrDefault(player.uuid, 0);
        int newValue = Math.max(0, value + rank);
        tag.ranks.put(player.uuid, newValue);
        plugin.getLogger().info("Tournament: " + player.getName() + " " + value + " => " + newValue);
    }

    public int getRank(TetrisPlayer player) {
        return tag.ranks.getOrDefault(player.uuid, 0);
    }

    public void getSidebarLines(List<Component> l) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.removeIf(p -> tag.ranks.getOrDefault(p.getUniqueId(), 0) == 0);
        if (players.isEmpty()) return;
        l.add(text(Unicode.tiny("opponents"), GOLD, ITALIC));
        Collections.sort(players, (a, b) -> {
                int val = Integer.compare(tag.ranks.getOrDefault(b.getUniqueId(), 0),
                                          tag.ranks.getOrDefault(a.getUniqueId(), 0));
                if (val != 0) return val;
                return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
            });
        for (int i = 0; i < 9; i += 1) {
            if (i >= players.size()) break;
            Player p = players.get(i);
            l.add(join(noSeparators(),
                       Glyph.toComponent("" + (i + 1)),
                       text(Unicode.subscript("" + tag.ranks.get(p.getUniqueId())), GOLD),
                       space(),
                       p.displayName()));
        }
    }

    private void tick() {
        if (auto) tryToBuildBattles();
    }

    private void tryToBuildBattles() {
        Map<Integer, List<Player>> map = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TetrisPlayer session = plugin.sessions.of(player);
            if (session.getGame() != null) continue;
            int rank = tag.ranks.getOrDefault(player.getUniqueId(), 0);
            map.computeIfAbsent(rank, i -> new ArrayList<>()).add(player);
        }
        for (Map.Entry<Integer, List<Player>> entry : map.entrySet()) {
            int rank = entry.getKey();
            List<Player> list = entry.getValue();
            if (list.size() < 2) continue;
            Collections.shuffle(list, Rnd.get());
            while (list.size() >= 2) {
                TetrisBattle battle = new TetrisBattle();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < 4; i += 1) {
                    Player player = list.remove(list.size() - 1);
                    TetrisGame game = plugin.startGame(player);
                    battle.getGames().add(game);
                    game.setBattle(battle);
                    names.add(player.getName());
                    if (list.isEmpty()) break;
                }
                for (TetrisGame game : battle.getGames()) {
                    game.getPlayer().getPlayer().sendMessage(text("Starting game with " + String.join(", ", names), GREEN));
                }
                plugin.getLogger().info("[Tournament] Starting battle (" + rank + ")" + String.join(", ", names));
            }
        }
    }

    @Data
    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
    }
}
