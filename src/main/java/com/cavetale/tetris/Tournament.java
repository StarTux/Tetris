package com.cavetale.tetris;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.item.trophy.TrophyCategory;
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
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Tournament {
    private final TetrisPlugin plugin;
    @Getter private Tag tag;
    private BukkitTask task;
    @Getter private boolean auto = false;
    private int autoCooldown = 0;
    private List<Highscore> highscore = List.of();
    private List<Component> highscoreLines = List.of();

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
        computeHighscore();
    }

    public void save() {
        if (tag == null) tag = new Tag();
        plugin.getDataFolder().mkdirs();
        Json.save(new File(plugin.getDataFolder(), "tournament.json"), tag, true);
    }

    public void onGameOver(TetrisGame game, TetrisBattle battle) {
        plugin.getLogger().info("Kicking " + game.getPlayer().getPlayer().getName() + " because they are afk");
        if (game.getLines() == 0) game.getPlayer().getPlayer().kick(text("afk"));
    }

    public void onVictory(TetrisGame winner, TetrisBattle battle) {
        addRank(winner.getPlayer().getUuid(), 1);
        save();
        computeHighscore();
    }

    public void addRank(UUID uuid, int rank) {
        int value = tag.ranks.getOrDefault(uuid, 0);
        int newValue = Math.max(0, value + rank);
        tag.ranks.put(uuid, newValue);
    }

    public int getRank(TetrisPlayer player) {
        return tag.ranks.getOrDefault(player.uuid, 0);
    }

    public Highscore getHighscore(UUID uuid) {
        for (Highscore it : highscore) {
            if (uuid.equals(it.uuid)) return it;
        }
        return new Highscore(uuid, 0);
    }

    public void computeHighscore() {
        this.highscore = Highscore.of(tag.ranks);
        this.highscoreLines = Highscore.sidebar(highscore, TrophyCategory.TETRIS);
    }

    public int reward() {
        int result = Highscore.reward(tag.ranks,
                                      "tetris_tournament",
                                      TrophyCategory.TETRIS,
                                      join(noSeparators(), plugin.tetrisTitle, text(" Tournament", GREEN)),
                                      hi -> "You earned " + hi.score + " point" + (hi.score != 1 ? "s" : ""));
        List<String> titles = List.of("Tetromino",
                                      "TetrisO",
                                      "TetrisL",
                                      "TetrisS",
                                      "TetrisT",
                                      "TetrisJ",
                                      "TetrisZ",
                                      "TetrisI");
        List<Highscore> list = Highscore.of(tag.ranks);
        for (int i = 0; i < list.size() && i < 3; i += 1) {
            String winnerName = PlayerCache.nameForUuid(list.get(i).uuid);
            String cmd = "titles unlockset " + winnerName + " " + String.join(" ", titles);
            plugin.getLogger().info("Running command: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            result += 1;
        }
        return result;
    }

    public void getSidebarLines(TetrisPlayer p, List<Component> l) {
        l.add(text(Unicode.tiny("tournament"), GOLD));
        if (p.getGame() == null) {
            l.add(text("Please wait until a", WHITE, ITALIC));
            l.add(text("game becomes available", WHITE, ITALIC));
        }
        Highscore playerRank = getHighscore(p.uuid);
        l.add(join(noSeparators(), text("Your score ", GRAY),
                   (playerRank.getPlacement() > 0
                    ? Glyph.toComponent("" + playerRank.getPlacement())
                    : Mytems.QUESTION_MARK),
                   text(Unicode.subscript("" + playerRank.score), GOLD)));
        l.addAll(highscoreLines);
    }

    private void tick() {
        if (auto) auto();
    }

    private void auto() {
        List<Player> players = getWaitingPlayers();
        if (players.size() < 2) {
            autoCooldown = 60;
        } else if (autoCooldown > 0) {
            autoCooldown -= 1;
        } else {
            tryToBuildBattles(players);
            autoCooldown = 60;
        }
    }

    private List<Player> getWaitingPlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TetrisPlayer session = plugin.sessions.of(player);
            if (session.getGame() == null) result.add(player);
        }
        return result;
    }

    private void tryToBuildBattles(List<Player> players) {
        Map<Integer, List<Player>> map = new HashMap<>();
        for (Player player : players) {
            int rank = tag.ranks.getOrDefault(player.getUniqueId(), 0);
            map.computeIfAbsent(rank, i -> new ArrayList<>()).add(player);
        }
        if (map.get(0) != null && map.get(0).size() == 1 && map.get(1) != null) {
            map.get(1).addAll(map.remove(0));
        }
        for (Map.Entry<Integer, List<Player>> entry : map.entrySet()) {
            int rank = entry.getKey();
            List<Player> list = entry.getValue();
            if (list.size() < 2) continue;
            Collections.shuffle(list, Rnd.get());
            while (list.size() >= 2) {
                TetrisBattle battle = new TetrisBattle();
                List<String> names = new ArrayList<>();
                int max = list.size() == 3 ? 3 : 2;
                for (int i = 0; i < max; i += 1) {
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
            }
        }
    }

    @Data
    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
    }

    public void setAuto(boolean value) {
        this.auto = value;
        this.autoCooldown = 0;
    }
}
