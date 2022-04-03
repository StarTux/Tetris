package com.cavetale.tetris;

import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.mytems.util.BlockColor;
import com.cavetale.mytems.util.Items;
import com.cavetale.tetris.sql.SQLScore;
import com.cavetale.worldmarker.entity.EntityMarker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.title;

@Getter @RequiredArgsConstructor
public final class TetrisGame {
    private final TetrisPlayer player;
    private final TetrisPlace place;
    private final Vec3i home;
    private GameState state = GameState.INIT;
    private TetrisBoard board;
    private TetrisBlock block;
    private Tetromino nextBlock;
    private BukkitTask task;
    private int fallingTicks;
    private int stateTicks;
    private int score;
    private int battleScore;
    private int lines;
    private int level;
    private List<Integer> fullRows = List.of();
    private int clearTicks;
    @Setter private TetrisBattle battle;
    @Setter private WorldSlice slice;

    public void initialize(Player p) {
        board = new TetrisBoard(10, 20);
        makeNewBlock();
        state = GameState.FALL;
        task = Bukkit.getScheduler().runTaskTimer(TetrisPlugin.instance, this::tick, 0L, 1L);
        fallingTicks = 10;
        drawFrame();
        drawBoard();
        drawBlock(true);
        teleportHome(p);
        p.sendExperienceChange((float) (lines % 10) / 10.0f, level);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.5f, 2.0f);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (slice != null && slice.getGame() == this) {
            slice.setGame(null);
            slice = null;
        }
        if (player.getGame() == this) player.setGame(null);
        clearFrame();
        TetrisPlugin.instance.games.remove(this);
        state = GameState.DISABLE;
        Player p = player.getPlayer();
        if (p != null) p.getInventory().clear();
    }

    private void clearFrame() {
        for (int y = -1; y <= board.height; y += 1) {
            for (int x = -1; x <= board.width; x += 1) {
                for (int z = -1; z <= 1; z += 1) {
                    place.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void drawFrame() {
        for (int y = -1; y <= board.height; y += 1) {
            for (int x = -1; x <= board.width; x += 1) {
                if ((x == -1 || x == board.width) || (y == -1 || y == board.height)) {
                    Material frameMaterial = Material.DEEPSLATE_BRICKS;
                    place.getBlockAt(x, y, -1).setType(frameMaterial, false);
                    place.getBlockAt(x, y, 0).setType(frameMaterial, false);
                    place.getBlockAt(x, y, 1).setType(frameMaterial, false);
                } else {
                    place.getBlockAt(x, y, -1).setType(Material.WHITE_STAINED_GLASS, false);
                    place.getBlockAt(x, y, 1).setType(Material.BARRIER, false);
                }
            }
        }
    }

    private void drawBoard() {
        board.forEachCell((x, y, color) -> {
                Material material = color == null
                    ? Material.AIR
                    : color.getMaterial(BlockColor.Suffix.CONCRETE);
                place.getBlockAt(x, y, 0).setType(material, false);
            });
    }

    private void drawBlock(boolean visible) {
        block.getBoard().forEachCell((dx, dy, color) -> {
                if (color == null) return;
                final int x = dx + block.getX();
                final int y = dy + block.getY();
                if (x < 0 || x >= board.width) return;
                if (y < 0 || y >= board.height) return;
                Material material = visible
                    ? color.getMaterial(BlockColor.Suffix.CONCRETE_POWDER)
                    : Material.AIR;
                place.getBlockAt(x, y, 0).setType(material, false);
            });
    }

    public boolean doesBlockFitAt(final int offx, final int offy) {
        boolean[] result = new boolean[1];
        result[0] = true;
        block.getBoard().forEachCell((dx, dy, color) -> {
                if (color == null) return;
                final int x = offx + dx;
                if (x < 0 || x >= board.width) {
                    result[0] = false;
                    return;
                }
                final int y = offy + dy;
                if (y < 0) { // y is allowed to be above the board!
                    result[0] = false;
                    return;
                }
                if (board.get(x, y) != 0) {
                    result[0] = false;
                    return;
                }
            });
        return result[0];
    }

    public boolean doesBlockFitWith(final int rotation) {
        boolean[] result = new boolean[1];
        result[0] = true;
        block.getBoard(rotation).forEachCell((dx, dy, color) -> {
                if (color == null) return;
                final int x = block.getX() + dx;
                if (x < 0 || x >= board.width) {
                    result[0] = false;
                    return;
                }
                final int y = block.getY() + dy;
                if (y < 0) { // y is allowed to be above the board!
                    result[0] = false;
                    return;
                }
                if (board.get(x, y) != 0) {
                    result[0] = false;
                    return;
                }
            });
        return result[0];
    }

    private void makeNewBlock() {
        block = new TetrisBlock(nextBlock != null ? nextBlock : Rnd.tetromino(), Rnd.tetrisBlockColor());
        block.setX(board.width / 2 - 2);
        block.setY(board.height);
        nextBlock = Rnd.tetromino();
    }

    private void tick() {
        if (battle != null && battle.findWinner() == this) {
            player.getPlayer().showTitle(title(text("VICTOR", GREEN, BOLD),
                                               text(Unicode.tiny("final score ") + score, GRAY)));
            TetrisPlugin.instance.onVictory(this, battle);
            battle.disable();
            disable();
            return;
        }
        GameState newState;
        try {
            newState = tickState(state);
        } catch (IllegalStateException ise) {
            disable();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            disable();
            return;
        }
        stateTicks += 1;
        if (newState == null) return;
        switch (newState) {
        case LAND: {
            boolean[] lost = new boolean[1];
            block.getBoard().forEachCell((x, y, color) -> {
                    if (y + block.getY() > board.height) {
                        lost[0] = true;
                    }
                });
            if (lost[0]) {
                state = GameState.LOSE;
                player.getPlayer().showTitle(title(text("GAME OVER", RED),
                                                   text(Unicode.tiny("final score ") + score, GRAY)));
                if (score > 0) {
                    TetrisPlugin.instance.database.insertAsync(new SQLScore(this), null);
                }
                stateTicks = 0;
            } else {
                board.paste(block.getBoard(), block.getX(), block.getY());
                fullRows = board.getFullRows();
                if (fullRows.isEmpty()) {
                    state = GameState.FALL;
                    Player p = player.getPlayer();
                    p.playSound(p.getLocation(), Sound.BLOCK_STONE_FALL, SoundCategory.BLOCKS, 1.0f, 0.5f);
                } else {
                    state = GameState.CLEAR;
                    int newLines = fullRows.size();
                    clearTicks = 10 * newLines;
                    lines += newLines;
                    int scoreBonus = scoreBonus(newLines) * (level + 1);
                    score += scoreBonus;
                    battleScore += scoreBonus;
                    if (battle != null) {
                        for (TetrisGame other : battle.getGames()) {
                            if (other == this) continue;
                            other.battleScore -= scoreBonus;
                        }
                    }
                    int newLevel = lines / 10;
                    Player p = player.getPlayer();
                    if (newLevel > level) {
                        level = newLevel;
                        p.showTitle(title(text("" + level, GREEN),
                                                           text(Unicode.tiny("levelup"), GREEN)));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
                    }
                    p.sendExperienceChange((float) (lines % 10) / 10.0f, level);
                    for (int y : fullRows) {
                        board.clearRow(y);
                        for (int x = 0; x < board.width; x += 1) {
                            Location location = place.getBlockAt(x, y, 0).getLocation().add(0.5, 0.0, 0.5);
                            location.getWorld().spawn(location, TNTPrimed.class, tnt -> {
                                    tnt.setPersistent(false);
                                    tnt.setFuseTicks(clearTicks);
                                    tnt.setSilent(true);
                                    EntityMarker.setId(tnt, "tetris");
                                });
                        }
                    }
                }
                drawBoard();
                makeNewBlock();
                drawBlock(true);
                stateTicks = 0;
            }
            break;
        }
        case DISABLE: {
            disable();
            break;
        }
        case FALL:
            state = GameState.FALL;
            stateTicks = 0;
            break;
        default: throw new IllegalStateException("newState=" + newState);
        }
    }

    private int scoreBonus(int newLines) {
        switch (newLines) {
        case 1: return 40;
        case 2: return 100;
        case 3: return 300;
        case 4: return 1200;
        default: return 0;
        }
    }

    private GameState tickState(GameState currentState) {
        switch (currentState) {
        case FALL: return tickFall();
        case LOSE: return tickLose();
        case CLEAR: return tickClear();
        default: throw new IllegalStateException("state=" + currentState);
        }
    }

    private GameState tickFall() {
        if (battleScore < (-100 * (level + 1))) {
            battleScore = 0;
            shiftUp();
        }
        fallingTicks -= 1;
        if (fallingTicks > 0) return null;
        if (!doesBlockFitAt(block.getX(), block.getY() - 1)) {
            return GameState.LAND;
        }
        drawBlock(false);
        block.setY(block.getY() - 1);
        drawBlock(true);
        fallingTicks = 10 - level;
        return null;
    }

    private GameState tickClear() {
        clearTicks -= 1;
        if (clearTicks > 0) return null;
        for (int i = fullRows.size() - 1; i >= 0; i -= 1) {
            board.dropRow(fullRows.get(i));
        }
        drawBoard();
        fullRows = List.of();
        Player p = player.getPlayer();
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 1.25f);
        return GameState.FALL;
    }

    private GameState tickLose() {
        final int interval = 5;
        if (stateTicks % interval != 0) return null;
        int y = stateTicks / interval;
        if (y >= board.height) {
            return GameState.DISABLE;
        }
        for (int x = 0; x < board.width; x += 1) {
            place.getBlockAt(x, y, 0).setType(Material.REDSTONE_BLOCK);
        }
        return null;
    }

    private void bit(Player p, float pitch) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.BLOCKS, 0.2f, pitch);
    }

    public void playerInput(Player p, Hotbar hotbar) {
        if (state != GameState.FALL) {
            bit(p, 0.5f);
            return;
        }
        switch (hotbar) {
        case LEFT:
            move(p, -1);
            break;
        case RIGHT:
            move(p, 1);
            break;
        case DOWN:
            if (doesBlockFitAt(block.getX(), block.getY() - 1)) {
                drawBlock(false);
                block.setY(block.getY() - 1);
                drawBlock(true);
                p.sendActionBar(text("Down", GREEN));
                bit(p, 1.0f);
            }
            break;
        case TURN_LEFT:
            turn(p, -1);
            break;
        case TURN_RIGHT:
            turn(p, 1);
            break;
        case HOME:
            teleportHome(p);
        default: break;
        }
    }

    private void move(Player p, int dx) {
        if (!doesBlockFitAt(block.getX() + dx, block.getY())) return;
        drawBlock(false);
        block.setX(block.getX() + dx);
        drawBlock(true);
        bit(p, 1.0f);
        p.sendActionBar(text((dx < 0 ? "Left" : "Right"), AQUA));
    }

    private void turn(Player p, int rotation) {
        int newRotation = block.getRotation() + rotation;
        while (newRotation >= 4) newRotation -= 4;
        while (newRotation < 0) newRotation += 4;
        if (!doesBlockFitWith(newRotation)) return;
        drawBlock(false);
        block.setRotation(newRotation);
        drawBlock(true);
        bit(p, 1.0f);
        p.sendActionBar(text((rotation < 0 ? "Turn Left" : "Turn Right"), AQUA));
    }

    public boolean isActive() {
        switch (state) {
        case LOSE: case DISABLE: return false;
        default: return true;
        }
    }

    public void getSidebarLines(List<Component> l) {
        if (state == GameState.LOSE) {
            l.add(text("GAME OVER", RED, BOLD));
        }
        l.add(join(noSeparators(),
                       text(Unicode.tiny("next"), GRAY),
                       text(" [", GRAY),
                       nextBlock.mytems.component,
                       text("]", GRAY)));
        l.add(join(separator(space()), text(Unicode.tiny("score"), GRAY), text("" + score, WHITE)));
        l.add(join(separator(space()), text(Unicode.tiny("level"), GRAY), text("" + level, WHITE)));
        if (battle != null) {
            l.add(text(Unicode.tiny("opponents"), GOLD, ITALIC));
            List<TetrisGame> games = new ArrayList<>(battle.getGames());
            games.remove(this);
            Collections.sort(games, (a, b) -> Integer.compare(b.getScore(), a.getScore()));
            int scoreLength = 0;
            for (TetrisGame other : games) {
                scoreLength = Math.max(scoreLength, ("" + other.getScore()).length());
            }
            for (TetrisGame other : games) {
                String scoreString = String.format("%" + scoreLength + "d", other.getScore());
                Player p = other.getPlayer().getPlayer();
                l.add(join(separator(space()),
                               text(scoreString, other.isActive() ? WHITE : DARK_GRAY),
                               (p != null ? p.displayName() : text(other.getPlayer().getName(), WHITE))));
            }
        }
    }

    public void shiftUp() {
        drawBlock(false);
        block.setY(block.getY() + 1);
        board.shiftUp();
        drawBoard();
        drawBlock(true);
    }

    public void teleportHome(Player p) {
        Location loc = home.toLocation(place.getWorld());
        loc.setDirection(new Vector(-place.front.getModX(),
                                    0.0,
                                    -place.front.getModZ()));
        p.teleport(loc);
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(true);
        for (int i = 0; i < 9; i += 1) {
            Hotbar hotbar = Hotbar.ofSlot(i);
            if (hotbar == null || hotbar.mytems == null) {
                p.getInventory().setItem(i, null);
            } else {
                p.getInventory().setItem(i, Items.text(hotbar.mytems.createIcon(),
                                                       List.of(hotbar.text)));
            }
        }
        p.getInventory().setHeldItemSlot(Hotbar.NEUTRAL.slot);
    }
}
