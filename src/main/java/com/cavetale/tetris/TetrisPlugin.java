package com.cavetale.tetris;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.tetris.sql.SQLScore;
import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class TetrisPlugin extends JavaPlugin {
    @Getter protected static TetrisPlugin instance;
    @Getter private TetrisCommand tetrisCommand = new TetrisCommand(this);
    private AdminCommand adminCommand = new AdminCommand(this);
    private EventListener eventListener = new EventListener(this);
    protected Sessions sessions = new Sessions(this);
    @Getter @Setter private Tournament tournament = null;
    @Getter protected final Allocator allocator = new Allocator(this);
    protected List<TetrisGame> games = new ArrayList<>();
    @Getter protected TetrisMatch match = new TetrisMatch(this);
    protected SQLDatabase database = new SQLDatabase(this);
    public final Component tetrisTitle = join(noSeparators(), new Component[] {
            text("T", GOLD),
            text("E", BLUE),
            text("T", GOLD),
            text("R", RED),
            text("I", YELLOW),
            text("S", GREEN)
        }).decorate(BOLD);

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        database.registerTables(List.of(SQLScore.class));
        database.createAllTables();
        tetrisCommand.enable();
        adminCommand.enable();
        eventListener.enable();
        sessions.enable();
        Bukkit.getScheduler().runTask(this, () -> allocator.enable(Bukkit.getWorld("tetris"), 24));
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
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

    public void onGameOver(TetrisGame game, TetrisBattle battle) {
        if (tournament != null) tournament.onGameOver(game, battle);
    }

    public TetrisGame startGame(Player player) {
        TetrisPlayer session = sessions.of(player);
        if (session.getGame() != null) {
            session.getGame().disable();
            session.setGame(null);
        }
        WorldSlice slice = allocator.dealSlice();
        if (slice == null) return null;
        int y = (int) allocator.getWorld().getSpawnLocation().getY();
        Block block = allocator.getWorld().getBlockAt(slice.x, y, slice.z);
        do {
            boolean allEmpty = true;
            LOOP: for (int x = 0; x < 12; x += 1) {
                for (int z = 0; z < 12; z += 1) {
                    if (!block.getRelative(x, 0, z).isEmpty()) {
                        allEmpty = false;
                        break LOOP;
                    }
                }
            }
            if (allEmpty) break;
            block = block.getRelative(0, 1, 0);
        } while (block.getY() < allocator.getWorld().getMaxHeight());
        block = block.getRelative(0, 8, 0);
        TetrisPlace place = new TetrisPlace(block, BlockFace.EAST, BlockFace.SOUTH);
        Vec3i home = Vec3i.of(block).add(5, 7, 20);
        TetrisGame game = new TetrisGame(session, place, home);
        game.setSlice(slice);
        session.setGame(game);
        games.add(game);
        slice.setGame(game);
        game.initialize(player);
        return game;
    }

    public TetrisGame gameOf(Player player) {
        for (TetrisGame game : games) {
            if (game.getPlayer().is(player)) return game;
        }
        return null;
    }

    private void tick() {
        if (match.isEnabled()) match.tick();
    }
}
