package com.cavetale.tetris;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Sessions implements Listener {
    private final TetrisPlugin plugin;
    private Map<UUID, TetrisPlayer> sessions = new HashMap<>();;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            of(p).enable(p);
        }
    }

    public void disable() {
        for (TetrisPlayer session : sessions.values()) {
            session.disable();
        }
        sessions.clear();
    }

    public TetrisPlayer of(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new TetrisPlayer(player));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        of(p).disable();
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        of(player).enable(player);
        if (plugin.getTournament() == null && plugin.getMatch().isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (!plugin.getMatch().isEnabled()) return;
                    player.sendMessage(textOfChildren(newline(),
                                                      text("A match is about to start. ", GREEN, BOLD),
                                                      Mytems.MOUSE_LEFT,
                                                      text("Click here to join", GREEN, BOLD),
                                                      newline())
                                       .hoverEvent(showText(text("/tetris join", GREEN)))
                                       .clickEvent(runCommand("/tetris join")));
                }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (event.getNewSlot() == Hotbar.NEUTRAL.slot) return;
        Player player = event.getPlayer();
        TetrisPlayer session = sessions.get(player.getUniqueId());
        if (session == null) return;
        TetrisGame game = session.getGame();
        if (game == null) return;
        event.setCancelled(true);
        if (event.getPreviousSlot() != Hotbar.NEUTRAL.slot) {
            player.getInventory().setHeldItemSlot(Hotbar.NEUTRAL.slot);
        }
        Hotbar hotbar = Hotbar.ofSlot(event.getNewSlot());
        if (hotbar == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
                game.playerInput(player, hotbar);
            });
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        TetrisPlayer session = of(event.getPlayer());
        TetrisGame game = session.getGame();
        List<Component> l = new ArrayList<>();
        l.add(plugin.tetrisTitle);
        if (game != null) {
            game.getSidebarLines(l);
            game.bossbar(event);
        }
        if (plugin.getTournament() != null) {
            plugin.getTournament().getSidebarLines(session, l);
        }
        if (l != null && !l.isEmpty()) {
            event.sidebar(PlayerHudPriority.HIGHEST, l);
        }
        if (game == null && plugin.getTournament() == null && plugin.getMatch().isEnabled()) {
            event.bossbar(PlayerHudPriority.DEFAULT,
                          textOfChildren(text("Type "),
                                         text("/tetris join", GREEN),
                                         text(" to join the match: "),
                                         text(plugin.getMatch().getJoined().size(), GREEN)),
                          BossBar.Color.GREEN,
                          BossBar.Overlay.PROGRESS,
                          plugin.getMatch().getProgress());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TetrisPlayer session = of(player);
        TetrisGame game = session.getGame();
        if (game != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        TetrisPlayer session = of(player);
        TetrisGame game = session.getGame();
        if (game == null) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
                game.playerInputDrop(player);
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        TetrisPlayer session = of(player);
        TetrisGame game = session.getGame();
        if (game == null) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
                game.playerInputSwap(player);
                player.getInventory().setItemInOffHand(null);
                player.getInventory().setItem(Hotbar.NEUTRAL.slot, tooltip(Hotbar.NEUTRAL.mytems.createIcon(),
                                                                           List.of(Hotbar.NEUTRAL.text)));
            });
    }

    @EventHandler
    private void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(Bukkit.getWorld("tetris").getSpawnLocation());
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(Bukkit.getWorld("tetris").getSpawnLocation());
    }
}
