package com.cavetale.tetris;

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
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Tournament {
    private static final int MIN_WAIT_TIME = 20;

    private final TetrisPlugin plugin;
    @Getter private Tag tag;
    private BukkitTask task;
    @Getter private boolean auto = false;
    private List<Highscore> highscore = List.of();
    private List<Component> highscoreLines = List.of();

    public void enable() {
        load();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickOncePerSecond, 20L, 20L);
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
        addScore(winner.getPlayer().getUuid(), Math.min(10_000, winner.getScore()));
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

    public int getRank(Player player) {
        return tag.ranks.getOrDefault(player.getUniqueId(), 0);
    }

    public void addLines(UUID uuid, int lines) {
        final int value = tag.lines.getOrDefault(uuid, 0);
        final int newValue = Math.max(0, value + lines);
        tag.lines.put(uuid, newValue);
    }

    public int getLines(TetrisPlayer player) {
        return tag.lines.getOrDefault(player.uuid, 0);
    }

    public int getLines(Player player) {
        return tag.lines.getOrDefault(player.getUniqueId(), 0);
    }

    public void addScore(UUID uuid, int score) {
        int value = tag.scores.getOrDefault(uuid, 0);
        int newValue = Math.max(0, value + score);
        tag.scores.put(uuid, newValue);
    }

    public int getScore(TetrisPlayer player) {
        return tag.scores.getOrDefault(player.uuid, 0);
    }

    public int getScore(Player player) {
        return tag.scores.getOrDefault(player.getUniqueId(), 0);
    }

    public Highscore getHighscore(UUID uuid) {
        for (Highscore it : highscore) {
            if (uuid.equals(it.uuid)) return it;
        }
        return new Highscore(uuid, 0);
    }

    public void computeHighscore() {
        this.highscore = Highscore.of(tag.scores);
        this.highscoreLines = Highscore.sidebar(highscore, TrophyCategory.TETRIS);
    }

    public int reward() {
        int result = Highscore.reward(tag.scores,
                                      "tetris_tournament",
                                      TrophyCategory.TETRIS,
                                      textOfChildren(plugin.tetrisTitle, text(" Tournament", GREEN)),
                                      hi -> "You earned " + hi.score + " point" + (hi.score != 1 ? "s" : ""));
        List<String> titles = List.of("Tetromino",
                                      "TetrisO",
                                      "TetrisL",
                                      "TetrisS",
                                      "TetrisT",
                                      "TetrisJ",
                                      "TetrisZ",
                                      "TetrisI");
        List<Highscore> list = Highscore.of(tag.scores);
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
        l.add(text(tiny("tournament"), GOLD));
        if (auto && p.getGame() == null) {
            final int waitTime = tag.waitTimes.getOrDefault(p.getPlayer().getUniqueId(), 0);
            final int scoreTolerance = calculateScoreTolerance(waitTime);
            l.add(text("Please wait until a", WHITE, ITALIC));
            l.add(text("game becomes available", WHITE, ITALIC));
            l.add(textOfChildren(text(tiny("waiting for "), GRAY),
                                 text(waitTime),
                                 text(tiny("s"), GRAY)));
            l.add(textOfChildren(text(tiny("players within "), GRAY),
                                 text(scoreTolerance),
                                 text(tiny("p"), GRAY)));
        }
        Highscore playerRank = getHighscore(p.uuid);
        l.add(textOfChildren(text("Your score ", GRAY),
                             (playerRank.getPlacement() > 0
                              ? Glyph.toComponent("" + playerRank.getPlacement())
                              : Mytems.QUESTION_MARK),
                             text(subscript("" + playerRank.score), GOLD)));
        l.addAll(highscoreLines);
    }

    private void tickOncePerSecond() {
        if (!auto) return;
        List<Player> players = getWaitingPlayers();
        if (players.size() < 2) {
            return;
        }
        for (Player player : players) {
            final UUID uuid = player.getUniqueId();
            tag.waitTimes.put(uuid, tag.waitTimes.getOrDefault(uuid, 0) + 1);
        }
        players.removeIf(p -> tag.waitTimes.getOrDefault(p.getUniqueId(), 0) < MIN_WAIT_TIME);
        while (players.size() >= 2) {
            if (0 == tryToBuildBattles(players)) break;
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

    /**
     * Try to build a battle from the list of available players and
     * remove them from the list.
     * @return the number of battles built
     */
    private int tryToBuildBattles(List<Player> players) {
        Collections.shuffle(players, Rnd.get());
        int result = 0;
        for (Player player : List.copyOf(players)) {
            if (players.size() < 2) break;
            if (!players.contains(player)) continue;
            final int playerScore = tag.scores.getOrDefault(player.getUniqueId(), 0);
            final int waitTime = tag.waitTimes.getOrDefault(player.getUniqueId(), 0);
            final int scoreTolerance = calculateScoreTolerance(waitTime);
            final List<Player> matchList = new ArrayList<>();
            matchList.add(player);
            for (Player opponent : players) {
                if (opponent == player) continue;
                final int opponentScore = tag.scores.getOrDefault(opponent.getUniqueId(), 0);
                final int opponentWaitTime = tag.waitTimes.getOrDefault(opponent.getUniqueId(), 0);
                final int opponentScoreTolerance = calculateScoreTolerance(opponentWaitTime);
                if (Math.abs(opponentScore - playerScore) > Math.min(scoreTolerance, opponentScoreTolerance)) continue;
                matchList.add(opponent);
                if (matchList.size() >= 3 && players.size() > 4) break;
            }
            if (matchList.size() < 2) continue;
            players.removeAll(matchList);
            buildBattle(playerScore, scoreTolerance, waitTime, matchList);
            result += 1;
        }
        return result;
    }

    private static int calculateScoreTolerance(int waitTime) {
        return Math.max(0, (waitTime - MIN_WAIT_TIME) * 1000); // 400 points per second good?
    }

    private void buildBattle(int playerScore, int scoreTolerance, int waitTime, List<Player> matchList) {
        TetrisBattle battle = new TetrisBattle();
        List<String> names = new ArrayList<>();
        for (Player player : matchList) {
            TetrisGame game = plugin.startGame(player);
            battle.getGames().add(game);
            game.setBattle(battle);
            names.add(player.getName());
            tag.waitTimes.remove(player.getUniqueId());
        }
        for (TetrisGame game : battle.getGames()) {
            game.getPlayer().getPlayer().sendMessage(text("Starting game with " + String.join(", ", names), GREEN));
            game.getPlayer().getPlayer().sendMessage(text("Good luck and have fun!", GREEN));
        }
        plugin.getLogger().info("[Tournament] Starting battle"
                                + " score:" + playerScore
                                + " tolerance:" + scoreTolerance
                                + " wait:" + waitTime
                                + " [ " + String.join(", ", names) + " ]");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
    }

    @Data
    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
        protected Map<UUID, Integer> lines = new HashMap<>();
        protected Map<UUID, Integer> scores = new HashMap<>();
        protected Map<UUID, Integer> waitTimes = new HashMap<>();
    }

    public void setAuto(boolean value) {
        this.auto = value;
    }
}
