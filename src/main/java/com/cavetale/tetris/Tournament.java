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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
    private static final double INIT_ELO = 100.0;

    private final TetrisPlugin plugin;
    @Getter @Setter private Tag tag;
    private BukkitTask task;
    @Getter @Setter private boolean auto = false;
    @Getter @Setter private boolean event = true;
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

    public void onVictory(TetrisGame winner, TetrisBattle battle) {
        addRank(winner.getPlayer().getUuid(), 1);
        addScore(winner.getPlayer().getUuid(), Math.min(10_000, winner.getScore()));
        // Elos
        final Map<UUID, Double> elos = new HashMap<>();
        for (TetrisGame game : battle.getGames()) {
            final UUID uuid = game.getPlayer().getUuid();
            elos.put(uuid, getElo(uuid));
        }
        for (UUID hero : elos.keySet()) {
            double opponentElo = 0.0;
            for (UUID opponent : elos.keySet()) {
                if (hero.equals(opponent)) continue;
                opponentElo += elos.get(opponent);
            }
            opponentElo /= (double) Math.max(1, elos.size() - 1);
            final double heroElo = elos.get(hero);
            final boolean win = winner.getPlayer().is(hero);
            final double outcome = win ? 1.0 : 0.0;
            final double newElo = computeRating(heroElo, opponentElo, outcome, 32.0);
            plugin.getLogger().info("[Elo]"
                                    + " " + (win ? "WIN" : "LOSE")
                                    + " " + PlayerCache.nameForUuid(hero)
                                    + " " + ((int) heroElo)
                                    + " vs " + ((int) opponentElo)
                                    + " => " + ((int) newElo));
            tag.elos.put(hero, newElo);
        }
        // Save
        save();
        computeHighscore();
    }

    private static double computeWinProbability(double rating, double opponent) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponent - rating) / 400.0));
    }

    private static double computeRating(double rating, double opponent, double outcome, double k) {
        final double expecation = computeWinProbability(rating, opponent);
        return rating + k * (outcome - expecation);
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

    public double getElo(Player player) {
        return getElo(player.getUniqueId());
    }

    public double getElo(UUID uuid) {
        return tag.elos.getOrDefault(uuid, INIT_ELO);
    }

    public Highscore getHighscore(UUID uuid) {
        for (Highscore it : highscore) {
            if (uuid.equals(it.uuid)) return it;
        }
        return new Highscore(uuid, 0);
    }

    public void computeHighscore() {
        final Map<UUID, Integer> elos = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : tag.elos.entrySet()) {
            elos.put(entry.getKey(), entry.getValue().intValue());
        }
        this.highscore = Highscore.of(elos);
        this.highscoreLines = Highscore.sidebar(highscore, TrophyCategory.TETRIS);
    }

    public int reward() {
        int result = Highscore.reward(tag.scores,
                                      "tetris_tournament",
                                      TrophyCategory.TETRIS,
                                      textOfChildren(plugin.tetrisTitle, text(" Tournament", GREEN)),
                                      hi -> "You earned a rating of " + hi.score);
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
        l.add(textOfChildren(text(tiny("round "), GRAY),
                             text(tag.round, GOLD)));
        if (plugin.sessions.of(p.getPlayer()).getGame() == null) {
            if (tag.alreadyPlayed.contains(p.uuid)) {
                l.add(textOfChildren(text("Please wait for", GRAY)));
                l.add(textOfChildren(text("the next round.", GRAY)));
            }
            Highscore playerRank = getHighscore(p.uuid);
            l.add(textOfChildren(text("Your rating ", GRAY),
                                 (playerRank.getPlacement() > 0
                                  ? Glyph.toComponent("" + playerRank.getPlacement())
                                  : Mytems.QUESTION_MARK),
                                 text(subscript("" + playerRank.score), GOLD)));
            l.addAll(highscoreLines);
        }
    }

    private void tickOncePerSecond() {
        if (!auto) return;
        if (tag.round > 0 && tag.warmupTime < 5) {
            tag.warmupTime += 1;
            plugin.getLogger().info("[Tournament] Warmup: " + tag.warmupTime);
            return;
        }
        if (tag.round == 0 || (!tag.alreadyPlayed.isEmpty() && plugin.getGames().isEmpty())) {
            tag.startNewRound();
            save();
            plugin.getLogger().info("[Tournament] New round: " + tag.round);
            return;
        }
        final List<Player> players = getWaitingPlayers();
        Collections.shuffle(players);
        Collections.sort(players, Comparator.comparing(this::getElo));
        if (players.size() < 2) {
            return;
        } else if (players.size() <= 3) {
            buildBattle(players);
        } else {
            buildBattle(players.subList(0, 2));
        }
    }

    private List<Player> getWaitingPlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (tag.alreadyPlayed.contains(player.getUniqueId())) {
                continue;
            }
            if (plugin.sessions.of(player).getGame() != null) {
                continue;
            }
            result.add(player);
        }
        return result;
    }

    private void buildBattle(List<Player> matchList) {
        TetrisBattle battle = new TetrisBattle();
        List<String> names = new ArrayList<>();
        for (Player player : matchList) {
            tag.alreadyPlayed.add(player.getUniqueId());
            TetrisGame game = plugin.startGame(player);
            battle.getGames().add(game);
            game.setBattle(battle);
            names.add(player.getName());
        }
        for (TetrisGame game : battle.getGames()) {
            game.getPlayer().getPlayer().sendMessage(text("Starting game with " + String.join(", ", names), GREEN));
            game.getPlayer().getPlayer().sendMessage(text("Good luck and have fun!", GREEN));
        }
        plugin.getLogger().info("[Tournament] Starting battle" + " [ " + String.join(", ", names) + " ]");
        if (event) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
        }
    }

    @Data
    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
        protected Map<UUID, Integer> lines = new HashMap<>();
        protected Map<UUID, Integer> scores = new HashMap<>();
        protected Map<UUID, Double> elos = new HashMap<>();
        protected int round = 0;
        protected Set<UUID> alreadyPlayed = new HashSet<>();
        protected int warmupTime;

        protected void startNewRound() {
            round += 1;
            alreadyPlayed.clear();
            warmupTime = 0;
        }
    }
}
