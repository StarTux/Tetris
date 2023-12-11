package com.cavetale.tetris;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.minigame.MinigameFlag;
import com.cavetale.core.event.minigame.MinigameMatchCompleteEvent;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.session.Session;
import com.cavetale.mytems.util.BlockColor;
import com.cavetale.mytems.util.Items;
import com.cavetale.tetris.sql.SQLScore;
import com.cavetale.worldmarker.entity.EntityMarker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.mytems.MytemsPlugin.sessionOf;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.keybind;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
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
    private Tetromino savedBlock;
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
    private final List<Tetromino> tetrominos = new ArrayList<>();
    private int tetrominoIndex;
    private List<GlowItemFrame> itemFrames = new ArrayList<>();
    private TetrisGame lastBattleScoreFrom;

    public void initialize(Player p) {
        board = new TetrisBoard(10, 20);
        tetrominos.addAll(List.of(Tetromino.values()));
        Collections.shuffle(tetrominos);
        nextBlock = tetrominos.get(0);
        tetrominoIndex = 1;
        makeNewBlock();
        state = GameState.FALL;
        task = Bukkit.getScheduler().runTaskTimer(TetrisPlugin.instance, this::tick, 0L, 1L);
        fallingTicks = 20;
        drawFrame();
        drawBoard();
        drawBlock(true);
        teleportHome(p);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.5f, 2.0f);
        educate(p);
        updateScoreFrame("glhf!");
        updateNextFrames();
    }

    protected static void educate(Player p) {
        for (Hotbar hotbar : Hotbar.values()) {
            List<Component> components = new ArrayList<>();
            components.add(space());
            if (hotbar == Hotbar.NEUTRAL) {
                components.add(text("[", GRAY));
                components.add(keybind("key.drop"));
                components.add(text("]", GRAY));
                components.add(space());
                components.add(text("Drop", RED));
                components.add(newline());
                components.add(space());
                components.add(text("[", GRAY));
                components.add(keybind("key.swapOffhand"));
                components.add(text("]", GRAY));
                components.add(space());
                components.add(text("Save", AQUA));
            } else {
                components.add(text("[", GRAY));
                components.add(keybind("key.hotbar." + (hotbar.slot + 1)));
                components.add(text("]", GRAY));
                if (hotbar.slot == Hotbar.NEUTRAL.slot - 1) {
                    components.add(text("/scroll up", GRAY));
                } else if (hotbar.slot == Hotbar.NEUTRAL.slot + 1) {
                    components.add(text("/scroll down", GRAY));
                }
                components.add(space());
                components.add(hotbar.mytems.component);
                components.add(hotbar.text);
            }
            p.sendMessage(join(noSeparators(), components));
        }
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
        if (p != null) {
            p.getInventory().clear();
            sessionOf(p).setHidingPlayers(false);
        }
    }

    private void clearFrame() {
        for (int y = -1; y <= board.height; y += 1) {
            for (int x = -1; x <= board.width; x += 1) {
                for (int z = -1; z <= 1; z += 1) {
                    place.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        for (GlowItemFrame itemFrame : itemFrames) {
            itemFrame.remove();
        }
        itemFrames.clear();
    }

    private void drawFrame() {
        for (int y = -1; y <= board.height; y += 1) {
            for (int x = -1; x <= board.width; x += 1) {
                if ((x == -1 || x == board.width) || (y == -1 || y == board.height)) {
                    Material frameMaterial = Material.WHITE_CONCRETE;
                    place.getBlockAt(x, y, -1).setType(frameMaterial, false);
                    place.getBlockAt(x, y, 0).setType(frameMaterial, false);
                    place.getBlockAt(x, y, 1).setType(frameMaterial, false);
                } else {
                    place.getBlockAt(x, y, -1).setType((x % 2) == (y % 2) ? Material.BLACK_CONCRETE : Material.BLACK_TERRACOTTA, false);
                    place.getBlockAt(x, y, 1).setType(Material.LIGHT, false);
                }
            }
        }
        for (int x = board.width; x >= -1; x -= 1) {
            Location location = place.getBlockAt(x, board.height, 2).getLocation().add(0.5, 0.5, 0.0);
            GlowItemFrame itemFrame = location.getWorld().spawn(location, GlowItemFrame.class, e -> {
                    e.setPersistent(false);
                    e.setVisible(false);
                    e.setFixed(true);
                    e.setFacingDirection(BlockFace.SOUTH);
                });
            itemFrames.add(itemFrame);
        }
    }

    private void updateScoreFrame(String text) {
        List<ItemStack> items = Glyph.toItemStacks(text);
        for (int i = 0; i < itemFrames.size(); i += 1) {
            GlowItemFrame itemFrame = itemFrames.get(i);
            int ii = items.size() - 1 - i;
            itemFrame.setItem(ii < 0 ? null : items.get(ii));
        }
    }

    private void updateNextFrames() {
        itemFrames.get(itemFrames.size() - 1).setItem(nextBlock.mytems.createIcon());
        itemFrames.get(itemFrames.size() - 2).setItem(savedBlock != null
                                                      ? savedBlock.mytems.createIcon()
                                                      : null);
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
        // Draw shadow
        int y2 = block.getY();
        while (doesBlockFitAt(block.getX(), y2 - 1, block.getRotation())) {
            y2 -= 1;
        }
        final int shadowY = y2;
        block.getBoard().forEachCell((dx, dy, color) -> {
                if (color == null) return;
                final int x = dx + block.getX();
                final int y = dy + shadowY;
                if (x < 0 || x >= board.width) return;
                if (y < 0 || y >= board.height) return;
                Material material = visible
                    ? color.getMaterial(BlockColor.Suffix.STAINED_GLASS)
                    : Material.AIR;
                place.getBlockAt(x, y, 0).setType(material, false);
            });
        // Draw original
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

    public boolean doesBlockFitAt(final int offx, final int offy, final int rotation) {
        boolean[] result = new boolean[1];
        result[0] = true;
        block.getBoard(rotation).forEachCell((dx, dy, color) -> {
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

    private void makeNewBlock() {
        block = new TetrisBlock(nextBlock, nextBlock.color);
        block.setX(board.width / 2 - 2);
        block.setY(board.height);
        makeNextBlock();
    }

    private void makeNextBlock() {
        if (tetrominoIndex >= tetrominos.size()) {
            tetrominoIndex = 0;
            Collections.shuffle(tetrominos);
        }
        nextBlock = tetrominos.get(tetrominoIndex++);
    }

    private void tick() {
        if (battle != null && battle.findWinner() == this) {
            do {
                MinigameMatchCompleteEvent event = new MinigameMatchCompleteEvent(MinigameMatchType.TETRIS);
                if (TetrisPlugin.getInstance().getTournament() != null) {
                    event.addFlags(MinigameFlag.EVENT);
                }
                for (TetrisGame game : battle.getGames()) {
                    if (game.getLines() < 4) continue;
                    event.addPlayerUuid(game.player.uuid);
                }
                event.addWinnerUuid(this.player.uuid);
                event.callEvent();
            } while (false);
            player.getPlayer().showTitle(title(text("VICTOR", GREEN, BOLD),
                                               text(tiny("final score ") + score, GRAY)));
            TetrisPlugin.instance.onVictory(this, battle);
            Korobeniki.play(getPlayer().getPlayer());
            // Scores
            battle.getGames().sort((b, a) -> Integer.compare(a.score, b.score));
            List<Component> messageLines = new ArrayList<>();
            messageLines.add(empty());
            int rank = 0;
            for (TetrisGame game : battle.getGames()) {
                rank += 1;
                messageLines.add(textOfChildren(text(rank + " ", GOLD),
                                                text(game.player.getName(), WHITE),
                                                text(tiny(" score"), GRAY),
                                                text(game.score, WHITE),
                                                text(tiny(" lines"), GRAY),
                                                text(game.lines, WHITE),
                                                text(tiny(" lvl"), GRAY),
                                                text(game.level, BLUE)));
            }
            messageLines.add(empty());
            Component msg = join(separator(newline()), messageLines);
            for (TetrisGame game : battle.getGames()) {
                Player target = game.player.getPlayer();
                if (target != null) target.sendMessage(msg);
            }
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
                                                   text(tiny("final score ") + score, GRAY)));
                player.getPlayer().sendMessage(join(separator(space()),
                                                    text("GAME OVER", RED),
                                                    text(tiny("Final score"), GRAY),
                                                    text(score, GOLD)));
                if (score > 0) {
                    TetrisPlugin.instance.database.insertAsync(new SQLScore(this), null);
                    TetrisPlugin.instance.getTetrisCommand().rebuildHighscores();
                }
                if (battle != null) {
                    TetrisPlugin.instance.onGameOver(this, battle);
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
                    if (newLines >= 4 && battle != null) {
                        battleScore += level;
                        for (TetrisGame other : battle.getGames()) {
                            if (other == this) continue;
                            other.battleScore -= level;
                            other.lastBattleScoreFrom = this;
                        }
                    }
                    int newLevel = lines / 10;
                    Player p = player.getPlayer();
                    if (newLevel > level) {
                        level = newLevel;
                        p.showTitle(title(text("" + level, GREEN),
                                          text(tiny("levelup"), GREEN),
                                          times(Duration.ofSeconds(0),
                                                Duration.ofSeconds(1),
                                                Duration.ofSeconds(0))));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
                    }
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
                updateScoreFrame("" + score + " l" + level);
                updateNextFrames();
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

    private void resetFallingTicks() {
        if (battle != null) {
            final boolean isTournament = TetrisPlugin.getInstance().getTournament() != null;
            int useLevel = level;
            for (TetrisGame other : battle.getGames()) {
                if (!other.state.isDuringGame()) continue;
                useLevel = isTournament
                    ? Math.max(useLevel, other.getLevel())
                    : Math.min(useLevel, other.getLevel());
            }
            fallingTicks = isTournament
                ? 10 - useLevel
                : 12 - useLevel;
        } else {
            fallingTicks = 20 - (level % 20);
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
        if (battleScore < -level) {
            battleScore = Math.max(0, battleScore + level);
            shiftUp();
            if (lastBattleScoreFrom != null) {
                player.applyPlayer(p -> p.sendMessage(text(lastBattleScoreFrom.player.getName()
                                                           + " sent you a line", RED)));
                lastBattleScoreFrom.player.applyPlayer(p -> p.sendMessage(text("You sent a line to "
                                                                               + player.getName())));
            }
        }
        fallingTicks -= 1;
        if (fallingTicks > 0) return null;
        if (!doesBlockFitAt(block.getX(), block.getY() - 1, block.getRotation())) {
            return GameState.LAND;
        }
        drawBlock(false);
        block.setY(block.getY() - 1);
        drawBlock(true);
        resetFallingTicks();
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
            if (doesBlockFitAt(block.getX(), block.getY() - 1, block.getRotation())) {
                drawBlock(false);
                block.setY(block.getY() - 1);
                drawBlock(true);
                resetFallingTicks();
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
            break;
        case EYE:
            hidePlayers(p);
            break;
        default: break;
        }
    }

    private void hidePlayers(Player p) {
        Session session = sessionOf(p);
        boolean newValue = !session.isHidingPlayers();
        session.setHidingPlayers(newValue);
        if (newValue) {
            p.sendActionBar(textOfChildren(Mytems.BLIND_EYE, text(" Hiding other Players", GOLD)));
        } else {
            p.sendActionBar(textOfChildren(Mytems.BLIND_EYE, text(" No longer hiding other Players", AQUA)));
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.5f);
    }

    public void playerInputDrop(Player p) {
        if (state != GameState.FALL) {
            bit(p, 0.5f);
            return;
        }
        p.sendActionBar(text("Drop", GREEN));
        boolean success = false;
        drawBlock(false);
        while (doesBlockFitAt(block.getX(), block.getY() - 1, block.getRotation())) {
            block.setY(block.getY() - 1);
            success = true;
        }
        drawBlock(true);
        if (success) {
            resetFallingTicks();
            bit(p, 2.0f);
        } else {
            bit(p, 0.5f);
        }
    }

    public void playerInputSwap(Player p) {
        if (!state.isDuringGame()) {
            bit(p, 0.5f);
            return;
        }
        p.sendActionBar(text("Swap", GREEN));
        Tetromino old = savedBlock;
        savedBlock = nextBlock;
        if (old != null) {
            nextBlock = old;
        } else {
            makeNextBlock();
        }
        updateNextFrames();
        bit(p, 2.0f);
    }

    private void move(Player p, int dx) {
        if (!doesBlockFitAt(block.getX() + dx, block.getY(), block.getRotation())) return;
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
        int[] dx = new int[1];
        block.getBoard(newRotation).forEachCell((x, y, color) -> {
                if (color == null) return;
                int x2 = x + block.getX();
                if (x2 < 0) {
                    dx[0] = Math.max(dx[0], -x2);
                } else if (x2 >= board.width) {
                    dx[0] = Math.min(dx[0], -(x2 - board.width + 1));
                }
            });
        if (!doesBlockFitAt(block.getX() + dx[0], block.getY(), newRotation)) return;
        drawBlock(false);
        block.setRotation(newRotation);
        block.setX(block.getX() + dx[0]);
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
        l.add(textOfChildren(text(tiny("next"), GRAY),
                             text(" [", GRAY), nextBlock.mytems.component, text("]", GRAY)));
        l.add(textOfChildren(text(tiny("saved"), GRAY), text(" [", GRAY),
                             (savedBlock != null ? savedBlock.mytems.component : empty()), text("]", GRAY)));
        l.add(join(separator(space()), text(tiny("score"), GRAY), text("" + score, WHITE)));
        l.add(join(separator(space()), text(tiny("level"), GRAY), text("" + level, WHITE)));
        l.add(join(separator(space()), text(tiny("lines"), GRAY), text("" + lines, WHITE)));
        if (battle != null) {
            l.add(text(tiny("opponents"), GOLD));
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

    public void bossbar(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGH,
                      textOfChildren(text(tiny("next"), GRAY),
                                     text("["),
                                     nextBlock.mytems.component,
                                     text("]"),
                                     space(),
                                     text(tiny("lvl"), GRAY),
                                     text("" + level)),
                      BossBar.Color.BLUE,
                      BossBar.Overlay.PROGRESS,
                      ((float) (lines % 10)) / 10.0f);
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
