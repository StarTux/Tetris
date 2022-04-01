package com.cavetale.tetris;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

@RequiredArgsConstructor
public final class Allocator {
    private final TetrisPlugin plugin;
    private String worldName;
    private int gridSize;
    private final List<WorldSlice> slices = new ArrayList<>();
    private int centerX;
    private int centerZ;

    public void enable(final World world, final int theGridSize) {
        this.worldName = world.getName();
        this.gridSize = theGridSize;
        WorldBorder border = world.getWorldBorder();
        final Location center = border.getCenter();
        centerX = center.getBlockX();
        centerZ = center.getBlockZ();
        final double size = Math.min(1024.0, border.getSize() * 0.5);
        final int margin = 16 * 8;
        int ax = (int) Math.round(center.getX() - size) + margin;
        int bx = (int) Math.round(center.getX() + size) - margin;
        int az = (int) Math.round(center.getZ() - size) + margin;
        int bz = (int) Math.round(center.getZ() + size) - margin;
        for (int z = az; z + gridSize < bz; z += gridSize) {
            for (int x = ax; x + gridSize < bx; x += gridSize) {
                WorldSlice slice = new WorldSlice(x, z, gridSize);
                slices.add(slice);
            }
        }
        plugin.getLogger().info("[Allocator] Created " + slices.size() + " slices");
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public WorldSlice dealSlice() {
        WorldSlice result = null;
        int minDist = Integer.MAX_VALUE;
        for (WorldSlice slice : slices) {
            if (slice.isUsed()) continue;
            int dx = Math.abs(centerX - slice.x);
            int dz = Math.abs(centerZ - slice.z);
            int dist = dx + dz;
            if (result == null || dist < minDist) {
                result = slice;
                minDist = dist;
            }
        }
        return result;
    }
}
