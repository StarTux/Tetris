package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.block.BlockFace;
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

    protected void start(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        if (session.getGame() != null) session.getGame().disable();
        TetrisPlace place = new TetrisPlace(player.getLocation().getBlock().getRelative(-5, -10, -20),
                                            BlockFace.EAST,
                                            BlockFace.SOUTH);
        TetrisGame game = new TetrisGame(session, place);
        game.initialize(player);
        session.setGame(game);
        player.sendMessage(text("Starting game!", GREEN));
    }
}
