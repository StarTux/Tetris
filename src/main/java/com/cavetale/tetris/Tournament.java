package com.cavetale.tetris;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.SQLTrophy;
import com.cavetale.fam.trophy.Trophies;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.playercache.PlayerCache;
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
    private List<Rank> highscore = List.of();

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
        updateHighscoreList();
    }

    public void save() {
        if (tag == null) tag = new Tag();
        plugin.getDataFolder().mkdirs();
        Json.save(new File(plugin.getDataFolder(), "tournament.json"), tag, true);
    }

    public void onVictory(TetrisGame winner, TetrisBattle battle) {
        for (TetrisGame game : battle.getGames()) {
            if (winner == game) {
                addRank(game.getPlayer().uuid, 1);
            } else {
                addRank(game.getPlayer().uuid, 0);
            }
        }
        save();
        updateHighscoreList();
    }

    public void addRank(UUID uuid, int rank) {
        int value = tag.ranks.getOrDefault(uuid, 0);
        int newValue = Math.max(0, value + rank);
        tag.ranks.put(uuid, newValue);
    }

    public int getRank(TetrisPlayer player) {
        return tag.ranks.getOrDefault(player.uuid, 0);
    }

    private List<Rank> computeHighscoreList() {
        List<Rank> result = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : tag.ranks.entrySet()) {
            result.add(new Rank(entry.getValue(), entry.getKey()));
        }
        Collections.sort(result, (a, b) -> {
                int score = Integer.compare(b.score(), a.score());
                return score != 0
                    ? score
                    : String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name());
            });
        return result;
    }

    public void updateHighscoreList() {
        this.highscore = computeHighscoreList();
    }

    public void getSidebarLines(List<Component> l) {
        if (highscore.isEmpty()) return;
        l.add(text(Unicode.tiny("tournament"), GOLD, ITALIC));
        for (int i = 0; i < 10; i += 1) {
            if (i >= highscore.size()) break;
            Rank rank = highscore.get(i);
            l.add(join(noSeparators(),
                       Glyph.toComponent("" + (i + 1)),
                       text(Unicode.subscript("" + rank.score()), GOLD),
                       space(),
                       rank.displayName()));
        }
    }

    public int reward() {
        updateHighscoreList();
        if (highscore.isEmpty()) return 0;
        int placement = 0;
        int lastScore = -1;
        int count = 0;
        List<SQLTrophy> trophies = new ArrayList<>();
        for (Rank rank : highscore) {
            if (lastScore != rank.score()) {
                lastScore = rank.score();
                placement += 1;
            }
            if (placement <= 3) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + rank.name() + " Tetromino");
            }
            trophies.add(new SQLTrophy(rank.uuid(), "tetris_tournament",
                                       placement,
                                       TrophyCategory.TETRIS,
                                       join(noSeparators(), plugin.tetrisTitle, text(" Tournament", GREEN)),
                                       "You earned " + rank.score() + " point" + (rank.score() != 1 ? "s" : "")));
            count += 1;
        }
        Trophies.insertTrophies(trophies);
        return count;
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
            }
        }
    }

    @Data
    public static final class Tag {
        protected Map<UUID, Integer> ranks = new HashMap<>();
    }

    private static record Rank(int score, UUID uuid) {
        public String name() {
            return PlayerCache.nameForUuid(uuid());
        }

        public Component displayName() {
            Player player = Bukkit.getPlayer(uuid());
            return player != null
                ? player.displayName()
                : text(name());
        }
    }
}
