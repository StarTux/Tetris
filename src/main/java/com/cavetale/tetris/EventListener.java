package com.cavetale.tetris;

import com.cavetale.worldmarker.entity.EntityMarker;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final TetrisPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onEntityExplode(EntityExplodeEvent event) {
        if (!EntityMarker.hasId(event.getEntity(), "tetris")) return;
        event.blockList().clear();
        event.setCancelled(true);
        event.getEntity().remove();
    }
}
