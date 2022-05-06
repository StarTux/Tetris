package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.Unicode;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.tetris.sql.SQLScore;
import com.winthier.playercache.PlayerCache;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class TetrisCommand extends AbstractCommand<TetrisPlugin> {
    private final List<Highscore> highscore = new ArrayList<>();
    private final List<Highscore> ranks = new ArrayList<>();
    private final Map<UUID, Highscore> playerRanks = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd yyyy");

    protected TetrisCommand(final TetrisPlugin plugin) {
        super(plugin, "tetris");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .playerCaller(this::start);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop your game")
            .playerCaller(this::stop);
        rootNode.addChild("hi").denyTabCompletion()
            .description("Highscore")
            .senderCaller(this::highscore);
        rootNode.addChild("rank").denyTabCompletion()
            .description("Player rankings")
            .senderCaller(this::rank);
        rebuildHighscores();
    }

    protected void rebuildHighscores() {
        plugin.database.find(SQLScore.class)
            .orderByDescending("score")
            .findListAsync(list -> {
                    highscore.clear();
                    ranks.clear();
                    playerRanks.clear();
                    int hiPlacement = 0;
                    int hiLastScore = -1;
                    int rankPlacement = 0;
                    int rankLastScore = -1;
                    for (SQLScore row : list) {
                        if (highscore.size() < 10) {
                            if (hiLastScore != row.getScore()) {
                                hiLastScore = row.getScore();
                                hiPlacement += 1;
                            }
                            highscore.add(new Highscore(hiPlacement, row));
                        }
                        if (!playerRanks.containsKey(row.getPlayer())) {
                            if (rankLastScore != row.getScore()) {
                                rankLastScore = row.getScore();
                                rankPlacement += 1;
                            }
                            Highscore hi = new Highscore(rankPlacement, row);
                            if (ranks.size() < 10) {
                                ranks.add(hi);
                            }
                            playerRanks.put(row.getPlayer(), hi);
                        }
                    }
                });
    }

    private void start(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        if (session.getGame() != null) {
            throw new CommandWarn("You're already playing!");
        }
        if (plugin.getTournament() != null) {
            throw new CommandWarn("There is a tournament underway!");
        }
        TetrisGame game = plugin.startGame(player);
        if (game == null) {
            throw new CommandWarn("Cannot start game! Please try again later!");
        }
        player.sendMessage(text("Starting a game!", GREEN));
    }

    private void stop(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        if (session.getGame() == null || !session.getGame().isActive()) {
            throw new CommandWarn("You're not playing!");
        }
        session.getGame().disable();
        player.sendMessage(text("Stopping game!", RED));
    }

    private void highscore(CommandSender sender) {
        if (highscore.isEmpty()) {
            sender.sendMessage(text("No highscores to show!", RED));
            return;
        }
        sender.sendMessage(join(separator(space()), plugin.tetrisTitle, text("Highscore", GOLD)));
        for (Highscore hi : highscore) {
            sender.sendMessage(rankComponent(hi));
        }
    }

    private void rank(CommandSender sender) {
        if (ranks.isEmpty()) {
            sender.sendMessage(text("No rankings to show!", RED));
            return;
        }
        sender.sendMessage(join(separator(space()), plugin.tetrisTitle, text("Player Ranking", GOLD)));
        if (sender instanceof Player player) {
            Highscore playerRank = playerRanks.get(player.getUniqueId());
            if (playerRank != null) {
                player.sendMessage(join(noSeparators(),
                                        text("Personal best: ", GRAY),
                                        rankComponent(playerRank)));
            }
        }
        for (Highscore hi : ranks) {
            sender.sendMessage(rankComponent(hi));
        }
    }

    private static Component rankComponent(Highscore hi) {
        return join(separator(space()),
                    Glyph.toComponent("" + hi.placement),
                    join(noSeparators(),
                         text(Unicode.tiny("score"), GRAY),
                         text("" + hi.row.getScore(), GOLD)),
                    join(noSeparators(),
                         text(Unicode.tiny("lvl"), GRAY),
                         text("" + hi.row.getLevel(), GOLD)),
                    text(hi.name(), WHITE),
                    text(DATE_FORMAT.format(hi.row.getTime()), GRAY, ITALIC));
    }

    @RequiredArgsConstructor
    private static class Highscore {
        protected final int placement;
        protected final SQLScore row;

        public String name() {
            return PlayerCache.nameForUuid(row.getPlayer());
        }
    }
}
