package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.Unicode;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.tetris.sql.SQLScore;
import com.winthier.playercache.PlayerCache;
import java.text.SimpleDateFormat;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
        plugin.database.scheduleAsyncTask(() -> {
                List<SQLScore> list = plugin.database.find(SQLScore.class)
                    .orderByDescending("score")
                    .limit(10)
                    .findList();
                SQLScore my = sender instanceof Player player
                    ? (plugin.database.find(SQLScore.class)
                       .eq("player", player.getUniqueId())
                       .orderByDescending("score")
                       .limit(1)
                       .findUnique())
                    : null;
                Bukkit.getScheduler().runTask(plugin, () -> highscoreCallback(sender, list, my));
            });
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd yyyy");

    private static Component rankComponent(SQLScore row) {
        return join(separator(space()),
                    join(noSeparators(),
                         text(Unicode.tiny("score"), GRAY),
                         text("" + row.getScore(), WHITE)),
                    join(noSeparators(),
                         text(Unicode.tiny("lvl"), GRAY),
                         text("" + row.getLevel())),
                    text(PlayerCache.nameForUuid(row.getPlayer()), WHITE),
                    text(DATE_FORMAT.format(row.getTime()), GRAY, ITALIC));
    }

    private void highscoreCallback(CommandSender sender, List<SQLScore> list, SQLScore my) {
        if (list.isEmpty()) {
            sender.sendMessage(text("No highscores to show!", RED));
            return;
        }
        sender.sendMessage(join(separator(space()), plugin.tetrisTitle, text("Highscore", GOLD)));
        if (my != null && !list.contains(my)) {
            sender.sendMessage(join(separator(space()),
                                    text("Your top score", GRAY),
                                    rankComponent(my)));
        }
        for (int i = 0; i < 10; i += 1) {
            if (i >= list.size()) break;
            SQLScore row = list.get(i);
            int rank = i + 1;
            sender.sendMessage(join(separator(space()),
                                    Glyph.toComponent("" + (i + 1)),
                                    rankComponent(row)));
        }
    }
}
