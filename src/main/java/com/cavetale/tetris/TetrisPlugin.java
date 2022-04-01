package com.cavetale.tetris;

import com.cavetale.area.struct.Vec3i;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TetrisPlugin extends JavaPlugin {
    @Getter protected static TetrisPlugin instance;
    private TetrisCommand tetrisCommand = new TetrisCommand(this);
    private AdminCommand adminCommand = new AdminCommand(this);
    private EventListener eventListener = new EventListener(this);
    protected Sessions sessions = new Sessions(this);
    @Getter @Setter private Tournament tournament = null;
    @Getter protected final Allocator allocator = new Allocator(this);
    protected List<TetrisGame> games = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        tetrisCommand.enable();
        adminCommand.enable();
        eventListener.enable();
        sessions.enable();
        Bukkit.getScheduler().runTask(this, () -> allocator.enable(Bukkit.getWorlds().get(0), 24));
    }

    @Override
    public void onDisable() {
        sessions.disable();
        for (TetrisGame game : new ArrayList<>(games)) game.disable();
        games.clear();
    }

    public void onVictory(TetrisGame game, TetrisBattle battle) {
        if (tournament != null) tournament.onVictory(game, battle);
    }

    public TetrisGame startGame(Player player) {
        TetrisPlayer session = sessions.of(player);
        if (session.getGame() != null) {
            session.getGame().disable();
            session.setGame(null);
        }
        WorldSlice slice = allocator.dealSlice();
        if (slice == null) return null;
        Block block = allocator.getWorld().getHighestBlockAt(slice.x, slice.z);
        while (!block.isEmpty()) block = block.getRelative(0, 1, 0);
        block = block.getRelative(0, 16, 0);
        TetrisPlace place = new TetrisPlace(block, BlockFace.EAST, BlockFace.SOUTH);
        Vec3i home = Vec3i.of(block).add(5, 10, 20);
        TetrisGame game = new TetrisGame(session, place, home);
        game.setSlice(slice);
        session.setGame(game);
        games.add(game);
        slice.setGame(game);
        game.initialize(player);
        return game;
    }
}
