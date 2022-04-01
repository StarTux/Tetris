package com.cavetale.tetris;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        Player p = event.getPlayer();
        of(p).enable(p);
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
        game.playerInput(player, hotbar);
    }

    @EventHandler
    private void onPlayerSidebar(PlayerSidebarEvent event) {
        TetrisPlayer session = of(event.getPlayer());
        TetrisGame game = session.getGame();
        List<Component> lines = new ArrayList<>();
        if (game != null) game.getSidebarLines(lines);
        if (plugin.getTournament() != null) plugin.getTournament().getSidebarLines(lines);
        if (lines != null && !lines.isEmpty()) {
            event.add(plugin, Priority.HIGHEST, lines);
        }
    }
}
