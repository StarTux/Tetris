package com.cavetale.tetris;

import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandNode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdminCommand extends AbstractCommand<TetrisPlugin> {
    protected AdminCommand(final TetrisPlugin plugin) {
        super(plugin, "tetrisadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .playerCaller(this::start);
        rootNode.addChild("shift").denyTabCompletion()
            .description("Shift up")
            .playerCaller(this::shift);
        CommandNode tournamentNode = rootNode.addChild("tournament")
            .description("Tournament commands");
        tournamentNode.addChild("enable");
        tournamentNode.addChild("disable");
        tournamentNode.addChild("auto");
    }

    private void start(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        if (session.getGame() != null) session.getGame().disable();
        Block block = player.getLocation().getBlock();
        TetrisPlace place = new TetrisPlace(block, BlockFace.EAST, BlockFace.SOUTH);
        Vec3i home = Vec3i.of(block).add(5, 10, 20);
        TetrisGame game = new TetrisGame(session, place, home);
        game.initialize(player);
        session.setGame(game);
        player.sendMessage(text("Starting game!", GREEN));
    }

    private void shift(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        session.getGame().shiftUp();
        player.sendMessage(text("Shifted up", YELLOW));
    }
}
