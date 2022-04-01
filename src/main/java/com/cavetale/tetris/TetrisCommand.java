package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class TetrisCommand extends AbstractCommand<TetrisPlugin> {
    protected TetrisCommand(final TetrisPlugin plugin) {
        super(plugin, "tetris");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .playerCaller(this::start);
    }

    public void start(Player player) {
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
}
