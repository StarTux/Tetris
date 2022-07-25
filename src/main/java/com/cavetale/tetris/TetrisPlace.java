package com.cavetale.tetris;

import com.cavetale.core.struct.Vec3i;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@RequiredArgsConstructor
public final class TetrisPlace {
    public final String worldName;
    public final Vec3i origin;
    public final BlockFace right;
    public final BlockFace front;

    public TetrisPlace(final Block block, final BlockFace right, final BlockFace front) {
        this.worldName = block.getWorld().getName();
        this.origin = Vec3i.of(block);
        this.right = right;
        this.front = front;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public Block getOriginBlock() {
        return origin.toBlock(getWorld());
    }

    public Block getBlockAt(int x, int y, int z) {
        return getOriginBlock()
            .getRelative(right, x)
            .getRelative(0, y, 0)
            .getRelative(front, z);
    }
}
